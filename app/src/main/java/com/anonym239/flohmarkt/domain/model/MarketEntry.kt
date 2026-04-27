package com.anonym239.flohmarkt.domain.model

data class MarketEntry(
    val id: String,
    val title: String,
    val description: String,
    val dateTime: String,
    val location: String,
    val address: String,
    val category: Category,
    val url: String,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val distanceKm: Double? = null  // Entfernung vom Nutzerstandort in km
)

enum class Category(val displayName: String) {
    ALL("Alles"),
    FLOHMARKT("Flohmarkt"),
    HAUSHALTSAUFLOESUNG("Haushaltsauflösung"),
    TROEDELMARKT("Trödelmarkt"),
    SONSTIGES("Sonstiges")
}

enum class DateFilter(val displayName: String) {
    ALL("Alle Termine"),
    TODAY("Heute"),
    THIS_WEEK("Diese Woche"),
    THIS_MONTH("Diesen Monat")
}
