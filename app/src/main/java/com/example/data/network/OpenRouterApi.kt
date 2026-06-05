package com.example.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @GET("api/v1/models")
    suspend fun getModels(): OpenRouterModelsResponse

    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Header("HTTP-Referer") referer: String = "https://ai.studio.build/openrouter-chat",
        @Header("X-Title") title: String = "OpenRouter Chat Bot",
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

@JsonClass(generateAdapter = true)
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelDto>
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val context_length: Int? = null,
    val pricing: OpenRouterPricing? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterPricing(
    val prompt: String? = null,
    val completion: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<NetworkMessage>,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NetworkMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<NetworkChoice>? = null,
    val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class NetworkChoice(
    val message: NetworkMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    val message: String?,
    val code: Int? = null
)
