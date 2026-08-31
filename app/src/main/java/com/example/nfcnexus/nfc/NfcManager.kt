package com.example.nfcnexus.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import com.example.nfcnexus.data.model.HexMemoryBlock
import com.example.nfcnexus.data.model.NfcTagData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NfcSessionMode {
    data object Idle : NfcSessionMode()
    data object Reading : NfcSessionMode()
    data class Writing(val message: NdefMessage, val targetTitle: String) : NfcSessionMode()
    data object Formatting : NfcSessionMode()
    data object Locking : NfcSessionMode()
    data class Cloning(val message: NdefMessage) : NfcSessionMode()
}

sealed class NfcEvent {
    data class TagScanned(val tagData: NfcTagData, val memoryBlocks: List<HexMemoryBlock>) : NfcEvent()
    data class WriteCompleted(val result: NfcOperationResult) : NfcEvent()
    data class FormatCompleted(val result: NfcOperationResult) : NfcEvent()
    data class LockCompleted(val result: NfcOperationResult) : NfcEvent()
    data class CloneCompleted(val result: NfcOperationResult, val verifiedTagData: NfcTagData?) : NfcEvent()
    data class Error(val message: String) : NfcEvent()
}

class NfcManager(private val context: Context) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private val _sessionMode = MutableStateFlow<NfcSessionMode>(NfcSessionMode.Idle)
    val sessionMode: StateFlow<NfcSessionMode> = _sessionMode.asStateFlow()

    private val _events = MutableSharedFlow<NfcEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<NfcEvent> = _events.asSharedFlow()

    val isNfcSupported: Boolean
        get() = nfcAdapter != null

    val isNfcEnabled: Boolean
        get() = nfcAdapter?.isEnabled == true

    fun setMode(mode: NfcSessionMode) {
        _sessionMode.value = mode
    }

    fun enableReaderMode(activity: Activity) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        nfcAdapter.enableReaderMode(activity, this, flags, options)
    }

    fun disableReaderMode(activity: Activity) {
        if (nfcAdapter == null) return
        try {
            nfcAdapter.disableReaderMode(activity)
        } catch (ignored: Exception) {}
    }

    override fun onTagDiscovered(tag: Tag) {
        when (val mode = _sessionMode.value) {
            is NfcSessionMode.Idle, is NfcSessionMode.Reading -> {
                try {
                    val tagData = NfcReader.readTagData(tag)
                    val memoryBlocks = NfcReader.dumpMemoryBlocks(tag)
                    _events.tryEmit(NfcEvent.TagScanned(tagData, memoryBlocks))
                } catch (e: Exception) {
                    _events.tryEmit(NfcEvent.Error("Failed to read NFC tag: ${e.localizedMessage}"))
                }
            }

            is NfcSessionMode.Writing -> {
                val result = NfcWriter.writeNdefMessage(tag, mode.message)
                _events.tryEmit(NfcEvent.WriteCompleted(result))
                _sessionMode.value = NfcSessionMode.Idle
            }

            is NfcSessionMode.Formatting -> {
                val result = NfcWriter.eraseTag(tag)
                _events.tryEmit(NfcEvent.FormatCompleted(result))
                _sessionMode.value = NfcSessionMode.Idle
            }

            is NfcSessionMode.Locking -> {
                val result = NfcWriter.makeReadOnly(tag)
                _events.tryEmit(NfcEvent.LockCompleted(result))
                _sessionMode.value = NfcSessionMode.Idle
            }

            is NfcSessionMode.Cloning -> {
                val result = NfcWriter.writeNdefMessage(tag, mode.message)
                val verifiedData = if (result is NfcOperationResult.Success) {
                    try { NfcReader.readTagData(tag) } catch (e: Exception) { null }
                } else null
                _events.tryEmit(NfcEvent.CloneCompleted(result, verifiedData))
                _sessionMode.value = NfcSessionMode.Idle
            }
        }
    }
}
