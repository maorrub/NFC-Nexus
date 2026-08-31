package com.example.nfcnexus.data.repository

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ApduLogEntry
import com.example.nfcnexus.data.model.ParsedRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets

data class EmulatedCardProfile(
    val id: String = "default_card",
    val title: String = "NFC Nexus Portfolio",
    val subtitle: String = "https://github.com/developer/portfolio",
    val payloadType: String = "URI / Web Link",
    val ndefBytes: ByteArray,
    val isEnabled: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EmulatedCardProfile
        if (id != other.id) return false
        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (payloadType != other.payloadType) return false
        if (isEnabled != other.isEnabled) return false
        if (!ndefBytes.contentEquals(other.ndefBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + payloadType.hashCode()
        result = 31 * result + ndefBytes.contentHashCode()
        result = 31 * result + isEnabled.hashCode()
        return result
    }
}

object EmulationRepository {

    // Default NDEF Message: URI Record to developer portfolio
    private val defaultNdefRecord = NdefRecord.createUri("https://github.com/developer/portfolio")
    private val defaultNdefMessage = NdefMessage(arrayOf(defaultNdefRecord))

    private val _currentCard = MutableStateFlow(
        EmulatedCardProfile(
            id = "default_1",
            title = "Developer Portfolio",
            subtitle = "https://github.com/developer/portfolio",
            payloadType = "URI / Web Link",
            ndefBytes = defaultNdefMessage.toByteArray(),
            isEnabled = true
        )
    )
    val currentCard: StateFlow<EmulatedCardProfile> = _currentCard.asStateFlow()

    private val _apduLogs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val apduLogs: StateFlow<List<ApduLogEntry>> = _apduLogs.asStateFlow()

    private val _totalApduTransactions = MutableStateFlow(0)
    val totalApduTransactions: StateFlow<Int> = _totalApduTransactions.asStateFlow()

    fun setEmulatedPayload(
        title: String,
        subtitle: String,
        payloadType: String,
        ndefBytes: ByteArray
    ) {
        _currentCard.update {
            it.copy(
                title = title,
                subtitle = subtitle,
                payloadType = payloadType,
                ndefBytes = ndefBytes
            )
        }
    }

    fun setEmulatedCardFromRecord(record: ParsedRecord, customTitle: String? = null, ndefBytes: ByteArray) {
        val (title, subtitle, type) = when (record) {
            is ParsedRecord.Uri -> Triple(customTitle ?: "Web Link", record.uri, "URI / Web Link")
            is ParsedRecord.Text -> Triple(customTitle ?: "Text Message", record.text, "Plain Text")
            is ParsedRecord.Wifi -> Triple(customTitle ?: "Wi-Fi Access", "SSID: ${record.ssid}", "Wi-Fi Config")
            is ParsedRecord.VCard -> Triple(customTitle ?: "Contact Card", record.formattedName, "vCard Contact")
            is ParsedRecord.Mime -> Triple(customTitle ?: "Custom MIME", record.mimeType, "MIME Media")
            is ParsedRecord.Aar -> Triple(customTitle ?: "App Launcher", record.packageName, "Android App")
            is ParsedRecord.Unknown -> Triple(customTitle ?: "Raw NDEF", record.tnfName, "Raw Record")
        }
        setEmulatedPayload(title, subtitle, type, ndefBytes)
    }

    fun toggleEmulationActive() {
        _currentCard.update { it.copy(isEnabled = !it.isEnabled) }
    }

    fun setEmulationActive(active: Boolean) {
        _currentCard.update { it.copy(isEnabled = active) }
    }

    fun logApduTransaction(
        commandName: String,
        commandHex: String,
        responseHex: String,
        statusCodeHex: String,
        isSuccess: Boolean,
        description: String = ""
    ) {
        val entry = ApduLogEntry(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            commandName = commandName,
            commandApduHex = commandHex,
            responseApduHex = responseHex,
            statusCodeHex = statusCodeHex,
            isSuccess = isSuccess,
            description = description
        )
        _apduLogs.update { (listOf(entry) + it).take(100) } // Keep last 100 entries
        _totalApduTransactions.update { it + 1 }
    }

    fun clearLogs() {
        _apduLogs.value = emptyList()
        _totalApduTransactions.value = 0
    }
}
