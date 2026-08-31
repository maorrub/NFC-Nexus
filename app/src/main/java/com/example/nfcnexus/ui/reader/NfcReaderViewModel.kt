package com.example.nfcnexus.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcnexus.data.model.HexMemoryBlock
import com.example.nfcnexus.data.model.NfcTagData
import com.example.nfcnexus.data.repository.TagRepository
import com.example.nfcnexus.nfc.NfcEvent
import com.example.nfcnexus.nfc.NfcManager
import com.example.nfcnexus.nfc.NfcSessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val isScanning: Boolean = false,
    val scannedTag: NfcTagData? = null,
    val memoryBlocks: List<HexMemoryBlock> = emptyList(),
    val showMemoryInspector: Boolean = false,
    val isSavedToLibrary: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class NfcReaderViewModel(
    private val tagRepository: TagRepository,
    private val nfcManager: NfcManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            nfcManager.events.collect { event ->
                when (event) {
                    is NfcEvent.TagScanned -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scannedTag = event.tagData,
                                memoryBlocks = event.memoryBlocks,
                                isSavedToLibrary = false,
                                statusMessage = "Tag read successfully! (${event.tagData.records.size} records)",
                                isError = false
                            )
                        }
                        // Auto-save to scanned history
                        tagRepository.saveScannedTag(event.tagData)
                    }
                    is NfcEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                statusMessage = event.message,
                                isError = true
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startScanning() {
        _uiState.update { it.copy(isScanning = true, statusMessage = "Hold NFC tag near the back of device...") }
        nfcManager.setMode(NfcSessionMode.Reading)
    }

    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
        nfcManager.setMode(NfcSessionMode.Idle)
    }

    fun saveTagToLibrary(customTitle: String? = null) {
        val tag = _uiState.value.scannedTag ?: return
        viewModelScope.launch {
            tagRepository.saveScannedTag(tag, customTitle)
            _uiState.update { it.copy(isSavedToLibrary = true, statusMessage = "Tag saved to library!") }
        }
    }

    fun setShowMemoryInspector(show: Boolean) {
        _uiState.update { it.copy(showMemoryInspector = show) }
    }

    fun clearScannedTag() {
        _uiState.update {
            it.copy(
                scannedTag = null,
                memoryBlocks = emptyList(),
                showMemoryInspector = false,
                isSavedToLibrary = false,
                statusMessage = null
            )
        }
    }
}
