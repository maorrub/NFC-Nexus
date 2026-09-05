package com.example.nfcnexus.nfc.parser

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import java.nio.charset.StandardCharsets
import java.util.Arrays

object NdefMessageParser {

    fun parse(message: NdefMessage?): List<ParsedRecord> {
        if (message == null) return emptyList()
        return message.records.mapNotNull { parseRecord(it) }
    }

    fun parseRecord(record: NdefRecord): ParsedRecord {
        val tnf = record.tnf
        val type = record.type ?: byteArrayOf()
        val payload = record.payload ?: byteArrayOf()
        val rawHex = payload.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

        return when (tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                when {
                    Arrays.equals(type, NdefRecord.RTD_TEXT) -> {
                        TextRecordParser.parse(record)
                    }
                    Arrays.equals(type, NdefRecord.RTD_URI) -> {
                        val uriRecord = UriRecordParser.parse(record)
                        val lowerUri = uriRecord.uri.lowercase()
                        if (lowerUri.startsWith("data:image/") ||
                            lowerUri.endsWith(".jpg") || lowerUri.endsWith(".jpeg") ||
                            lowerUri.endsWith(".png") || lowerUri.endsWith(".webp") ||
                            lowerUri.endsWith(".gif") || lowerUri.contains("/image/")
                        ) {
                            val base64Thumb = if (lowerUri.startsWith("data:image/")) {
                                uriRecord.uri.substringAfter("base64,")
                            } else null
                            ParsedRecord.Image(
                                uri = uriRecord.uri,
                                title = uriRecord.title.ifEmpty { "Photo / Image" },
                                mimeType = if (lowerUri.contains("png")) "image/png" else "image/jpeg",
                                base64Thumbnail = base64Thumb,
                                byteSize = payload.size,
                                rawBytesHex = rawHex
                            )
                        } else {
                            uriRecord
                        }
                    }
                    Arrays.equals(type, NdefRecord.RTD_SMART_POSTER) -> {
                        // Smart poster often contains an inner NDEF message
                        try {
                            val innerMsg = NdefMessage(payload)
                            val innerParsed = parse(innerMsg)
                            innerParsed.firstOrNull { it is ParsedRecord.Uri || it is ParsedRecord.Image }
                                ?: innerParsed.firstOrNull()
                                ?: ParsedRecord.Unknown("TNF_WELL_KNOWN (Smart Poster)", type.joinToString(""){ "%02X".format(it.toInt() and 0xFF) }, rawHex, rawHex)
                        } catch (e: Exception) {
                            ParsedRecord.Unknown("TNF_WELL_KNOWN (Smart Poster)", type.joinToString(""){ "%02X".format(it.toInt() and 0xFF) }, rawHex, rawHex)
                        }
                    }
                    else -> {
                        ParsedRecord.Unknown("TNF_WELL_KNOWN", type.joinToString(""){ "%02X".format(it.toInt() and 0xFF) }, rawHex, rawHex)
                    }
                }
            }

            NdefRecord.TNF_MIME_MEDIA -> {
                val mimeType = String(type, StandardCharsets.US_ASCII).lowercase()
                when {
                    mimeType.startsWith("image/") -> {
                        val base64 = android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP)
                        val dataUri = "data:$mimeType;base64,$base64"
                        ParsedRecord.Image(
                            uri = dataUri,
                            title = "Embedded Photo (${payload.size}B)",
                            mimeType = mimeType,
                            base64Thumbnail = base64,
                            byteSize = payload.size,
                            rawBytesHex = rawHex
                        )
                    }
                    mimeType == "application/vnd.wfa.wsc" -> {
                        WifiRecordParser.parse(record)
                    }
                    mimeType == "text/vcard" || mimeType == "text/x-vcard" -> {
                        VCardParser.parse(record)
                    }
                    mimeType.startsWith("text/") || mimeType == "application/json" || mimeType == "application/xml" -> {
                        val text = try {
                            String(payload, StandardCharsets.UTF_8)
                        } catch (e: Exception) {
                            null
                        }
                        ParsedRecord.Mime(mimeType, text, rawHex, rawHex)
                    }
                    else -> {
                        ParsedRecord.Mime(mimeType, null, rawHex, rawHex)
                    }
                }
            }

            NdefRecord.TNF_ABSOLUTE_URI -> {
                val uriStr = String(type, StandardCharsets.UTF_8)
                ParsedRecord.Uri(
                    uri = uriStr,
                    title = uriStr.take(30),
                    scheme = uriStr.substringBefore(":", ""),
                    rawBytesHex = rawHex
                )
            }

            NdefRecord.TNF_EXTERNAL_TYPE -> {
                val extType = String(type, StandardCharsets.US_ASCII)
                if (extType.equals("android.com:pkg", ignoreCase = true)) {
                    val pkgName = String(payload, StandardCharsets.UTF_8)
                    ParsedRecord.Aar(pkgName, rawHex)
                } else {
                    ParsedRecord.Unknown("TNF_EXTERNAL ($extType)", type.joinToString(""){ "%02X".format(it.toInt() and 0xFF) }, rawHex, rawHex)
                }
            }

            NdefRecord.TNF_EMPTY -> {
                ParsedRecord.Unknown("TNF_EMPTY", "", "", "")
            }

            else -> {
                val tnfName = when (tnf) {
                    NdefRecord.TNF_UNKNOWN -> "TNF_UNKNOWN"
                    NdefRecord.TNF_UNCHANGED -> "TNF_UNCHANGED"
                    else -> "TNF_$tnf"
                }
                ParsedRecord.Unknown(tnfName, type.joinToString(""){ "%02X".format(it.toInt() and 0xFF) }, rawHex, rawHex)
            }
        }
    }
}
