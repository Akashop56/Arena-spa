data class ProviderMessage(val role: String, val content: String)






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

fun main() {
    val b = ContextBudget()
    // 40 turns, some huge
    val hist = (1..40).map { i ->
        ProviderMessage(if (i % 2 == 1) "user" else "assistant",
            if (i == 10) "X".repeat(60_000) else "turn number $i with a little bit of content")
    }
    println("raw history turns      : " + hist.size)
    println("raw estimated tokens   : " + b.estimateTokens(hist))
    val clamped = hist.map { ProviderMessage(it.role, b.clampMessage(it.content, ContextBudget.MAX_SINGLE_MESSAGE_TOKENS)) }
    println("after per-msg clamp    : " + b.estimateTokens(clamped))
    val fitted = b.fitHistory(clamped, 2300)
    println("after fitHistory(2300) : " + b.estimateTokens(fitted) + " tokens, " + fitted.size + " turns")
    println("within budget          : " + (b.estimateTokens(fitted) <= 2300))
    println("keeps most recent turn : " + (fitted.last().content == hist.last().content))
    println("starts on user turn    : " + (fitted.first().role == "user"))
    println("truncation marked      : " + clamped[9].content.endsWith("[truncated]"))
    println("empty history          : " + b.fitHistory(emptyList(), 1000).size)
    println("zero budget            : " + b.fitHistory(clamped, 0).size)
    // a single message bigger than the whole budget
    val one = listOf(ProviderMessage("user", "Y".repeat(100_000)))
    println("oversized single       : " + b.fitHistory(one, 500).size + " (dropped, no crash)")
}
