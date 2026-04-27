package com.anonym239.flohmarkt.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.DateFilter
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.domain.repository.MarketRepository
import com.anonym239.flohmarkt.util.LocationHelper
import com.anonym239.flohmarkt.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val entries: List<MarketEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val location: String = "",
    val selectedCategory: Category = Category.ALL,
    val selectedDateFilter: DateFilter = DateFilter.ALL,
    val isRefreshing: Boolean = false,
    val hasSearched: Boolean = false,
    // GPS
    val userLat: Double? = null,
    val userLon: Double? = null,
    val isLocating: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val gpsCity: String? = null  // Stadt aus GPS
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MarketRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Prüfe ob Location-Permission bereits vorhanden
        if (locationHelper.hasLocationPermission()) {
            _uiState.update { it.copy(locationPermissionGranted = true) }
            fetchGpsLocation()
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(locationPermissionGranted = true) }
        fetchGpsLocation()
    }

    fun fetchGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            try {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    val city = locationHelper.getCityFromLocation(location)
                    _uiState.update {
                        it.copy(
                            userLat = location.latitude,
                            userLon = location.longitude,
                            gpsCity = city,
                            location = if (it.location.isBlank() && city != null) city else it.location,
                            isLocating = false
                        )
                    }
                    // Automatisch suchen wenn Ort per GPS gefunden
                    if (_uiState.value.location.isNotBlank()) {
                        search()
                    }
                } else {
                    _uiState.update { it.copy(isLocating = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLocating = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Debounced auto-search wenn bereits gesucht wurde
        if (_uiState.value.hasSearched) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(600)
                performSearch()
            }
        }
    }

    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun onLocationConfirmed() {
        // Wird aufgerufen wenn Nutzer Ort bestätigt (Enter/Button)
        if (_uiState.value.location.isNotBlank()) {
            search()
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (_uiState.value.hasSearched) {
            performSearch()
        }
    }

    fun onDateFilterSelected(dateFilter: DateFilter) {
        _uiState.update { it.copy(selectedDateFilter = dateFilter) }
        if (_uiState.value.hasSearched) {
            performSearch()
        }
    }

    fun search() {
        _uiState.update { it.copy(hasSearched = true) }
        performSearch()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        performSearch(isRefresh = true)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleFavorite(entry: MarketEntry) {
        viewModelScope.launch {
            try {
                if (entry.isFavorite) {
                    repository.removeFavorite(entry.id)
                } else {
                    repository.addFavorite(entry)
                }
                _uiState.update { state ->
                    state.copy(
                        entries = state.entries.map { e ->
                            if (e.id == entry.id) e.copy(isFavorite = !e.isFavorite) else e
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Favorit konnte nicht geändert werden")
                }
            }
        }
    }

    private fun performSearch(isRefresh: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val state = _uiState.value
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            when (val result = repository.searchMarkets(
                query = state.searchQuery,
                location = state.location,
                category = state.selectedCategory,
                dateFilter = state.selectedDateFilter,
                userLat = state.userLat,
                userLon = state.userLon
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            entries = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
