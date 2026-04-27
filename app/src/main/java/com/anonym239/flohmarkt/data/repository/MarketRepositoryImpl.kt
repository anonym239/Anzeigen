package com.anonym239.flohmarkt.data.repository

import com.anonym239.flohmarkt.data.local.dao.FavoriteDao
import com.anonym239.flohmarkt.data.remote.RemoteDataSource
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.DateFilter
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.domain.repository.MarketRepository
import com.anonym239.flohmarkt.util.LocationHelper
import com.anonym239.flohmarkt.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val favoriteDao: FavoriteDao,
    private val locationHelper: LocationHelper
) : MarketRepository {

    override suspend fun searchMarkets(
        query: String,
        location: String,
        category: Category,
        dateFilter: DateFilter,
        userLat: Double?,
        userLon: Double?
    ): Result<List<MarketEntry>> {
        return try {
            val dtos = remoteDataSource.searchMarkets(query, location, category, dateFilter)
            val favoriteIds = try {
                getFavoriteIds()
            } catch (e: Exception) {
                emptySet()
            }
            val entries = dtos.map { dto ->
                dto.toDomain(
                    isFavorite = favoriteIds.contains(dto.id),
                    userLat = userLat,
                    userLon = userLon,
                    locationHelper = locationHelper
                )
            }
            // Sortiere nach Entfernung wenn GPS verfügbar
            val sorted = if (userLat != null && userLon != null) {
                entries.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            } else entries
            Result.Success(sorted)
        } catch (e: Exception) {
            Result.Error(
                message = buildUserFriendlyError(e),
                throwable = e
            )
        }
    }

    override fun getFavorites(): Flow<List<MarketEntry>> {
        return favoriteDao.getAllFavorites()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun addFavorite(entry: MarketEntry): Result<Unit> {
        return try {
            favoriteDao.insertFavorite(entry.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                message = "Favorit konnte nicht gespeichert werden: ${e.localizedMessage}",
                throwable = e
            )
        }
    }

    override suspend fun removeFavorite(entryId: String): Result<Unit> {
        return try {
            favoriteDao.deleteFavoriteById(entryId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                message = "Favorit konnte nicht entfernt werden: ${e.localizedMessage}",
                throwable = e
            )
        }
    }

    override suspend fun isFavorite(entryId: String): Boolean {
        return try {
            favoriteDao.isFavorite(entryId) > 0
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getFavoriteIds(): Set<String> {
        return try {
            favoriteDao.getAllFavoriteIds().toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun buildUserFriendlyError(e: Exception): String {
        val message = e.message ?: e.localizedMessage ?: "Unbekannter Fehler"
        return when {
            message.contains("Unable to resolve host") ||
            message.contains("No address associated") ||
            message.contains("Network is unreachable") ->
                "Keine Internetverbindung. Bitte prüfen Sie Ihre Verbindung."
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ->
                "Die Anfrage hat zu lange gedauert. Bitte erneut versuchen."
            message.contains("401") || message.contains("403") ->
                "API-Schlüssel ungültig. Bitte prüfen Sie den API-Key."
            message.contains("402") ->
                "Kein Guthaben auf dem OpenRouter-Konto."
            message.contains("429") ->
                "Zu viele Anfragen. Bitte kurz warten und erneut versuchen."
            message.contains("500") || message.contains("502") || message.contains("503") ->
                "Server momentan nicht erreichbar. Bitte später versuchen."
            else -> message
        }
    }
}
