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
        val finalBytes = if (ndefBytes.isNotEmpty()) {
            ndefBytes
        } else {
            NdefPayloadBuilder.buildNdefMessageFromRecords(records).toByteArray()
        }
        _stagedTag.value = StagedCloneTag(
            title = title,
            sourceUid = "TEMPLATE",
            tagStandard = "NFC Forum Type 2/4",
            records = records,
            rawNdefBytes = finalBytes
        )
    }

    fun clearStagedTag() {
        _stagedTag.value = null
    }

    fun emulateStagedTag(): Boolean {
        val staged = _stagedTag.value ?: return false
        val finalBytes = if (staged.rawNdefBytes.isNotEmpty()) {
            staged.rawNdefBytes
        } else {
            NdefPayloadBuilder.buildNdefMessageFromRecords(staged.records).toByteArray()
        }
        val firstRecord = staged.records.firstOrNull()
        if (firstRecord != null) {
            EmulationRepository.setEmulatedCardFromRecord(
                record = firstRecord,
                customTitle = staged.title,
                ndefBytes = finalBytes
            )
        } else {
            EmulationRepository.setEmulatedPayload(
                title = staged.title,
                subtitle = "Cloned payload (${finalBytes.size} bytes)",
                payloadType = "Raw NDEF",
                ndefBytes = finalBytes
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
        return NdefPayloadBuilder.buildNdefMessageFromRecords(records)
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
