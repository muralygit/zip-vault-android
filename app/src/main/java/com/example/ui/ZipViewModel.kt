package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ZipFileEntity
import com.example.data.ZipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ZipSortType {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC
}

data class ZipUiState(
    val zipFiles: List<ZipFileEntity> = emptyList(),
    val searchQuery: String = "",
    val sortType: ZipSortType = ZipSortType.DATE_DESC,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ZipViewModel(private val repository: ZipRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(ZipSortType.DATE_DESC)
    private val _isLoading = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Combine raw repository list with query and sorting logic
    val uiState: StateFlow<ZipUiState> = combine(
        repository.allZipFiles,
        _searchQuery,
        _sortType,
        _isLoading,
        _statusMessage,
        _errorMessage
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val rawFiles = flows[0] as List<ZipFileEntity>
        val query = flows[1] as String
        val sort = flows[2] as ZipSortType
        val loading = flows[3] as Boolean
        val status = flows[4] as String?
        val error = flows[5] as String?
        
        // Filter files
        val filtered = if (query.isBlank()) {
            rawFiles
        } else {
            rawFiles.filter { it.name.contains(query, ignoreCase = true) || it.tags.contains(query, ignoreCase = true) }
        }

        // Sort files
        val sorted = when (sort) {
            ZipSortType.DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            ZipSortType.DATE_ASC -> filtered.sortedBy { it.dateAdded }
            ZipSortType.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ZipSortType.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            ZipSortType.SIZE_DESC -> filtered.sortedByDescending { it.size }
            ZipSortType.SIZE_ASC -> filtered.sortedBy { it.size }
        }

        ZipUiState(
            zipFiles = sorted,
            searchQuery = query,
            sortType = sort,
            isLoading = loading,
            statusMessage = status,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZipUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sortType: ZipSortType) {
        _sortType.value = sortType
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun importZip(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.importZipFile(context, uri)
                .onSuccess { savedFile ->
                    _statusMessage.value = "Successfully imported \"${savedFile.name}\" to Vault"
                }
                .onFailure { error ->
                    _errorMessage.value = "Failed to import ZIP: ${error.localizedMessage ?: "Unknown error"}"
                }
            _isLoading.value = false
        }
    }

    fun updateTags(zipFile: ZipFileEntity, newTags: String) {
        viewModelScope.launch {
            repository.updateZipFile(zipFile.copy(tags = newTags))
            _statusMessage.value = "Tags updated for \"${zipFile.name}\""
        }
    }

    fun deleteZip(zipFile: ZipFileEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteZipFile(zipFile)
                .onSuccess {
                    _statusMessage.value = "Deleted \"${zipFile.name}\" from Vault"
                }
                .onFailure { error ->
                    _errorMessage.value = "Failed to delete file: ${error.localizedMessage ?: "Unknown error"}"
                }
            _isLoading.value = false
        }
    }
}

class ZipViewModelFactory(private val repository: ZipRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZipViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZipViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
