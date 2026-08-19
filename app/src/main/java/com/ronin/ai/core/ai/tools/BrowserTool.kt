package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Opens URLs and performs web searches in the default browser. */
@Singleton
class BrowserTool @Inject constructor(
    private val deviceManager: DeviceManager
) : RoninTool {

    override val definition = ToolDefinition(
        id = "browser",
        name = "Browser",
        description = "Open a website or search the web, e.g. “search for today's news”.",
        category = ToolCategory.BROWSER
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.BROWSER_SEARCH

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val query = param.trim()
        if (query.isBlank()) {
            return ToolResult(false, "What should I search for?", IntentType.BROWSER_SEARCH)
        }
        val isUrl = query.matches(Regex("""^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(/.*)?$"""))
        val url = if (isUrl) {
            if (query.startsWith("http://") || query.startsWith("https://")) query else "https://$query"
        } else {
            "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
        }
        val opened = deviceManager.openUrl(url)
        return if (opened) {
            ToolResult(true, "Opened $query in the browser.", IntentType.BROWSER_SEARCH, url)
        } else {
            ToolResult(false, "I couldn't open the browser.", IntentType.BROWSER_SEARCH)
        }
    }
}
