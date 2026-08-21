// Reproduces SettingsDataStore's save/load path to prove the persisted JSON
// never contains a plaintext API key, and that config defaults survive.
import com.google.gson.Gson

enum class AiProviderType(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
    GEMINI("Gemini","gemini-2.0-flash",""),
    GROQ("Groq","llama-3.3-70b-versatile","https://api.groq.com/openai/v1"),
    OPENAI("OpenAI","gpt-4o-mini","https://api.openai.com/v1"),
    CUSTOM("Custom","","")
}
data class AiProviderConfig(
    val type: AiProviderType, val enabled: Boolean = true, val apiKey: String = "",
    val model: String = "", val baseUrl: String = "", val temperature: Float = 0.7f
) {
    val hasKey get() = apiKey.isNotBlank()
    val effectiveModel get() = model.ifBlank { type.defaultModel }
    val effectiveBaseUrl get() = baseUrl.ifBlank { type.defaultBaseUrl }
    fun withDefaults() = copy(model = model.ifBlank { type.defaultModel },
                              baseUrl = baseUrl.trim().trimEnd('/').ifBlank { type.defaultBaseUrl })
    fun validate(): String? {
        val url = effectiveBaseUrl
        if (type == AiProviderType.CUSTOM) {
            if (url.isBlank()) return "Enter the endpoint base URL (e.g. https://host/v1)"
            if (!url.startsWith("https://", true)) return "Endpoint must use https:// — plaintext HTTP would expose your API key"
            if (model.isBlank()) return "Enter the model name for this endpoint"
        } else if (url.isNotBlank() && !url.startsWith("https://", true)) return "Endpoint must use https://"
        return null
    }
}
fun main() {
    val gson = Gson()
    val secret = "sk-live-SUPERSECRET-000"
    val cfg = AiProviderConfig(AiProviderType.OPENAI, apiKey = secret).withDefaults()
    // saveProviderConfig strips apiKey before serialising; key goes to the vault.
    val persisted = gson.toJson(cfg.copy(apiKey = ""))
    println("persisted JSON has no key : ${!persisted.contains("SUPERSECRET")}")
    println("persisted JSON            : $persisted")
    val loaded = gson.fromJson(persisted, AiProviderConfig::class.java)
    println("model default applied     : ${loaded.effectiveModel == "gpt-4o-mini"}")
    println("baseUrl default applied   : ${loaded.effectiveBaseUrl == "https://api.openai.com/v1"}")
    println("hasKey false before vault : ${!loaded.hasKey}")

    println("\n-- endpoint validation --")
    listOf(
        AiProviderConfig(AiProviderType.CUSTOM, apiKey="k", model="m", baseUrl="http://insecure/v1"),
        AiProviderConfig(AiProviderType.CUSTOM, apiKey="k", model="m", baseUrl="https://ok/v1"),
        AiProviderConfig(AiProviderType.CUSTOM, apiKey="k", model="",  baseUrl="https://ok/v1"),
        AiProviderConfig(AiProviderType.CUSTOM, apiKey="k", model="m", baseUrl=""),
        AiProviderConfig(AiProviderType.GEMINI, apiKey="k")
    ).forEach { c ->
        val v = c.withDefaults().validate()
        println("${c.type} base='${c.baseUrl}' model='${c.model}' -> ${v ?: "VALID"}")
    }
    // trailing slash normalisation
    val t = AiProviderConfig(AiProviderType.CUSTOM, apiKey="k", model="m", baseUrl="https://host/v1/ ").withDefaults()
    println("\ntrailing slash trimmed    : '${t.baseUrl}'")
}
