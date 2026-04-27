package com.anonym239.flohmarkt.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Request ──────────────────────────────────────────────────────────────────

data class OpenRouterRequest(
    val model: String = "openai/gpt-4o-mini",
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 4000,
    val temperature: Double = 0.3
)

data class Message(
    val role: String,
    val content: String
)

// ── Response ─────────────────────────────────────────────────────────────────

data class OpenRouterResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: OpenRouterError?
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
    val code: String?
)

// ── Parsed Market Entry from AI ───────────────────────────────────────────────

data class MarketEntryDto(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("date_time") val dateTime: String,
    val location: String,
    val address: String,
    val category: String,
    val url: String,
    @SerializedName("image_url") val imageUrl: String?
)

data class MarketEntriesWrapper(
    val entries: List<MarketEntryDto>
)
