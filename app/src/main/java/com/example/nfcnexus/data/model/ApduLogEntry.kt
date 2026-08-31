package com.example.nfcnexus.data.model

data class ApduLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val commandName: String,
    val commandApduHex: String,
    val responseApduHex: String,
    val statusCodeHex: String, // e.g. "90 00", "6A 82"
    val isSuccess: Boolean,
    val description: String = ""
)
