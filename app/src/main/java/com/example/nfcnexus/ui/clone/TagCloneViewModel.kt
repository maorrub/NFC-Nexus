package com.example.nfcnexus.ui.clone

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import androidx.lifecycle.ViewModel
import com.example.nfcnexus.data.model.NfcTagData
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.data.repository.EmulationRepository
import com.example.nfcnexus.nfc.builder.NdefPayloadBuilder
import com.example.nfcnexus.nfc.builder.WifiTlvEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StagedCloneTag(
    val title: String,
    val sourceUid: String,
    val tagStandard: String,
    val records: List<ParsedRecord>,
    val rawNdefBytes: ByteArray,
    val stagedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StagedCloneTag
        return rawNdefBytes.contentEquals(other.rawNdefBytes)
    }

    override fun hashCode(): Int {
        return rawNdefBytes.contentHashCode()
    }
}

class TagCloneViewModel : ViewModel() {

    private val _stagedTag = MutableStateFlow<StagedCloneTag?>(null)
    val stagedTag: StateFlow<StagedCloneTag?> = _stagedTag.asStateFlow()

    fun stageTagForCloning(tagData: NfcTagData) {
        val ndefBytes = if (!tagData.rawPayloadHex.isNullOrEmpty()) {
            hexStringToByteArray(tagData.rawPayloadHex)
        } else {
            // Reconstruct NdefMessage from parsed records
            buildNdefMessageFromRecords(tagData.records).toByteArray()
        }

        val firstRecord = tagData.records.firstOrNull()
        val title = when (firstRecord) {
            is ParsedRecord.Text -> firstRecord.text.take(24)
            is ParsedRecord.Uri -> firstRecord.title.ifEmpty { firstRecord.uri.take(24) }
            is ParsedRecord.Wifi -> "Wi-Fi: ${firstRecord.ssid}"
            is ParsedRecord.VCard -> "vCard: ${firstRecord.formattedName}"
            is ParsedRecord.Mime -> "MIME: ${firstRecord.mimeType}"
            is ParsedRecord.Aar -> "App: ${firstRecord.packageName.substringAfterLast('.')}"
            else -> "Tag ${tagData.uidHex.take(8)}"
        }

        _stagedTag.value = StagedCloneTag(
            title = title,
            sourceUid = tagData.uidHex,
            tagStandard = tagData.tagStandard,
            records = tagData.records,
            rawNdefBytes = ndefBytes
        )
    }

    fun stageDirectPayload(title: String, records: List<ParsedRecord>, ndefBytes: ByteArray) {
        _stagedTag.value = StagedCloneTag(
            title = title,
            sourceUid = "TEMPLATE",
            tagStandard = "NFC Forum Type 2/4",
            records = records,
            rawNdefBytes = ndefBytes
        )
    }

    fun clearStagedTag() {
        _stagedTag.value = null
    }

    fun emulateStagedTag(): Boolean {
        val staged = _stagedTag.value ?: return false
        val firstRecord = staged.records.firstOrNull()
        if (firstRecord != null) {
            EmulationRepository.setEmulatedCardFromRecord(
                record = firstRecord,
                customTitle = staged.title,
                ndefBytes = staged.rawNdefBytes
            )
        } else {
            EmulationRepository.setEmulatedPayload(
                title = staged.title,
                subtitle = "Cloned payload (${staged.rawNdefBytes.size} bytes)",
                payloadType = "Raw NDEF",
                ndefBytes = staged.rawNdefBytes
            )
        }
        EmulationRepository.setEmulationActive(true)
        return true
    }

    fun getStagedNdefMessage(): NdefMessage? {
        val bytes = _stagedTag.value?.rawNdefBytes ?: return null
        return try {
            NdefMessage(bytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildNdefMessageFromRecords(records: List<ParsedRecord>): NdefMessage {
        val ndefRecords = records.map { record ->
            when (record) {
                is ParsedRecord.Text -> NdefPayloadBuilder.createTextRecord(record.text, record.languageCode)
                is ParsedRecord.Uri -> NdefPayloadBuilder.createUriRecord(record.uri)
                is ParsedRecord.Wifi -> NdefPayloadBuilder.createWifiRecord(
                    ssid = record.ssid,
                    password = record.networkKey,
                    authType = if (record.authType.contains("WPA3")) WifiTlvEncoder.AuthType.WPA3_PERSONAL else WifiTlvEncoder.AuthType.WPA2_PERSONAL
                )
                is ParsedRecord.VCard -> NdefPayloadBuilder.createVCardRecord(
                    fullName = record.formattedName,
                    organization = record.organization,
                    title = record.title,
                    phone = record.phoneNumbers.firstOrNull() ?: "",
                    email = record.emails.firstOrNull() ?: "",
                    url = record.urls.firstOrNull() ?: "",
                    note = record.note,
                    address = record.address
                )
                is ParsedRecord.Mime -> NdefPayloadBuilder.createMimeRecord(
                    mimeType = record.mimeType,
                    data = record.contentString?.toByteArray() ?: hexStringToByteArray(record.payloadHex)
                )
                is ParsedRecord.Aar -> NdefPayloadBuilder.createAarRecord(record.packageName)
                is ParsedRecord.Unknown -> NdefPayloadBuilder.createTextRecord("Unknown Record")
            }
        }
        if (ndefRecords.isEmpty()) {
            return NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))
        }
        return NdefMessage(ndefRecords.toTypedArray())
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val clean = s.replace(":", "").replace(" ", "").replace("\n", "")
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
