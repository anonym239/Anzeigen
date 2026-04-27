package com.anonym239.flohmarkt.domain.repository

import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.DateFilter
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.util.Result
import kotlinx.coroutines.flow.Flow

interface MarketRepository {

    suspend fun searchMarkets(
        query: String,
        location: String,
        category: Category,
        dateFilter: DateFilter,
        userLat: Double? = null,
        userLon: Double? = null
    ): Result<List<MarketEntry>>

    fun getFavorites(): Flow<List<MarketEntry>>

    suspend fun addFavorite(entry: MarketEntry): Result<Unit>

    suspend fun removeFavorite(entryId: String): Result<Unit>

    suspend fun isFavorite(entryId: String): Boolean
}
