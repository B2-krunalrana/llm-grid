package com.example.data

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.local.ModelCache
import com.example.data.local.ModelDao
import com.example.data.network.ChatCompletionRequest
import com.example.data.network.NetworkMessage
import com.example.data.network.OpenRouterApi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class ChatBackup(
    val title: String,
    val modelId: String,
    val messages: List<BackupMessage>
)

@JsonClass(generateAdapter = true)
data class BackupMessage(
    val role: String,
    val content: String,
    val timestamp: Long
)

class ChatRepository(
    private val chatDao: ChatDao,
    private val modelDao: ModelDao,
    private val openRouterApi: OpenRouterApi
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val backupAdapter = moshi.adapter(ChatBackup::class.java)

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessionsFlow()
    val allCachedModels: Flow<List<ModelCache>> = modelDao.getAllModelsFlow()

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSessionFlow(sessionId)

    suspend fun getSession(sessionId: Int): ChatSession? =
        chatDao.getSessionById(sessionId)

    suspend fun createNewSession(title: String, modelId: String): Int {
        return withContext(Dispatchers.IO) {
            val session = ChatSession(
                title = title,
                modelId = modelId
            )
            chatDao.insertSession(session).toInt()
        }
    }

    suspend fun updateSessionTitle(sessionId: Int, newTitle: String) {
        withContext(Dispatchers.IO) {
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.insertSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun updateSessionModel(sessionId: Int, newModelId: String) {
        withContext(Dispatchers.IO) {
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.insertSession(session.copy(modelId = newModelId, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteSession(sessionId: Int) {
        withContext(Dispatchers.IO) {
            chatDao.deleteSessionAndMessages(sessionId)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            chatDao.clearAllChats()
        }
    }

    suspend fun sendMessage(
        sessionId: Int,
        userText: String,
        apiKey: String,
        modelId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Insert User Message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = userText
            )
            chatDao.insertMessage(userMsg)

            // Update session's timestamp
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.insertSession(session.copy(updatedAt = System.currentTimeMillis()))
            }

            // 2. Fetch full conversation history to feed to OpenRouter
            val history = chatDao.getMessagesForSession(sessionId)
            val networkMessages = history.map { NetworkMessage(it.role, it.content) }

            // 3. Request
            val request = ChatCompletionRequest(
                model = modelId,
                messages = networkMessages
            )

            val authorizationHeader = "Bearer $apiKey"
            val response = openRouterApi.createChatCompletion(
                bearerToken = authorizationHeader,
                request = request
            )

            // Check if response returned an error
            if (response.error != null) {
                return@withContext Result.failure(Exception(response.error.message ?: "Unknown API Error"))
            }

            val replyText = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty reply from OpenRouter API"))

            // 4. Save Assistant Response
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = replyText
            )
            chatDao.insertMessage(assistantMsg)

            // Update session's timestamp
            if (session != null) {
                chatDao.insertSession(session.copy(updatedAt = System.currentTimeMillis()))
            }

            Result.success(replyText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAndCacheModels(): Result<List<ModelCache>> = withContext(Dispatchers.IO) {
        try {
            val response = openRouterApi.getModels()
            val entities = response.data.map { dto ->
                // Parse pricing securely
                val pricingPrompt = dto.pricing?.prompt?.toDoubleOrNull() ?: 0.0
                val pricingCompletion = dto.pricing?.completion?.toDoubleOrNull() ?: 0.0

                ModelCache(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    contextLength = dto.context_length ?: 2048,
                    pricingPrompt = pricingPrompt * 1_000_000.0, // convert back to standard rates per million if desired, or keep direct
                    pricingCompletion = pricingCompletion * 1_000_000.0
                )
            }

            if (entities.isNotEmpty()) {
                modelDao.clearAllModels()
                modelDao.insertModels(entities)
            }
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -- Export features --

    suspend fun exportMarkdown(sessionId: Int): String = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId) ?: return@withContext "Chat Session not found."
        val messages = chatDao.getMessagesForSession(sessionId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val builder = StringBuilder()
        builder.append("# Chat: ${session.title}\n\n")
        builder.append("- **Model**: `${session.modelId}`\n")
        builder.append("- **Export Date**: ${dateFormat.format(Date())}\n")
        builder.append("- **Total Messages**: ${messages.size}\n\n")
        builder.append("---\n\n")

        for (msg in messages) {
            val sender = if (msg.role == "user") "🧑 **User**" else "🤖 **Assistant**"
            builder.append("$sender\n")
            builder.append("${msg.content}\n\n")
            builder.append("---\n\n")
        }

        builder.toString()
    }

    suspend fun exportJson(sessionId: Int): String = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId) ?: return@withContext "{}"
        val messages = chatDao.getMessagesForSession(sessionId)

        val backup = ChatBackup(
            title = session.title,
            modelId = session.modelId,
            messages = messages.map {
                BackupMessage(role = it.role, content = it.content, timestamp = it.timestamp)
            }
        )

        backupAdapter.toJson(backup)
    }

    // -- Import features --

    suspend fun importJson(jsonContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup = backupAdapter.fromJson(jsonContent)
                ?: return@withContext Result.failure(Exception("Failed to parse JSON file"))

            // 1. Create a session
            val sessionId = createNewSession(
                title = "${backup.title} (Imported)",
                modelId = backup.modelId
            )

            // 2. Insert messages
            for (msg in backup.messages) {
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = msg.role,
                        content = msg.content,
                        timestamp = msg.timestamp
                    )
                )
            }

            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
