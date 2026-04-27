package com.anonym239.flohmarkt.data.remote

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

    suspend fun searchMarkets(
        query: String,
        location: String,
        category: Category,
        dateFilter: DateFilter
    ): List<MarketEntryDto> {
        val categoryText = when (category) {
            Category.ALL -> "Flohmärkte, Haushaltsauflösungen, Trödelmärkte und ähnliche Veranstaltungen"
            Category.FLOHMARKT -> "Flohmärkte"
            Category.HAUSHALTSAUFLOESUNG -> "Haushaltsauflösungen"
            Category.TROEDELMARKT -> "Trödelmärkte"
            Category.SONSTIGES -> "sonstige Secondhand-Veranstaltungen"
        }

        val dateText = when (dateFilter) {
            DateFilter.ALL -> "in den nächsten Wochen"
            DateFilter.TODAY -> "heute"
            DateFilter.THIS_WEEK -> "diese Woche"
            DateFilter.THIS_MONTH -> "diesen Monat"
        }

        val locationText = if (location.isBlank()) "Deutschland" else location
        val queryText = if (query.isBlank()) "" else " mit dem Suchbegriff \"$query\""

        val systemPrompt = """
            Du bist ein hilfreicher Assistent, der Informationen über Flohmärkte, 
            Haushaltsauflösungen und Trödelmärkte in Deutschland zusammenstellt.
            Antworte IMMER ausschließlich mit einem validen JSON-Array im folgenden Format, 
            ohne zusätzlichen Text, Erklärungen oder Markdown-Codeblöcke:
            [
              {
                "id": "eindeutige_id_string",
                "title": "Titel der Veranstaltung",
                "description": "Ausführliche Beschreibung (2-3 Sätze)",
                "date_time": "Datum und Uhrzeit als lesbarer Text, z.B. Samstag, 15. Juni 2025, 08:00 - 16:00 Uhr",
                "location": "Stadt/Ort",
                "address": "Vollständige Adresse",
                "category": "FLOHMARKT oder HAUSHALTSAUFLOESUNG oder TROEDELMARKT oder SONSTIGES",
                "url": "https://beispiel.de/veranstaltung",
                "image_url": null
              }
            ]
        """.trimIndent()

        val userPrompt = """
            Erstelle eine Liste von 10 realistischen $categoryText$queryText 
            in der Region $locationText, die $dateText stattfinden.
            Gib mir die Ergebnisse als JSON-Array zurück. 
            Verwende realistische deutsche Städtenamen, Adressen und Veranstaltungsdetails.
            Die URLs sollen auf bekannte Plattformen wie ebay-kleinanzeigen.de, 
            flohmarkt.de, markt.de oder lokale Stadtportale verweisen.
            Antworte NUR mit dem JSON-Array, kein anderer Text.
        """.trimIndent()

        val request = OpenRouterRequest(
            model = Constants.OPENROUTER_MODEL,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            maxTokens = 4000,
            temperature = 0.4
        )

        val response = apiService.chatCompletion(request)

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unbekannter Serverfehler"
            throw Exception("API-Fehler ${response.code()}: $errorBody")
        }

        val body = response.body()
            ?: throw Exception("Leere Antwort vom Server erhalten")

        if (body.error != null) {
            throw Exception("OpenRouter Fehler: ${body.error.message ?: "Unbekannter Fehler"}")
        }

        val content = body.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: throw Exception("Keine Inhalte in der API-Antwort gefunden")

        return parseMarketEntries(content)
    }

    private fun parseMarketEntries(content: String): List<MarketEntryDto> {
        // Bereinige den Content - entferne mögliche Markdown-Codeblöcke
        val cleanContent = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val type = object : TypeToken<List<MarketEntryDto>>() {}.type
            gson.fromJson<List<MarketEntryDto>>(cleanContent, type)
                ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // Versuche, ein einzelnes Objekt zu parsen falls Array-Parsing fehlschlägt
            try {
                val single = gson.fromJson(cleanContent, MarketEntryDto::class.java)
                listOf(single)
            } catch (e2: Exception) {
                throw Exception("Antwort konnte nicht verarbeitet werden: ${e.message}")
            }
        }
    }
}
