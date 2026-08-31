package com.example.nfcnexus.ui.writer

import android.nfc.NdefMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.data.repository.TagRepository
import com.example.nfcnexus.nfc.NfcEvent
import com.example.nfcnexus.nfc.NfcManager
import com.example.nfcnexus.nfc.NfcOperationResult
import com.example.nfcnexus.nfc.NfcSessionMode
import com.example.nfcnexus.nfc.builder.NdefPayloadBuilder
import com.example.nfcnexus.nfc.builder.WifiTlvEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WritePayloadType(val label: String) {
    TEXT("Text Note"),
    URL("Web URL / Link"),
    WIFI("Wi-Fi Network"),
    VCARD("Business Card (vCard)"),
    MIME("Custom MIME"),
    AAR("App Launcher (AAR)")
}

data class WriterUiState(
    val selectedType: WritePayloadType = WritePayloadType.URL,
    // Text fields
    val textContent: String = "Hello from NFC Nexus!",
    val textLanguage: String = "en",
    // URL fields
    val urlContent: String = "https://github.com/developer/portfolio",
    // Wi-Fi fields
    val wifiSsid: String = "Nexus-Network",
    val wifiPassword: String = "NexusPass2026!",
    val wifiAuthType: WifiTlvEncoder.AuthType = WifiTlvEncoder.AuthType.WPA2_PERSONAL,
    // vCard fields
    val vcardName: String = "Alex Nexus",
    val vcardOrg: String = "Nexus Technologies",
    val vcardTitle: String = "Architect",
    val vcardPhone: String = "+1-555-0199",
    val vcardEmail: String = "alex@nexus.io",
    val vcardUrl: String = "https://nexus.io",
    val vcardAddress: String = "San Francisco, CA",
    val vcardNote: String = "NFC Developer",
    // MIME fields
    val mimeType: String = "application/json",
    val mimeContent: String = "{\"app\":\"NFC Nexus\",\"version\":\"1.0\"}",
    // AAR fields
    val aarPackageName: String = "com.example.nfcnexus",
    // Active session
    val isWriting: Boolean = false,
    val writeModeName: String = "Write Tag",
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val showLockConfirmDialog: Boolean = false
)

class NfcWriterViewModel(
    private val tagRepository: TagRepository,
    private val nfcManager: NfcManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriterUiState())
    val uiState: StateFlow<WriterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            nfcManager.events.collect { event ->
                when (event) {
                    is NfcEvent.WriteCompleted -> {
                        handleOperationResult(event.result)
                    }
                    is NfcEvent.FormatCompleted -> {
                        handleOperationResult(event.result)
                    }
                    is NfcEvent.LockCompleted -> {
                        handleOperationResult(event.result)
                    }
                    is NfcEvent.CloneCompleted -> {
                        handleOperationResult(event.result)
                    }
                    is NfcEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isWriting = false,
                                statusMessage = event.message,
                                isSuccess = false
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleOperationResult(result: NfcOperationResult) {
        when (result) {
            is NfcOperationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isWriting = false,
                        statusMessage = result.message,
                        isSuccess = true
                    )
                }
            }
            is NfcOperationResult.Error -> {
                _uiState.update {
                    it.copy(
                        isWriting = false,
                        statusMessage = result.message,
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun setPayloadType(type: WritePayloadType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun updateTextFields(text: String, lang: String = _uiState.value.textLanguage) {
        _uiState.update { it.copy(textContent = text, textLanguage = lang) }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(urlContent = url) }
    }

    fun updateWifi(ssid: String, pass: String, auth: WifiTlvEncoder.AuthType) {
        _uiState.update { it.copy(wifiSsid = ssid, wifiPassword = pass, wifiAuthType = auth) }
    }

    fun updateVCard(
        name: String, org: String, title: String,
        phone: String, email: String, url: String,
        address: String, note: String
    ) {
        _uiState.update {
            it.copy(
                vcardName = name, vcardOrg = org, vcardTitle = title,
                vcardPhone = phone, vcardEmail = email, vcardUrl = url,
                vcardAddress = address, vcardNote = note
            )
        }
    }

    fun updateMime(type: String, content: String) {
        _uiState.update { it.copy(mimeType = type, mimeContent = content) }
    }

    fun updateAar(pkg: String) {
        _uiState.update { it.copy(aarPackageName = pkg) }
    }

    fun startWriteSession() {
        val ndefMessage = buildCurrentNdefMessage()
        _uiState.update {
            it.copy(
                isWriting = true,
                writeModeName = "Writing Tag...",
                statusMessage = "Hold target NFC tag near the device to write payload."
            )
        }
        nfcManager.setMode(NfcSessionMode.Writing(ndefMessage, _uiState.value.selectedType.label))
    }

    fun startFormatEraseSession() {
        _uiState.update {
            it.copy(
                isWriting = true,
                writeModeName = "Erasing Tag...",
                statusMessage = "Approach NFC tag to format/erase into an empty NDEF tag."
            )
        }
        nfcManager.setMode(NfcSessionMode.Formatting)
    }

    fun requestLockTag() {
        _uiState.update { it.copy(showLockConfirmDialog = true) }
    }

    fun dismissLockDialog() {
        _uiState.update { it.copy(showLockConfirmDialog = false) }
    }

    fun confirmLockSession() {
        _uiState.update {
            it.copy(
                showLockConfirmDialog = false,
                isWriting = true,
                writeModeName = "Locking Tag (Permanent)...",
                statusMessage = "Approach NFC tag to permanently lock as Read-Only."
            )
        }
        nfcManager.setMode(NfcSessionMode.Locking)
    }

    fun startCloneWriteSession(ndefMessage: NdefMessage) {
        _uiState.update {
            it.copy(
                isWriting = true,
                writeModeName = "Writing Cloned Payload...",
                statusMessage = "Approach target NFC tag to write and verify cloned payload."
            )
        }
        nfcManager.setMode(NfcSessionMode.Cloning(ndefMessage))
    }

    fun cancelSession() {
        _uiState.update { it.copy(isWriting = false, statusMessage = null) }
        nfcManager.setMode(NfcSessionMode.Idle)
    }

    fun saveAsTemplate(title: String) {
        viewModelScope.launch {
            val record = buildCurrentParsedRecord()
            tagRepository.saveTemplate(title, "NFC Forum Type 2/4", listOf(record))
            _uiState.update { it.copy(statusMessage = "Saved template: $title", isSuccess = true) }
        }
    }

    fun buildCurrentNdefMessage(): NdefMessage {
        val s = _uiState.value
        val record = when (s.selectedType) {
            WritePayloadType.TEXT -> NdefPayloadBuilder.createTextRecord(s.textContent, s.textLanguage)
            WritePayloadType.URL -> NdefPayloadBuilder.createUriRecord(s.urlContent)
            WritePayloadType.WIFI -> NdefPayloadBuilder.createWifiRecord(s.wifiSsid, s.wifiPassword, s.wifiAuthType)
            WritePayloadType.VCARD -> NdefPayloadBuilder.createVCardRecord(
                fullName = s.vcardName,
                organization = s.vcardOrg,
                title = s.vcardTitle,
                phone = s.vcardPhone,
                email = s.vcardEmail,
                url = s.vcardUrl,
                note = s.vcardNote,
                address = s.vcardAddress
            )
            WritePayloadType.MIME -> NdefPayloadBuilder.createMimeRecord(s.mimeType, s.mimeContent.toByteArray())
            WritePayloadType.AAR -> NdefPayloadBuilder.createAarRecord(s.aarPackageName)
        }
        return NdefPayloadBuilder.buildMessage(record)
    }

    private fun buildCurrentParsedRecord(): ParsedRecord {
        val s = _uiState.value
        return when (s.selectedType) {
            WritePayloadType.TEXT -> ParsedRecord.Text(s.textContent, s.textLanguage, "UTF-8", "")
            WritePayloadType.URL -> ParsedRecord.Uri(s.urlContent, s.urlContent, "", "")
            WritePayloadType.WIFI -> ParsedRecord.Wifi(s.wifiSsid, s.wifiAuthType.name, "AES", s.wifiPassword, "", "")
            WritePayloadType.VCARD -> ParsedRecord.VCard(
                formattedName = s.vcardName,
                organization = s.vcardOrg,
                title = s.vcardTitle,
                phoneNumbers = listOf(s.vcardPhone).filter { it.isNotEmpty() },
                emails = listOf(s.vcardEmail).filter { it.isNotEmpty() },
                urls = listOf(s.vcardUrl).filter { it.isNotEmpty() },
                note = s.vcardNote,
                address = s.vcardAddress,
                rawVCard = "",
                rawBytesHex = ""
            )
            WritePayloadType.MIME -> ParsedRecord.Mime(s.mimeType, s.mimeContent, "", "")
            WritePayloadType.AAR -> ParsedRecord.Aar(s.aarPackageName, "")
        }
    }
}
