package com.example.nfcnexus.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.nfcnexus.data.model.HexMemoryBlock
import com.example.nfcnexus.data.model.NfcTagData
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.nfc.parser.NdefMessageParser
import java.math.BigInteger

object NfcReader {

    fun readTagData(tag: Tag): NfcTagData {
        val uid = tag.id ?: byteArrayOf()
        val uidHex = uid.joinToString(":") { "%02X".format(it) }
        val uidDecimal = if (uid.isNotEmpty()) {
            BigInteger(1, uid).toString()
        } else {
            "0"
        }

        val techList = tag.techList.toList()
        var tagStandard = determineTagStandard(tag)
        var sak: String? = null
        var atqa: String? = null
        var historicalBytes: String? = null
        var maxNdefSize = 0
        var currentNdefSize = 0
        var isWritable = true
        var canMakeReadOnly = false
        var isFormatted = false
        var records: List<ParsedRecord> = emptyList()
        var rawPayloadHex: String? = null

        // 1. Read NfcA parameters if present
        NfcA.get(tag)?.let { nfcA ->
            sak = "0x%02X".format(nfcA.sak)
            atqa = nfcA.atqa?.joinToString(" ") { "%02X".format(it) }
        }

        // 2. Read IsoDep historical bytes if present
        IsoDep.get(tag)?.let { isoDep ->
            historicalBytes = isoDep.historicalBytes?.joinToString(" ") { "%02X".format(it) }
                ?: isoDep.hiLayerResponse?.joinToString(" ") { "%02X".format(it) }
        }

        // 3. Read NDEF if present
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            isFormatted = true
            try {
                ndef.connect()
                val ndefMessage = ndef.cachedNdefMessage ?: ndef.ndefMessage
                maxNdefSize = ndef.maxSize
                isWritable = ndef.isWritable
                canMakeReadOnly = ndef.canMakeReadOnly()

                if (ndefMessage != null) {
                    val rawBytes = ndefMessage.toByteArray()
                    currentNdefSize = rawBytes.size
                    rawPayloadHex = rawBytes.joinToString("") { "%02X".format(it) }
                    records = NdefMessageParser.parse(ndefMessage)
                }
                val ndefType = ndef.type
                if (ndefType.isNotEmpty() && tagStandard == "Standard NFC Tag") {
                    tagStandard = formatNdefType(ndefType)
                }
            } catch (e: Exception) {
                // Tag may have moved or I/O error
            } finally {
                try {
                    ndef.close()
                } catch (ignored: Exception) {}
            }
        } else {
            // Check if NdefFormatable
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                isFormatted = false
                isWritable = true
                tagStandard = "$tagStandard (Unformatted)"
            }
        }

        return NfcTagData(
            uidHex = uidHex.ifEmpty { "UNKNOWN" },
            uidDecimal = uidDecimal,
            tagStandard = tagStandard,
            techList = techList,
            sak = sak,
            atqa = atqa,
            historicalBytes = historicalBytes,
            maxNdefSize = maxNdefSize,
            currentNdefSize = currentNdefSize,
            isWritable = isWritable,
            canMakeReadOnly = canMakeReadOnly,
            isFormatted = isFormatted,
            records = records,
            rawPayloadHex = rawPayloadHex,
            scannedAt = System.currentTimeMillis()
        )
    }

    fun dumpMemoryBlocks(tag: Tag, maxPagesToRead: Int = 45): List<HexMemoryBlock> {
        val blocks = mutableListOf<HexMemoryBlock>()

        // 1. Try MifareUltralight
        val ultralight = MifareUltralight.get(tag)
        if (ultralight != null) {
            try {
                ultralight.connect()
                val totalPages = when (ultralight.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> 16
                    MifareUltralight.TYPE_ULTRALIGHT_C -> 48
                    else -> maxPagesToRead.coerceAtMost(135)
                }

                var page = 0
                while (page < totalPages) {
                    try {
                        val data = ultralight.readPages(page) // Reads 4 pages = 16 bytes
                        for (i in 0 until 4) {
                            val currentPage = page + i
                            if (currentPage >= totalPages) break
                            val pageBytes = data.copyOfRange(i * 4, (i + 1) * 4)
                            val hex = pageBytes.joinToString(" ") { "%02X".format(it) }
                            val ascii = pageBytes.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
                            
                            val notes = when (currentPage) {
                                0, 1 -> "UID / Manufacturer"
                                2 -> "Internal / Lock Bytes"
                                3 -> "Capability Container (CC)"
                                else -> if (currentPage < totalPages - 4) "User Memory" else "Lock / Config"
                            }

                            blocks.add(
                                HexMemoryBlock(
                                    index = currentPage,
                                    type = "Page",
                                    bytes = pageBytes,
                                    hexString = hex,
                                    asciiString = ascii,
                                    isReadOnly = currentPage < 3,
                                    notes = notes
                                )
                            )
                        }
                        page += 4
                    } catch (e: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                // I/O error
            } finally {
                try {
                    ultralight.close()
                } catch (ignored: Exception) {}
            }
        }

        return blocks
    }

    private fun determineTagStandard(tag: Tag): String {
        val techList = tag.techList.toSet()
        return when {
            MifareUltralight.get(tag) != null -> {
                when (MifareUltralight.get(tag)?.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "NFC Forum Type 2 (MIFARE Ultralight)"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "NFC Forum Type 2 (MIFARE Ultralight C)"
                    else -> "NFC Forum Type 2 (NTAG21x Series)"
                }
            }
            MifareClassic.get(tag) != null -> {
                when (MifareClassic.get(tag)?.type) {
                    MifareClassic.TYPE_CLASSIC -> "MIFARE Classic 1K / 4K"
                    MifareClassic.TYPE_PLUS -> "MIFARE Plus"
                    MifareClassic.TYPE_PRO -> "MIFARE Pro"
                    else -> "MIFARE Classic Compatible"
                }
            }
            IsoDep.get(tag) != null -> "NFC Forum Type 4 (ISO-DEP / DESFire)"
            NfcV.get(tag) != null -> "NFC Forum Type 5 (ISO 15693 / Vicinity)"
            NfcF.get(tag) != null -> "NFC Forum Type 3 (Sony FeliCa / JIS X 6319-4)"
            NfcB.get(tag) != null -> "ISO 14443-3B"
            NfcA.get(tag) != null -> "ISO 14443-3A"
            else -> "Standard NFC Tag"
        }
    }

    private fun formatNdefType(type: String): String {
        return when (type) {
            "org.nfcforum.ndef.type1" -> "NFC Forum Type 1 (Innovision Topaz)"
            "org.nfcforum.ndef.type2" -> "NFC Forum Type 2 (NXP NTAG / Ultralight)"
            "org.nfcforum.ndef.type3" -> "NFC Forum Type 3 (Sony FeliCa)"
            "org.nfcforum.ndef.type4" -> "NFC Forum Type 4 (ISO-DEP)"
            "com.nxp.ndef.mifareclassic" -> "MIFARE Classic NDEF Tag"
            else -> type
        }
    }
}
