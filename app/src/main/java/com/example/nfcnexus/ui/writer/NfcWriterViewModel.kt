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
    PHOTO("Photo / Image"),
    WIFI("Wi-Fi Network"),
    VCARD("Business Card (vCard)"),
    MIME("Custom MIME"),
    AAR("App Launcher (AAR)")
}

enum class PhotoMode(val label: String) {
    WEB_URL("Direct Photo Link (All Phones)"),
    GALLERY_EMBED("Gallery Photo (Embedded)")
}

data class WriterUiState(
    val selectedType: WritePayloadType = WritePayloadType.URL,
    // Text fields
    val textContent: String = "Hello from NFC Nexus!",
    val textLanguage: String = "en",
    // URL fields
    val urlContent: String = "https://github.com/developer/portfolio",
    // Photo fields
    val photoMode: PhotoMode = PhotoMode.WEB_URL,
    val photoUrl: String = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&auto=format&fit=crop",
    val photoTitle: String = "My Photo",
    val photoBytes: ByteArray? = null,
    val photoBase64: String? = null,
    val photoMimeType: String = "image/jpeg",
    val photoByteSize: Int = 0,
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

    fun updatePhotoMode(mode: PhotoMode) {
        _uiState.update { it.copy(photoMode = mode) }
    }

    fun updatePhotoUrl(url: String, title: String = _uiState.value.photoTitle) {
        _uiState.update { it.copy(photoUrl = url, photoTitle = title) }
    }

    fun setGalleryImage(bytes: ByteArray, mimeType: String = "image/jpeg", title: String = "Gallery Photo") {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        _uiState.update {
            it.copy(
                photoBytes = bytes,
                photoBase64 = base64,
                photoMimeType = mimeType,
                photoTitle = title,
                photoByteSize = bytes.size
            )
        }
    }

    fun compressAndSetGalleryImage(context: android.content.Context, uri: android.net.Uri, maxTargetBytes: Int = 3500) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap == null) return@launch

                // Downsample to thumbnail (max dimension 120px) to fit tag budget
                val maxDim = 120
                val ratio = minOf(1.0f, maxDim.toFloat() / maxOf(originalBitmap.width, originalBitmap.height))
                val targetW = maxOf(1, (originalBitmap.width * ratio).toInt())
                val targetH = maxOf(1, (originalBitmap.height * ratio).toInt())
                val scaled = android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)

                var quality = 80
                var stream = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
                while (stream.size() > maxTargetBytes && quality > 20) {
                    quality -= 15
                    stream = java.io.ByteArrayOutputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
                }
                val compressedBytes = stream.toByteArray()
                setGalleryImage(compressedBytes, "image/jpeg", "Gallery Photo (${targetW}x${targetH})")
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to process photo: ${e.localizedMessage}") }
            }
        }
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
            WritePayloadType.PHOTO -> {
                if (s.photoMode == PhotoMode.GALLERY_EMBED && s.photoBytes != null && s.photoBytes.isNotEmpty()) {
                    NdefPayloadBuilder.createImageRecord(
                        uriString = "data:${s.photoMimeType};base64,${s.photoBase64}",
                        rawImageBytes = s.photoBytes,
                        mimeType = s.photoMimeType
                    )
                } else {
                    NdefPayloadBuilder.createUriRecord(s.photoUrl)
                }
            }
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
            WritePayloadType.PHOTO -> {
                if (s.photoMode == PhotoMode.GALLERY_EMBED && s.photoBytes != null && s.photoBytes.isNotEmpty()) {
                    ParsedRecord.Image(
                        uri = "data:${s.photoMimeType};base64,${s.photoBase64}",
                        title = s.photoTitle,
                        mimeType = s.photoMimeType,
                        base64Thumbnail = s.photoBase64,
                        byteSize = s.photoByteSize,
                        rawBytesHex = s.photoBytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                    )
                } else {
                    ParsedRecord.Image(
                        uri = s.photoUrl,
                        title = s.photoTitle,
                        mimeType = "image/jpeg",
                        base64Thumbnail = null,
                        byteSize = s.photoUrl.length,
                        rawBytesHex = ""
                    )
                }
            }
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
