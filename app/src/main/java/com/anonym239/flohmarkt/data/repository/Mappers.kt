package com.anonym239.flohmarkt.data.repository

import com.anonym239.flohmarkt.data.local.entity.FavoriteEntity
import com.anonym239.flohmarkt.data.remote.dto.MarketEntryDto
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.MarketEntry

fun MarketEntryDto.toDomain(isFavorite: Boolean = false): MarketEntry {
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
        isFavorite = isFavorite
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
        isFavorite = true
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

private fun parseCategory(value: String): Category {
    return try {
        Category.valueOf(value.uppercase().trim())
    } catch (e: IllegalArgumentException) {
        when {
            value.contains("flohmarkt", ignoreCase = true) -> Category.FLOHMARKT
            value.contains("haushalt", ignoreCase = true) -> Category.HAUSHALTSAUFLOESUNG
            value.contains("troedel", ignoreCase = true) ||
            value.contains("trödel", ignoreCase = true) -> Category.TROEDELMARKT
            else -> Category.SONSTIGES
        }
    }
}
