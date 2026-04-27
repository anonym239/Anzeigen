package com.anonym239.flohmarkt.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val entry: MarketEntry? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // Cache für die aktuelle Suche - wird vom HomeViewModel übergeben
    companion object {
        // Statischer Cache für Einträge zwischen Screens
        private val entryCache = mutableMapOf<String, MarketEntry>()

        fun cacheEntry(entry: MarketEntry) {
            entryCache[entry.id] = entry
        }

        fun clearCache() {
            entryCache.clear()
        }
    }

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            try {
                // Zuerst aus Cache laden
                val cachedEntry = entryCache[entryId]
                if (cachedEntry != null) {
                    // Prüfe ob es ein Favorit ist
                    val isFav = try {
                        repository.isFavorite(entryId)
                    } catch (e: Exception) {
                        cachedEntry.isFavorite
                    }
                    _uiState.update {
                        it.copy(entry = cachedEntry.copy(isFavorite = isFav))
                    }
                    return@launch
                }

                // Aus Favoriten laden falls nicht im Cache
                try {
                    val favorites = repository.getFavorites()
                        .catch { emit(emptyList()) }
                        .first()
                    val favoriteEntry = favorites.find { it.id == entryId }
                    if (favoriteEntry != null) {
                        _uiState.update { it.copy(entry = favoriteEntry) }
                    } else {
                        _uiState.update {
                            it.copy(errorMessage = "Eintrag nicht gefunden")
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Eintrag konnte nicht geladen werden: ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Unerwarteter Fehler: ${e.localizedMessage}")
                }
            }
        }
    }

    fun toggleFavorite(entry: MarketEntry) {
        viewModelScope.launch {
            try {
                if (entry.isFavorite) {
                    repository.removeFavorite(entry.id)
                } else {
                    repository.addFavorite(entry)
                }
                val updatedEntry = entry.copy(isFavorite = !entry.isFavorite)
                _uiState.update { it.copy(entry = updatedEntry) }
                // Cache aktualisieren
                entryCache[entry.id] = updatedEntry
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Favorit konnte nicht geändert werden: ${e.localizedMessage}")
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
