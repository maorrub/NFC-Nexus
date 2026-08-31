package com.example.nfcnexus.data.model

data class HexMemoryBlock(
    val index: Int,
    val type: String, // "Page", "Block", "Sector"
    val bytes: ByteArray,
    val hexString: String,
    val asciiString: String,
    val isReadOnly: Boolean = false,
    val notes: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HexMemoryBlock
        if (index != other.index) return false
        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
