import java.util.Locale
import kotlin.math.ln

data class MemoryItem(val id: Long, val type: String, val title: String, val content: String,
                      val tags: List<String> = emptyList(), val importance: Int = 1,
                      val updatedAt: Long = System.currentTimeMillis())
data class ProviderMessage(val role: String, val content: String)








/**
 * Scores candidate memories against a query.
 *
 * The previous recall path took the first three tokens of the raw input and
 * OR-ed them into LIKE queries. Because English questions begin with filler
 * ("what do you know about my coffee preference" → what/do/you), the actual
 * subject was routinely dropped and RONIN appeared to forget things it had
 * been told. This ranker fixes that in three steps:
 *
 *  1. **Stopword removal** so only content-bearing terms are searched.
 *  2. **Term weighting** — rarer terms across the candidate set are worth more
 *     (a light IDF), so "coffee" outranks "my".
 *  3. **Signal blending** — lexical overlap is combined with the user-set
 *     importance and with recency, instead of ordering by importance alone.
 *
 * It is deliberately dependency-free and O(candidates × terms): this runs on
 * every turn on a low-end phone, so no embeddings and no extra libraries.
 */
class MemoryRanker  {

    /**
     * Words that carry no retrieval signal. Kept deliberately small — an
     * over-aggressive list would strip meaningful short words ("tea", "job").
     * Hindi stopwords are included because RONIN is bilingual.
     */
    private val stopwords: Set<String> = setOf(
        // English — questions, pronouns, auxiliaries, prepositions
        "a", "about", "all", "am", "an", "and", "any", "anything", "are", "as", "at",
        "be", "been", "but", "by", "can", "could", "did", "do", "does", "for", "from",
        "get", "give", "had", "has", "have", "he", "her", "him", "his", "how", "i",
        "if", "in", "into", "is", "it", "its", "just", "know", "me", "mine", "my",
        "of", "on", "or", "our", "out", "please", "remember", "she", "should", "so",
        "some", "tell", "that", "the", "their", "them", "then", "there", "these",
        "they", "this", "those", "to", "up", "us", "was", "we", "were", "what",
        "when", "where", "which", "who", "why", "will", "with", "would", "you",
        "your", "yours", "im", "ive", "dont", "doesnt", "cant", "thing", "things",
        // Hindi (Devanagari)
        "और", "का", "कि", "की", "के", "को", "क्या", "है", "हैं", "हूँ", "मुझे",
        "मेरा", "मेरी", "मेरे", "में", "यह", "वह", "से", "पर", "हो", "था", "थी"
    )

    /** Content-bearing terms, lowercased and de-duplicated, longest first. */
    fun terms(query: String): List<String> =
        query.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .asSequence()
            .filter { it.length >= 3 || it.any { ch -> ch.code in 0x900..0x97F } }
            .filter { it !in stopwords }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it.length }
            .take(MAX_TERMS)
            .toList()

    /**
     * Ranks [candidates] for [query] and returns the best [limit].
     * Items with no lexical overlap are dropped: padding the prompt with
     * unrelated memories actively degrades the model's answer.
     */
    fun rank(
        query: String,
        candidates: List<MemoryItem>,
        limit: Int,
        now: Long = System.currentTimeMillis()
    ): List<MemoryItem> {
        if (candidates.isEmpty()) return emptyList()
        val queryTerms = terms(query)
        if (queryTerms.isEmpty()) {
            // Nothing to match on — fall back to the most important/recent.
            return candidates
                .sortedWith(compareByDescending<MemoryItem> { it.importance }.thenByDescending { it.updatedAt })
                .take(limit)
        }

        // Light IDF: a term matching most memories discriminates poorly.
        val docFreq = queryTerms.associateWith { term ->
            candidates.count { it.searchText().contains(term) }
        }

        return candidates
            .map { item -> item to score(item, queryTerms, docFreq, candidates.size, now) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun score(
        item: MemoryItem,
        queryTerms: List<String>,
        docFreq: Map<String, Int>,
        corpusSize: Int,
        now: Long
    ): Double {
        val haystack = item.searchText()
        var lexical = 0.0
        for (term in queryTerms) {
            if (!haystack.contains(term)) continue
            val df = (docFreq[term] ?: 1).coerceAtLeast(1)
            val idf = ln(1.0 + corpusSize.toDouble() / df)
            // A hit in the title is a stronger signal than one in the body.
            val fieldBoost = if (item.title.lowercase(Locale.ROOT).contains(term)) 1.6 else 1.0
            lexical += idf * fieldBoost
        }
        if (lexical == 0.0) return 0.0

        // Importance is user/extractor-assigned (1..3).
        val importance = 1.0 + (item.importance.coerceIn(0, 5) * 0.25)

        // Recency: full weight for a day old, decaying to ~0.5 over a month.
        val ageDays = ((now - item.updatedAt).coerceAtLeast(0L)) / 86_400_000.0
        val recency = 1.0 / (1.0 + ln(1.0 + ageDays / 30.0))

        return lexical * importance * recency
    }

    private fun MemoryItem.searchText(): String =
        (title + " " + content + " " + tags.joinToString(" ")).lowercase(Locale.ROOT)

    private companion object {
        const val MAX_TERMS = 8
    }
}






/**
 * Keeps one request inside a sane context window.
 *
 * Before this existed the assembler always sent the last 24 turns verbatim
 * plus every recalled memory. Messages are unbounded in length, so a long
 * session (or one pasted log) could push the request past the model's context
 * limit — the provider then rejects the whole call and the user sees a hard
 * failure instead of a reply.
 *
 * Budgeting is done in characters. A real tokenizer would be more precise but
 * would mean shipping vocabulary files and per-model logic; for trimming
 * purposes an approximate ratio is sufficient and costs nothing on a low-end
 * device. The estimate deliberately errs on the side of over-counting.
 */
class ContextBudget  {

    /** Rough upper bound of tokens for [text] (~3.6 chars/token, conservative). */
    fun estimateTokens(text: String): Int =
        if (text.isEmpty()) 0 else (text.length / 3.6).toInt() + 1

    fun estimateTokens(messages: List<ProviderMessage>): Int =
        messages.sumOf { estimateTokens(it.content) + PER_MESSAGE_OVERHEAD }

    /**
     * Trims conversation history to fit [maxTokens], keeping the most recent
     * turns. History is dropped in whole messages from the oldest end so the
     * dialogue never starts mid-exchange; a leading assistant turn is also
     * dropped so the trimmed history begins with a user message.
     */
    fun fitHistory(history: List<ProviderMessage>, maxTokens: Int): List<ProviderMessage> {
        if (history.isEmpty() || maxTokens <= 0) return emptyList()

        val kept = ArrayDeque<ProviderMessage>()
        var used = 0
        for (message in history.asReversed()) {
            val cost = estimateTokens(message.content) + PER_MESSAGE_OVERHEAD
            if (used + cost > maxTokens) break
            kept.addFirst(message)
            used += cost
        }
        // Don't open the history on an assistant reply with no preceding user
        // turn — some providers reject that, and it reads as missing context.
        while (kept.size > 1 && kept.first().role == "assistant") {
            kept.removeFirst()
        }
        return kept.toList()
    }

    /**
     * Truncates a single oversized message at a word boundary. Used for
     * pasted logs, which would otherwise consume the entire budget alone.
     */
    fun clampMessage(text: String, maxTokens: Int): String {
        val maxChars = (maxTokens * 3.6).toInt()
        if (text.length <= maxChars) return text
        val cut = text.take(maxChars)
        val boundary = cut.lastIndexOf(' ')
        return (if (boundary > maxChars / 2) cut.take(boundary) else cut) + "\n…[truncated]"
    }

    companion object {
        /** Per-message role/formatting overhead charged by chat APIs. */
        const val PER_MESSAGE_OVERHEAD = 4

        /**
         * Total input budget. Chosen to fit comfortably inside the smallest
         * context window RONIN's providers offer (8k) while leaving room for
         * MAX_OUTPUT_TOKENS of reply.
         */
        const val TOTAL_INPUT_TOKENS = 3_500

        /** Reserved for the system prompt (identity, rules, memories). */
        const val SYSTEM_PROMPT_TOKENS = 1_200

        /** Any single user turn is clamped to this before being sent. */
        const val MAX_SINGLE_MESSAGE_TOKENS = 1_200
    }
}

fun buildSystemPrompt(name: String, prefs: List<MemoryItem>, mems: List<MemoryItem>): String = buildString {
    appendLine("You are $name, a personal AI assistant running on the user's Android device.")
    appendLine("RULES:")
    appendLine("- Never reveal your internal chain of thought.")
    fun String.clamp(max: Int = 220): String { val t = trim().replace('\n',' '); return if (t.length<=max) t else t.take(max).trimEnd()+"\u2026" }
    if (prefs.isNotEmpty()) { appendLine(); appendLine("USER PREFERENCES (respect these):"); prefs.forEach { appendLine("- " + it.content.clamp()) } }
    if (mems.isNotEmpty()) {
        appendLine(); appendLine("RELEVANT MEMORIES (use if helpful):")
        val seen = prefs.map { it.content.trim().lowercase() }.toMutableSet()
        mems.forEach { m -> if (seen.add(m.content.trim().lowercase())) appendLine("- " + m.content.clamp()) }
    }
}
fun main() {
    val now = System.currentTimeMillis(); val day = 86_400_000L
    val prefs = listOf(
        MemoryItem(1,"PREFERENCE","User name","Arjun",importance=3,updatedAt=now-40*day),
        MemoryItem(2,"PREFERENCE","User likes","I love black coffee in the morning",importance=2,updatedAt=now-2*day))
    val store = prefs + listOf(
        MemoryItem(3,"LONG_TERM","Allergy","allergic to peanuts",importance=3,updatedAt=now-1*day),
        MemoryItem(4,"LONG_TERM","Guitar goal","wants to learn guitar this year",importance=2,updatedAt=now-5*day),
        MemoryItem(5,"LONG_TERM","Long note","x".repeat(900),importance=1,updatedAt=now-3*day),
        // duplicate of a preference -> must not appear twice
        MemoryItem(6,"LONG_TERM","Coffee dup","I love black coffee in the morning",importance=1,updatedAt=now-1*day))
    val ranker = MemoryRanker(); val budget = ContextBudget()
    val q = "what do you know about my coffee preference"
    val recalled = ranker.rank(q, store, 6)
    val sys = buildSystemPrompt("RONIN", prefs, recalled)
    println("--- SYSTEM PROMPT ---"); println(sys)
    println("coffee fact present   : " + sys.contains("black coffee"))
    println("no duplicate coffee   : " + (Regex("black coffee").findAll(sys).count() == 1))
    println("long note clamped     : " + (!sys.contains("x".repeat(300))))
    println("system prompt tokens  : " + budget.estimateTokens(sys) + " (limit " + ContextBudget.SYSTEM_PROMPT_TOKENS + ")")
    println("within system budget  : " + (budget.estimateTokens(sys) <= ContextBudget.SYSTEM_PROMPT_TOKENS))
    val hist = (1..30).map { i -> ProviderMessage(if (i%2==1) "user" else "assistant", "conversation turn $i") }
    val fitted = budget.fitHistory(hist, ContextBudget.TOTAL_INPUT_TOKENS - budget.estimateTokens(sys))
    val total = budget.estimateTokens(sys) + budget.estimateTokens(fitted)
    println("total request tokens  : " + total + " (limit " + ContextBudget.TOTAL_INPUT_TOKENS + ")")
    println("WITHIN TOTAL BUDGET   : " + (total <= ContextBudget.TOTAL_INPUT_TOKENS))
}
