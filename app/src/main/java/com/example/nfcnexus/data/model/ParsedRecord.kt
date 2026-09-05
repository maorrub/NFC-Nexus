package com.example.nfcnexus.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ParsedRecord {
    abstract val rawBytesHex: String
    abstract val recordTypeName: String

    @Serializable
    data class Text(
        val text: String,
        val languageCode: String,
        val encoding: String,
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Text Note ($languageCode)"
    }

    @Serializable
    data class Uri(
        val uri: String,
        val title: String = "",
        val scheme: String = "",
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "URI / Web Link"
    }

    @Serializable
    data class Wifi(
        val ssid: String,
        val authType: String,
        val encryptionType: String,
        val networkKey: String,
        val macAddress: String = "",
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Wi-Fi Network Config"
    }

    @Serializable
    data class VCard(
        val formattedName: String,
        val organization: String = "",
        val title: String = "",
        val phoneNumbers: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val urls: List<String> = emptyList(),
        val note: String = "",
        val address: String = "",
        val rawVCard: String = "",
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Business Card (vCard)"
    }

    @Serializable
    data class Mime(
        val mimeType: String,
        val contentString: String?,
        val payloadHex: String,
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "MIME ($mimeType)"
    }

    @Serializable
    data class Aar(
        val packageName: String,
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Android App Record (AAR)"
    }

    @Serializable
    data class Image(
        val uri: String,
        val title: String = "Photo / Image",
        val mimeType: String = "image/jpeg",
        val base64Thumbnail: String? = null,
        val byteSize: Int = 0,
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Photo / Image"
    }

    @Serializable
    data class Unknown(
        val tnfName: String,
        val typeHex: String,
        val payloadHex: String,
        override val rawBytesHex: String
    ) : ParsedRecord() {
        override val recordTypeName: String get() = "Raw Record ($tnfName)"
    }
}
