package com.anonym239.flohmarkt.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Request ──────────────────────────────────────────────────────────────────

data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 3000,
    val temperature: Double = 0.3,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class ResponseFormat(val type: String = "text")

data class Message(
    val role: String,
    val content: String
)

// ── Response ─────────────────────────────────────────────────────────────────

data class OpenRouterResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: OpenRouterError?,
    val usage: Usage?
)

data class Choice(
    val message: ResponseMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ResponseMessage(
    val role: String?,
    val content: String?
)

data class OpenRouterError(
    val message: String?,
    val type: String?,
    val code: Any?  // kann String oder Int sein
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

// ── Parsed Market Entry from AI ───────────────────────────────────────────────

data class MarketEntryDto(
    val id: String?,
    val title: String?,
    val description: String?,
    @SerializedName("date_time") val dateTime: String?,
    val location: String?,
    val address: String?,
    val category: String?,
    val url: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val latitude: Double?,   // optionale Koordinaten für Entfernungsberechnung
    val longitude: Double?
)

data class MarketEntriesWrapper(
    val entries: List<MarketEntryDto>
)
