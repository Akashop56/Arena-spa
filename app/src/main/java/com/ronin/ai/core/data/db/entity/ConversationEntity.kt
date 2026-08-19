package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.ChatRole

@Entity(
    tableName = "conversations",
    indices = [Index("timestamp")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolUsed: String? = null
)

fun ConversationEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = ChatRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    toolUsed = toolUsed
)

fun ChatMessage.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    role = role.name,
    content = content,
    timestamp = timestamp,
    toolUsed = toolUsed
)
