package com.anonym239.flohmarkt.data.repository

import com.anonym239.flohmarkt.data.local.entity.FavoriteEntity
import com.anonym239.flohmarkt.data.remote.dto.MarketEntryDto
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.util.LocationHelper
import java.util.UUID

fun MarketEntryDto.toDomain(
    isFavorite: Boolean = false,
    userLat: Double? = null,
    userLon: Double? = null,
    locationHelper: LocationHelper? = null
): MarketEntry {
    val distanceKm = if (userLat != null && userLon != null &&
        this.latitude != null && this.longitude != null &&
        locationHelper != null) {
        locationHelper.calculateDistanceKm(userLat, userLon, this.latitude, this.longitude)
    } else null

    return MarketEntry(
        id = this.id ?: UUID.randomUUID().toString(),
        title = this.title ?: "Unbekannte Veranstaltung",
        description = this.description ?: "",
        dateTime = this.dateTime ?: "Datum unbekannt",
        location = this.location ?: "",
        address = this.address ?: "",
        category = parseCategory(this.category ?: ""),
        url = this.url ?: "",
        imageUrl = this.imageUrl,
        isFavorite = isFavorite,
        distanceKm = distanceKm
    )
}

fun FavoriteEntity.toDomain(): MarketEntry {
    return MarketEntry(
        id = this.id,
        title = this.title,
        description = this.description,
        dateTime = this.dateTime,
        location = this.location,
        address = this.address,
        category = parseCategory(this.category),
        url = this.url,
        imageUrl = this.imageUrl,
        isFavorite = true,
        distanceKm = null
    )
}

fun MarketEntry.toEntity(): FavoriteEntity {
    return FavoriteEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        dateTime = this.dateTime,
        location = this.location,
        address = this.address,
        category = this.category.name,
        url = this.url,
        imageUrl = this.imageUrl
    )
}

fun parseCategory(value: String): Category {
    if (value.isBlank()) return Category.SONSTIGES
    return try {
        Category.valueOf(value.uppercase().trim())
    } catch (e: IllegalArgumentException) {
        when {
            value.contains("flohmarkt", ignoreCase = true) -> Category.FLOHMARKT
            value.contains("haushalt", ignoreCase = true) ||
            value.contains("auflösung", ignoreCase = true) ||
            value.contains("aufloesung", ignoreCase = true) -> Category.HAUSHALTSAUFLOESUNG
            value.contains("troedel", ignoreCase = true) ||
            value.contains("trödel", ignoreCase = true) -> Category.TROEDELMARKT
            else -> Category.SONSTIGES
        }
    }
}
