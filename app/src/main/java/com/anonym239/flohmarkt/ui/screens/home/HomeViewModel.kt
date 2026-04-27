package com.anonym239.flohmarkt.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonym239.flohmarkt.domain.model.Category
import com.anonym239.flohmarkt.domain.model.DateFilter
import com.anonym239.flohmarkt.domain.model.MarketEntry
import com.anonym239.flohmarkt.domain.repository.MarketRepository
import com.anonym239.flohmarkt.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Auto-search when filters change (debounced)
        _uiState
            .debounce(300)
            .distinctUntilChanged { old, new ->
                old.searchQuery == new.searchQuery &&
                old.location == new.location &&
                old.selectedCategory == new.selectedCategory &&
                old.selectedDateFilter == new.selectedDateFilter
            }
            .onEach { state ->
                if (state.hasSearched) {
                    performSearch()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (_uiState.value.hasSearched) {
            search()
        }
    }

    fun onDateFilterSelected(dateFilter: DateFilter) {
        _uiState.update { it.copy(selectedDateFilter = dateFilter) }
        if (_uiState.value.hasSearched) {
            search()
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
                // Update the entry in the list
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
        viewModelScope.launch {
            val state = _uiState.value
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            when (val result = repository.searchMarkets(
                query = state.searchQuery,
                location = state.location,
                category = state.selectedCategory,
                dateFilter = state.selectedDateFilter
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
