package com.ronin.ai.core.ai.brain

import com.ronin.ai.core.domain.model.IntentMatch
import com.ronin.ai.core.domain.model.IntentType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic intent classifier: explicit-prefix regex rules first, then
 * keyword fallbacks. This runs before any LLM call, so tool intents work
 * instantly and offline.
 */
@Singleton
class IntentClassifier @Inject constructor() {

    private data class Rule(
        val intent: IntentType,
        val regex: Regex,
        val confidence: Float = 0.95f
    )

    private val rules = listOf(
        Rule(IntentType.RUN_ROUTINE, Regex("""^(?:run|execute|trigger|start)\s+(?:the\s+)?routine\s+(.+)$""")),
        Rule(IntentType.SAVE_MEMORY, Regex("""^(?:remember|don'?t forget)\s+(?:that\s+)?(.+)$""")),
        Rule(IntentType.RECALL_MEMORY, Regex("""^what do you (?:know|remember)(?:\s+about)?\s*(.+)?$"""), 0.9f),
        Rule(IntentType.RECALL_MEMORY, Regex("""^recall\s+(.+)$""")),
        Rule(IntentType.RECALL_MEMORY, Regex("""^what (?:do|have) you (?:know|remember|learned|learnt)\??$"""), 0.9f),
        Rule(IntentType.OPEN_APP, Regex("""^(?:open|launch|start)\s+(?:the\s+)?(?:app\s+)?(.+)$""")),
        Rule(IntentType.BROWSER_SEARCH, Regex("""^(?:search|google|look up)\s+(?:for\s+|the\s+)?(.+)$""")),
        Rule(IntentType.CREATE_NOTE, Regex("""^(?:note|make a note|take a note|write down|save note)[:\s]+(.+)$""")),
        Rule(IntentType.SEND_NOTIFICATION, Regex("""^(?:remind me|notify me|set a reminder|set reminder)\s+(?:to\s+|that\s+)?(.+)$""")),
        Rule(IntentType.DEVICE_CONTROL, Regex("""^(?:set\s+)?volume\s+(?:to\s+)?\d{1,3}$""")),
        Rule(IntentType.DEVICE_CONTROL, Regex("""^volume\s+(up|down)$""")),
        Rule(IntentType.DEVICE_CONTROL, Regex("""^(?:turn\s+)?(?:torch|flashlight)\s+(on|off)$""")),
        Rule(IntentType.DEVICE_CONTROL, Regex("""^(?:set\s+)?brightness\s+(?:to\s+)?\d{1,3}$""")),
        Rule(IntentType.BATTERY_STATUS, Regex("""^(?:what'?s|what is|tell me about|check|show)?\s*(?:my\s+)?(?:battery|battery level|battery status)(?:\??)$"""), 0.9f),
        Rule(IntentType.DEVICE_INFO, Regex("""^(?:what'?s|what is|tell me about|show|check)?\s*(?:my\s+)?(?:device|phone)\s*(?:info|information)?(?:\??)$"""), 0.85f),
        Rule(IntentType.TIME_INFO, Regex("""^(?:what|tell me|do you know)?\s*(?:the\s+)?(?:time|date|current time|today'?s date)(?:\??)$"""), 0.9f)
    )

    fun classify(input: String): IntentMatch {
        val text = input.trim().lowercase(Locale.ROOT).replace(Regex("""[.!?]+$"""), "")
        if (text.isBlank()) return IntentMatch(IntentType.GENERAL, "", 0.5f)

        for (rule in rules) {
            val match = rule.regex.find(text) ?: continue
            val param = match.groupValues.getOrNull(1)?.trim() ?: ""
            return IntentMatch(rule.intent, param, rule.confidence)
        }

        return when {
            text.contains("battery") -> IntentMatch(IntentType.BATTERY_STATUS, "", 0.8f)
            text.contains("device info") || text.contains("phone info") ->
                IntentMatch(IntentType.DEVICE_INFO, "", 0.8f)
            text.contains("what time") || text.contains("what's the time") || text.contains("current time") ->
                IntentMatch(IntentType.TIME_INFO, "", 0.8f)
            text.contains("remind") || text.contains("notify") ->
                IntentMatch(IntentType.SEND_NOTIFICATION, text.replace(Regex("""^(?:please\s+)?(?:remind|notify)(?:\s+me)?\s*"""), "").trim(), 0.7f)
            else -> IntentMatch(IntentType.GENERAL, "", 0.5f)
        }
    }
}
