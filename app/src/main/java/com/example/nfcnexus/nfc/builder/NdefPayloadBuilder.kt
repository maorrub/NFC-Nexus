package com.example.nfcnexus.nfc.builder

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

object NdefPayloadBuilder {

    // RTD URI Prefix lookup table (reverse map for encoding)
    private val URI_PREFIX_ENTRIES = listOf(
        "http://www." to 0x01.toByte(),
        "https://www." to 0x02.toByte(),
        "http://" to 0x03.toByte(),
        "https://" to 0x04.toByte(),
        "tel:" to 0x05.toByte(),
        "mailto:" to 0x06.toByte(),
        "ftp://anonymous:anonymous@" to 0x07.toByte(),
        "ftp://ftp." to 0x08.toByte(),
        "ftps://" to 0x09.toByte(),
        "sftp://" to 0x0A.toByte(),
        "smb://" to 0x0B.toByte(),
        "nfs://" to 0x0C.toByte(),
        "dav://" to 0x0E.toByte(),
        "news:" to 0x0F.toByte(),
        "telnet://" to 0x10.toByte(),
        "imap:" to 0x11.toByte(),
        "rtsp://" to 0x12.toByte(),
        "urn:" to 0x13.toByte(),
        "pop:" to 0x14.toByte(),
        "sip:" to 0x15.toByte(),
        "sips:" to 0x16.toByte(),
        "tftp:" to 0x17.toByte(),
        "btspp://" to 0x18.toByte(),
        "btl2cap://" to 0x19.toByte(),
        "btgoep://" to 0x1A.toByte(),
        "tcpobex://" to 0x1B.toByte(),
        "irdaobex://" to 0x1C.toByte(),
        "file://" to 0x1D.toByte(),
        "urn:epc:id:" to 0x1E.toByte(),
        "urn:epc:tag:" to 0x1F.toByte(),
        "urn:epc:pat:" to 0x20.toByte(),
        "urn:epc:raw:" to 0x21.toByte(),
        "urn:epc:" to 0x22.toByte(),
        "urn:nfc:" to 0x23.toByte()
    )

    fun createTextRecord(text: String, languageCode: String = "en"): NdefRecord {
        val lang = languageCode.ifEmpty { Locale.getDefault().language }.lowercase()
        val langBytes = lang.toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)

        val statusByte = (langBytes.size and 0x3F).toByte() // UTF-8 bit 7 = 0
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = statusByte
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }

    fun createUriRecord(uriString: String): NdefRecord {
        var prefixCode: Byte = 0x00
        var remainingUri = uriString

        for ((prefix, code) in URI_PREFIX_ENTRIES) {
            if (uriString.startsWith(prefix, ignoreCase = true)) {
                prefixCode = code
                remainingUri = uriString.substring(prefix.length)
                break
            }
        }

        val uriBytes = remainingUri.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + uriBytes.size)
        payload[0] = prefixCode
        System.arraycopy(uriBytes, 0, payload, 1, uriBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_URI,
            ByteArray(0),
            payload
        )
    }

    fun createWifiRecord(
        ssid: String,
        password: String,
        authType: WifiTlvEncoder.AuthType = WifiTlvEncoder.AuthType.WPA2_PERSONAL
    ): NdefRecord {
        val tlvPayload = WifiTlvEncoder.encode(ssid, password, authType)
        val mimeType = "application/vnd.wfa.wsc".toByteArray(StandardCharsets.US_ASCII)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeType,
            ByteArray(0),
            tlvPayload
        )
    }

    fun createVCardRecord(
        fullName: String,
        organization: String = "",
        title: String = "",
        phone: String = "",
        email: String = "",
        url: String = "",
        note: String = "",
        address: String = ""
    ): NdefRecord {
        val vcardBuilder = StringBuilder()
        vcardBuilder.append("BEGIN:VCARD\r\n")
        vcardBuilder.append("VERSION:3.0\r\n")
        vcardBuilder.append("FN:$fullName\r\n")
        if (organization.isNotEmpty()) vcardBuilder.append("ORG:$organization\r\n")
        if (title.isNotEmpty()) vcardBuilder.append("TITLE:$title\r\n")
        if (phone.isNotEmpty()) vcardBuilder.append("TEL:$phone\r\n")
        if (email.isNotEmpty()) vcardBuilder.append("EMAIL:$email\r\n")
        if (url.isNotEmpty()) vcardBuilder.append("URL:$url\r\n")
        if (note.isNotEmpty()) vcardBuilder.append("NOTE:$note\r\n")
        if (address.isNotEmpty()) vcardBuilder.append("ADR:;;$address;;;;\r\n")
        vcardBuilder.append("END:VCARD\r\n")

        val payload = vcardBuilder.toString().toByteArray(StandardCharsets.UTF_8)
        val mimeType = "text/vcard".toByteArray(StandardCharsets.US_ASCII)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeType,
            ByteArray(0),
            payload
        )
    }

    fun createMimeRecord(mimeType: String, data: ByteArray): NdefRecord {
        val mimeBytes = mimeType.toByteArray(StandardCharsets.US_ASCII)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeBytes,
            ByteArray(0),
            data
        )
    }

    fun createAarRecord(packageName: String): NdefRecord {
        return NdefRecord.createApplicationRecord(packageName)
    }

    fun buildMessage(vararg records: NdefRecord): NdefMessage {
        return NdefMessage(records)
    }

    fun buildMessage(records: List<NdefRecord>): NdefMessage {
        return NdefMessage(records.toTypedArray())
    }
}
