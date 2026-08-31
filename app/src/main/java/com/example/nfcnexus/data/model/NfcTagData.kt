package com.example.nfcnexus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NfcTagData(
    val uidHex: String,
    val uidDecimal: String,
    val tagStandard: String, // e.g. "NFC Forum Type 2 (NTAG213)", "MIFARE Classic 1K", etc.
    val techList: List<String>,
    val sak: String? = null,
    val atqa: String? = null,
    val historicalBytes: String? = null,
    val maxNdefSize: Int = 0,
    val currentNdefSize: Int = 0,
    val isWritable: Boolean = true,
    val canMakeReadOnly: Boolean = false,
    val isFormatted: Boolean = true,
    val records: List<ParsedRecord> = emptyList(),
    val rawPayloadHex: String? = null,
    val scannedAt: Long = System.currentTimeMillis()
)
