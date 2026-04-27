package com.anonym239.flohmarkt.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<MarketEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        repository.getFavorites()
            .onEach { favorites ->
                _uiState.update {
                    it.copy(
                        favorites = favorites,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Favoriten konnten nicht geladen werden: ${e.localizedMessage}"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun removeFavorite(entry: MarketEntry) {
        viewModelScope.launch {
            try {
                repository.removeFavorite(entry.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Favorit konnte nicht entfernt werden")
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
