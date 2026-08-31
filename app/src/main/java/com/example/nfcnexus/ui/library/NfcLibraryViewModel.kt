package com.example.nfcnexus.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcnexus.data.local.TagEntity
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.data.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    ALL("All Items"),
    FAVORITES("Favorites"),
    SCANNED("Scan History"),
    TEMPLATES("Templates")
}

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val searchQuery: String = "",
    val exportJsonDialogContent: String? = null,
    val showImportDialog: Boolean = false,
    val statusMessage: String? = null
)

class NfcLibraryViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagsList: StateFlow<List<TagEntity>> = _uiState
        .flatMapLatest { state ->
            val baseFlow = when {
                state.searchQuery.isNotEmpty() -> tagRepository.searchTags(state.searchQuery)
                state.selectedFilter == LibraryFilter.FAVORITES -> tagRepository.favoriteTags
                state.selectedFilter == LibraryFilter.SCANNED -> tagRepository.getTagsByCategory("SCANNED")
                state.selectedFilter == LibraryFilter.TEMPLATES -> tagRepository.getTagsByCategory("TEMPLATE")
                else -> tagRepository.allTags
            }
            baseFlow
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.toggleFavorite(tag)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }

    fun clearScannedHistory() {
        viewModelScope.launch {
            tagRepository.clearScannedHistory()
            _uiState.update { it.copy(statusMessage = "Cleared scan history") }
        }
    }

    fun exportTag(tag: TagEntity) {
        val jsonStr = tagRepository.exportToJson(tag)
        _uiState.update { it.copy(exportJsonDialogContent = jsonStr) }
    }

    fun exportAll() {
        val allTags = tagsList.value
        val jsonStr = tagRepository.exportAllToJson(allTags)
        _uiState.update { it.copy(exportJsonDialogContent = jsonStr) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportJsonDialogContent = null) }
    }

    fun showImportDialog(show: Boolean) {
        _uiState.update { it.copy(showImportDialog = show) }
    }

    fun importFromJson(jsonStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = tagRepository.importFromJson(jsonStr)
            result.onSuccess { count ->
                _uiState.update { it.copy(showImportDialog = false, statusMessage = "Imported $count tag(s) successfully!") }
                onResult(true, "Successfully imported $count tag(s)")
            }.onFailure { error ->
                onResult(false, "Import failed: ${error.localizedMessage}")
            }
        }
    }

    fun parseRecords(tag: TagEntity): List<ParsedRecord> = tagRepository.parseRecords(tag)
}
