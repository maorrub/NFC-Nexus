package com.example.nfcnexus.ui.emulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcnexus.data.local.TagEntity
import com.example.nfcnexus.data.model.ApduLogEntry
import com.example.nfcnexus.data.repository.EmulatedCardProfile
import com.example.nfcnexus.data.repository.EmulationRepository
import com.example.nfcnexus.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NfcEmulateViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    val currentCard: StateFlow<EmulatedCardProfile> = EmulationRepository.currentCard
    val apduLogs: StateFlow<List<ApduLogEntry>> = EmulationRepository.apduLogs
    val totalTransactions: StateFlow<Int> = EmulationRepository.totalApduTransactions

    val savedTags: StateFlow<List<TagEntity>> = tagRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEmulation() {
        EmulationRepository.toggleEmulationActive()
    }

    fun clearLogs() {
        EmulationRepository.clearLogs()
    }

    fun selectTagToEmulate(tag: TagEntity) {
        val records = tagRepository.parseRecords(tag)
        val firstRecord = records.firstOrNull()

        val rawNdef = if (tag.rawNdefHex.isNotEmpty()) {
            hexStringToByteArray(tag.rawNdefHex)
        } else {
            byteArrayOf()
        }

        val ndefBytes = if (rawNdef.isNotEmpty()) {
            rawNdef
        } else {
            com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.buildNdefMessageFromRecords(records).toByteArray()
        }

        if (firstRecord != null && ndefBytes.isNotEmpty()) {
            EmulationRepository.setEmulatedCardFromRecord(firstRecord, tag.title, ndefBytes)
        } else {
            EmulationRepository.setEmulatedPayload(
                title = tag.title,
                subtitle = "${records.size} NDEF Records",
                payloadType = tag.tagType,
                ndefBytes = ndefBytes
            )
        }
        EmulationRepository.setEmulationActive(true)
    }

    private fun buildNdefMessageFromRecords(records: List<com.example.nfcnexus.data.model.ParsedRecord>): android.nfc.NdefMessage {
        return com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.buildNdefMessageFromRecords(records)
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val clean = s.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        val len = clean.length
        if (len < 2) return ByteArray(0)
        val safeLen = if (len % 2 != 0) len - 1 else len
        val data = ByteArray(safeLen / 2)
        var i = 0
        while (i < safeLen) {
            val high = Character.digit(clean[i], 16)
            val low = Character.digit(clean[i + 1], 16)
            if (high == -1 || low == -1) break
            data[i / 2] = ((high shl 4) or low).toByte()
            i += 2
        }
        return data
    }
}
