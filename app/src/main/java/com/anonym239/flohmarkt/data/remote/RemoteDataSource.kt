package com.anonym239.flohmarkt.data.remote

import android.util.Log
import com.anonym239.flohmarkt.data.remote.api.OpenRouterApiService
import com.anonym239.flohmarkt.data.remote.dto.MarketEntryDto
import com.anonym239.flohmarkt.data.remote.dto.Message
import com.anonym239.flohmarkt.data.remote.dto.OpenRouterRequest
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.DateFilter
import com.anonym239.flohmarkt.util.Constants
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteDataSource @Inject constructor(
    private val apiService: OpenRouterApiService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "RemoteDataSource"
    }

    suspend fun searchMarkets(
        query: String,
        location: String,
        category: Category,
        dateFilter: DateFilter
    ): List<MarketEntryDto> {
        val categoryText = when (category) {
            Category.ALL -> "Flohmärkte, Haushaltsauflösungen und Trödelmärkte"
            Category.FLOHMARKT -> "Flohmärkte"
            Category.HAUSHALTSAUFLOESUNG -> "Haushaltsauflösungen"
            Category.TROEDELMARKT -> "Trödelmärkte"
            Category.SONSTIGES -> "Secondhand-Veranstaltungen"
        }

        val dateText = when (dateFilter) {
            DateFilter.ALL -> "in den nächsten 4 Wochen"
            DateFilter.TODAY -> "heute"
            DateFilter.THIS_WEEK -> "diese Woche"
            DateFilter.THIS_MONTH -> "diesen Monat"
        }

        val locationText = if (location.isBlank()) "Deutschland" else location.trim()
        val queryExtra = if (query.isBlank()) "" else ", Suchbegriff: \"${query.trim()}\""

        val prompt = """
Erstelle eine Liste von genau 10 realistischen $categoryText$queryExtra in der Region $locationText, die $dateText stattfinden.

Antworte NUR mit einem JSON-Array. Kein Text davor oder danach. Kein Markdown. Nur das JSON-Array.

Format:
[
  {
    "id": "1",
    "title": "Name der Veranstaltung",
    "description": "Kurze Beschreibung in 2 Sätzen",
    "date_time": "Samstag, 10. Mai 2025, 08:00 - 14:00 Uhr",
    "location": "Stadtname",
    "address": "Straße Hausnummer, PLZ Stadtname",
    "category": "FLOHMARKT",
    "url": "https://www.flohmarkt.de/veranstaltung",
    "image_url": null,
    "latitude": 48.1351,
    "longitude": 11.5820
  }
]

Regeln:
- category muss exakt sein: FLOHMARKT, HAUSHALTSAUFLOESUNG, TROEDELMARKT oder SONSTIGES
- Verwende realistische Adressen in $locationText und Umgebung
- latitude/longitude müssen echte Koordinaten der Stadt sein
- URLs auf bekannte Plattformen: ebay-kleinanzeigen.de, flohmarkt.de, markt.de
- Antworte NUR mit dem JSON-Array, absolut kein anderer Text
        """.trimIndent()

        val request = OpenRouterRequest(
            model = Constants.OPENROUTER_MODEL,
            messages = listOf(
                Message(role = "user", content = prompt)
            ),
            maxTokens = 3000,
            temperature = 0.3
        )

        Log.d(TAG, "Sending request to OpenRouter, model: ${Constants.OPENROUTER_MODEL}, location: $locationText")

        val response = try {
            apiService.chatCompletion(request)
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}", e)
            throw Exception("Netzwerkfehler: Bitte Internetverbindung prüfen. (${e.message})")
        }

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unbekannter Fehler"
            Log.e(TAG, "API error ${response.code()}: $errorBody")
            val friendlyMessage = when (response.code()) {
                401 -> "API-Schlüssel ungültig. Bitte in den Einstellungen prüfen."
                402 -> "Kein Guthaben auf dem OpenRouter-Konto."
                429 -> "Zu viele Anfragen. Bitte kurz warten und erneut versuchen."
                500, 502, 503 -> "Server vorübergehend nicht erreichbar. Bitte später versuchen."
                else -> "Serverfehler (${response.code()}): $errorBody"
            }
            throw Exception(friendlyMessage)
        }

        val body = response.body()
            ?: throw Exception("Leere Antwort vom Server erhalten")

        // Prüfe auf API-Fehler im Body
        if (body.error != null) {
            val errMsg = body.error.message ?: "Unbekannter API-Fehler"
            Log.e(TAG, "OpenRouter error in body: $errMsg")
            throw Exception("API-Fehler: $errMsg")
        }

        val content = body.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: throw Exception("Keine Inhalte in der API-Antwort gefunden")

        Log.d(TAG, "Received content (first 200 chars): ${content.take(200)}")

        return parseMarketEntries(content)
    }

    private fun parseMarketEntries(content: String): List<MarketEntryDto> {
        // Bereinige den Content - entferne Markdown-Codeblöcke und Whitespace
        var cleanContent = content.trim()

        // Entferne ```json ... ``` oder ``` ... ```
        cleanContent = cleanContent
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Finde das JSON-Array (suche nach [ ... ])
        val startIdx = cleanContent.indexOf('[')
        val endIdx = cleanContent.lastIndexOf(']')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            cleanContent = cleanContent.substring(startIdx, endIdx + 1)
        }

        Log.d(TAG, "Parsing JSON (first 300 chars): ${cleanContent.take(300)}")

        return try {
            val type = object : TypeToken<List<MarketEntryDto>>() {}.type
            val result = gson.fromJson<List<MarketEntryDto>>(cleanContent, type)
            result?.filter { it.title != null } ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            // Versuche einzelnes Objekt
            try {
                val single = gson.fromJson(cleanContent, MarketEntryDto::class.java)
                if (single?.title != null) listOf(single) else emptyList()
            } catch (e2: Exception) {
                Log.e(TAG, "Single object parse also failed: ${e2.message}")
                throw Exception("Antwort konnte nicht verarbeitet werden. Bitte erneut versuchen.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected parse error: ${e.message}")
            throw Exception("Unerwarteter Fehler beim Verarbeiten der Antwort.")
        }
    }
}
