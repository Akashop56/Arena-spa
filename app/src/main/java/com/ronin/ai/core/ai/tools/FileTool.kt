package com.ronin.ai.core.ai.tools

import android.content.Context
import com.ronin.ai.core.common.firstWords
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** App-scoped notes: create, list and read text files on the device. */
@Singleton
class FileTool @Inject constructor(
    @ApplicationContext context: Context
) : RoninTool {

    private val notesDir = File(context.filesDir, "ronin_notes").apply { mkdirs() }

    override val definition = ToolDefinition(
        id = "files",
        name = "Files & notes",
        description = "Save a note to the device, e.g. “note: buy milk tomorrow”.",
        category = ToolCategory.FILES
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.CREATE_NOTE

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val content = param.trim()
        if (content.isBlank()) {
            return ToolResult(false, "What should I write down?", IntentType.CREATE_NOTE)
        }
        return runCatching {
            val name = content.firstWords(4)
                .replace(Regex("""[^a-zA-Z0-9\s\-_]"""), "")
                .trim()
                .ifBlank { "note" }
            val file = File(notesDir, "$name-${System.currentTimeMillis()}.txt")
            file.writeText(content)
            ToolResult(
                true,
                "Saved note “${content.firstWords(12)}”.",
                IntentType.CREATE_NOTE,
                file.absolutePath
            )
        }.getOrElse { e ->
            ToolResult(false, "Couldn't save the note: ${e.message}", IntentType.CREATE_NOTE)
        }
    }

    fun listNotes(): List<File> =
        notesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}
