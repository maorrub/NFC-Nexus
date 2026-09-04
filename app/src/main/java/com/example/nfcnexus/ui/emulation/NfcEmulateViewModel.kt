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

        val ndefBytes = if (tag.rawNdefHex.isNotEmpty()) {
            hexStringToByteArray(tag.rawNdefHex)
        } else {
            buildNdefMessageFromRecords(records).toByteArray()
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
        val ndefRecords = records.map { record ->
            when (record) {
                is com.example.nfcnexus.data.model.ParsedRecord.Text -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createTextRecord(record.text, record.languageCode)
                is com.example.nfcnexus.data.model.ParsedRecord.Uri -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createUriRecord(record.uri)
                is com.example.nfcnexus.data.model.ParsedRecord.Wifi -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createWifiRecord(
                    ssid = record.ssid,
                    password = record.networkKey,
                    authType = if (record.authType.contains("WPA3")) com.example.nfcnexus.nfc.builder.WifiTlvEncoder.AuthType.WPA3_PERSONAL else com.example.nfcnexus.nfc.builder.WifiTlvEncoder.AuthType.WPA2_PERSONAL
                )
                is com.example.nfcnexus.data.model.ParsedRecord.VCard -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createVCardRecord(
                    fullName = record.formattedName,
                    organization = record.organization,
                    title = record.title,
                    phone = record.phoneNumbers.firstOrNull() ?: "",
                    email = record.emails.firstOrNull() ?: "",
                    url = record.urls.firstOrNull() ?: "",
                    note = record.note,
                    address = record.address
                )
                is com.example.nfcnexus.data.model.ParsedRecord.Mime -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createMimeRecord(
                    mimeType = record.mimeType,
                    data = record.contentString?.toByteArray() ?: hexStringToByteArray(record.payloadHex)
                )
                is com.example.nfcnexus.data.model.ParsedRecord.Aar -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createAarRecord(record.packageName)
                is com.example.nfcnexus.data.model.ParsedRecord.Unknown -> com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.createTextRecord("Unknown Record")
            }
        }
        if (ndefRecords.isEmpty()) {
            return android.nfc.NdefMessage(android.nfc.NdefRecord(android.nfc.NdefRecord.TNF_EMPTY, null, null, null))
        }
        return android.nfc.NdefMessage(ndefRecords.toTypedArray())
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
