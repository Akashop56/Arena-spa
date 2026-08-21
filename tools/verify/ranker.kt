import java.util.Locale
import kotlin.math.ln

data class MemoryItem(val id: Long, val type: String, val title: String, val content: String,
                      val tags: List<String> = emptyList(), val importance: Int = 1,
                      val updatedAt: Long = System.currentTimeMillis())






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

fun main() {
    val now = System.currentTimeMillis()
    val day = 86_400_000L
    val mem = listOf(
        MemoryItem(1,"PREFERENCE","User likes","I love black coffee in the morning",importance=2,updatedAt=now-2*day),
        MemoryItem(2,"PREFERENCE","User name","Arjun",importance=3,updatedAt=now-30*day),
        MemoryItem(3,"LONG_TERM","Guitar goal","wants to learn guitar this year",importance=2,updatedAt=now-5*day),
        MemoryItem(4,"LONG_TERM","Work","works at a hospital in Kanpur",importance=2,updatedAt=now-10*day),
        MemoryItem(5,"LONG_TERM","Allergy","allergic to peanuts",importance=3,updatedAt=now-1*day),
        MemoryItem(6,"LONG_TERM","Tea note","prefers masala chai in the evening",importance=1,updatedAt=now-3*day)
    )
    val r = MemoryRanker()
    listOf("what do you know about my coffee preference",
           "am I allergic to anything?",
           "do you remember what instrument I want to learn",
           "where do I work",
           "what is my name",
           "tell me about quantum physics").forEach { q ->
        println("")
        println("query : " + q)
        println("terms : " + r.terms(q))
        println("recall: " + r.rank(q, mem, 3).map { it.title })
    }
}
