// Exercises the exact parsing code paths from OpenAiCompatibleProvider and
// GeminiProvider against real-world response/SSE payloads.
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray

class AiRequestException(val userMessage: String, val httpCode: Int? = null) : Exception(userMessage)
val gson = Gson()

// ---- OpenAI-compatible buffered parse (copied from the provider) ----
fun parseOpenAi(json: JsonObject, model: String): String {
    val choices = json.getAsJsonArray("choices") ?: throw AiRequestException("Unexpected provider response (missing choices)")
    if (choices.size() == 0) throw AiRequestException("Provider returned an empty response")
    val message = choices[0].asJsonObject.getAsJsonObject("message")
    val content = message?.get("content")?.let { el -> if (el.isJsonNull) null else runCatching { el.asString }.getOrNull() }.orEmpty()
    return content.trim()
}
// ---- OpenAI SSE delta extraction ----
fun sseDelta(payload: String): String? = runCatching {
    gson.fromJson(payload, JsonObject::class.java)
        ?.getAsJsonArray("choices")?.takeIf { it.size() > 0 }
        ?.get(0)?.asJsonObject?.getAsJsonObject("delta")
        ?.get("content")?.takeIf { !it.isJsonNull }?.asString
}.getOrNull()
// ---- Gemini parse ----
fun parseGemini(json: JsonObject): String {
    val candidates = json.getAsJsonArray("candidates")
    if (candidates == null || candidates.size() == 0) {
        val block = runCatching { json.getAsJsonObject("promptFeedback")?.get("blockReason")?.asString }.getOrNull()
        throw AiRequestException(if (block != null) "Request blocked by Gemini ($block)" else "Gemini returned no candidates")
    }
    val parts = candidates[0].asJsonObject.getAsJsonObject("content")?.getAsJsonArray("parts")
    val text = parts?.joinToString("\n") { p -> runCatching { p.asJsonObject.get("text")?.asString }.getOrNull().orEmpty() }.orEmpty()
    if (text.isBlank()) throw AiRequestException("Gemini returned an empty response")
    return text.trim()
}
fun geminiSse(payload: String): String? = runCatching {
    gson.fromJson(payload, JsonObject::class.java)
        ?.getAsJsonArray("candidates")?.takeIf { it.size() > 0 }
        ?.get(0)?.asJsonObject?.getAsJsonObject("content")?.getAsJsonArray("parts")
        ?.joinToString("") { p -> runCatching { p.asJsonObject.get("text")?.asString }.getOrNull().orEmpty() }
}.getOrNull()

fun j(s: String) = gson.fromJson(s, JsonObject::class.java)

fun main() {
    println("== OpenAI/Groq buffered ==")
    println("normal      : '" + parseOpenAi(j("""{"choices":[{"message":{"role":"assistant","content":"Hello there"}}],"usage":{"total_tokens":9}}"""),"m") + "'")
    println("null content: '" + parseOpenAi(j("""{"choices":[{"message":{"content":null}}]}"""),"m") + "'")
    println("no message  : '" + parseOpenAi(j("""{"choices":[{"finish_reason":"stop"}]}"""),"m") + "'")
    runCatching { parseOpenAi(j("""{"error":{"message":"bad key"}}"""),"m") }
        .onFailure { println("error body  : ${(it as AiRequestException).userMessage}") }
    runCatching { parseOpenAi(j("""{"choices":[]}"""),"m") }
        .onFailure { println("empty arr   : ${(it as AiRequestException).userMessage}") }

    println("\n== OpenAI SSE stream ==")
    val sse = listOf(
        """{"choices":[{"delta":{"role":"assistant"},"index":0}]}""",
        """{"choices":[{"delta":{"content":"Hel"},"index":0}]}""",
        """{"choices":[{"delta":{"content":"lo"},"index":0}]}""",
        """{"choices":[{"delta":{"content":" world"},"index":0}]}""",
        """{"choices":[{"delta":{},"finish_reason":"stop","index":0}]}""",
        """not-json-at-all"""
    )
    val sb = StringBuilder()
    sse.forEach { p -> sseDelta(p)?.let { sb.append(it) } }
    println("assembled   : '$sb'  (malformed chunk skipped, no crash)")

    println("\n== Gemini ==")
    println("normal      : '" + parseGemini(j("""{"candidates":[{"content":{"parts":[{"text":"Hi from Gemini"}]}}]}""")) + "'")
    println("multi-part  : '" + parseGemini(j("""{"candidates":[{"content":{"parts":[{"text":"a"},{"text":"b"}]}}]}""")) + "'")
    runCatching { parseGemini(j("""{"promptFeedback":{"blockReason":"SAFETY"}}""")) }
        .onFailure { println("safety block: ${(it as AiRequestException).userMessage}") }
    runCatching { parseGemini(j("""{"candidates":[]}""")) }
        .onFailure { println("no candidate: ${(it as AiRequestException).userMessage}") }
    val gsb = StringBuilder()
    listOf("""{"candidates":[{"content":{"parts":[{"text":"स्ट्री"}]}}]}""",
           """{"candidates":[{"content":{"parts":[{"text":"मिंग"}]}}]}""").forEach { p -> geminiSse(p)?.let { gsb.append(it) } }
    println("gemini sse  : '$gsb' (unicode preserved)")
}
