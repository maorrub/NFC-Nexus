package com.example.nfcnexus.nfc.parser

import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import java.nio.charset.StandardCharsets

object VCardParser {

    fun parse(record: NdefRecord): ParsedRecord.VCard {
        val payload = record.payload ?: byteArrayOf()
        val rawHex = payload.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        val rawVCard = String(payload, StandardCharsets.UTF_8)

        var fn = ""
        var org = ""
        var title = ""
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val urls = mutableListOf<String>()
        var note = ""
        var address = ""

        val lines = rawVCard.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx == -1) continue

            val keyPart = trimmed.substring(0, colonIdx).uppercase()
            val value = trimmed.substring(colonIdx + 1).trim()

            when {
                keyPart == "FN" || keyPart.startsWith("FN;") -> {
                    fn = value
                }
                (keyPart == "N" || keyPart.startsWith("N;")) && fn.isEmpty() -> {
                    val parts = value.split(";")
                    val lastName = parts.getOrNull(0) ?: ""
                    val firstName = parts.getOrNull(1) ?: ""
                    fn = "$firstName $lastName".trim()
                }
                keyPart == "ORG" || keyPart.startsWith("ORG;") -> {
                    org = value.replace(";", " - ")
                }
                keyPart == "TITLE" || keyPart.startsWith("TITLE;") -> {
                    title = value
                }
                keyPart.startsWith("TEL") -> {
                    if (value.isNotEmpty()) phones.add(value)
                }
                keyPart.startsWith("EMAIL") -> {
                    if (value.isNotEmpty()) emails.add(value)
                }
                keyPart.startsWith("URL") -> {
                    if (value.isNotEmpty()) urls.add(value)
                }
                keyPart.startsWith("NOTE") -> {
                    note = value
                }
                keyPart.startsWith("ADR") -> {
                    address = value.split(";").filter { it.isNotBlank() }.joinToString(", ")
                }
            }
        }

        return ParsedRecord.VCard(
            formattedName = fn.ifEmpty { "Contact" },
            organization = org,
            title = title,
            phoneNumbers = phones,
            emails = emails,
            urls = urls,
            note = note,
            address = address,
            rawVCard = rawVCard,
            rawBytesHex = rawHex
        )
    }
}
