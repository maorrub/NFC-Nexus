package com.example.nfcnexus.hce

import java.io.ByteArrayOutputStream
import java.util.Arrays

data class ApduResponse(
    val responseBytes: ByteArray,
    val commandName: String,
    val statusCodeHex: String,
    val isSuccess: Boolean,
    val description: String,
    val updatedNdefBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ApduResponse
        return responseBytes.contentEquals(other.responseBytes)
    }

    override fun hashCode(): Int {
        return responseBytes.contentHashCode()
    }
}

class ApduProtocolHandler {

    enum class SelectedFile {
        NONE,
        CC_FILE,
        NDEF_FILE
    }

    private var selectedFile = SelectedFile.NONE
    private var isAppSelected = false

    // Buffer to support external writes (UPDATE BINARY)
    private var pendingNdefBuffer: ByteArray? = null

    companion object {
        // NFC Forum Type 4 Tag v2.0 NDEF Application AID: D2 76 00 00 85 01 01
        val NDEF_AID_V2 = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // NFC Forum Type 4 Tag v1.0 NDEF Application AID: D2 76 00 00 85 01 00
        val NDEF_AID_V1 = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x00.toByte()
        )

        // Backward compatibility alias
        val NDEF_AID = NDEF_AID_V2

        // Master File (MF) ID: 3F 00
        val MF_FILE_ID = byteArrayOf(0x3F.toByte(), 0x00.toByte())

        // Capability Container (CC) File ID: E1 03
        val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())

        // NDEF Data File ID: E1 04
        val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // Status Words (ISO/IEC 7816-4)
        val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_WRONG_OFFSET = byteArrayOf(0x6B.toByte(), 0x00.toByte())
        val SW_WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E.toByte(), 0x00.toByte())
        val SW_SECURITY_STATUS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())

        // Maximum transceive chunk size: 127 bytes ensures total response (data + 2 SW bytes)
        // never exceeds 256 bytes, preventing ISO-DEP buffer overflow on external readers.
        const val MAX_READ_CHUNK_SIZE = 127

        // Standard Capability Container (CC) 15-byte file for Type 4 Tag v2.0
        val CC_FILE = byteArrayOf(
            0x00.toByte(), 0x0F.toByte(), // CCLEN: 15 bytes
            0x20.toByte(),               // Mapping Version 2.0
            0x00.toByte(), 0x7F.toByte(), // MLe: Max Read Length (127 bytes, prevents frame overflow)
            0x00.toByte(), 0x7F.toByte(), // MLc: Max Write Length (127 bytes)
            0x04.toByte(), 0x06.toByte(), // NDEF File Control TLV: T=04, L=06
            0xE1.toByte(), 0x04.toByte(), // NDEF File ID (E1 04)
            0x20.toByte(), 0x00.toByte(), // Max NDEF File Size (8192 bytes / 8KB)
            0x00.toByte(),               // Read Access (00 = Free access)
            0x00.toByte()                // Write Access (00 = Free access / writable)
        )
    }

    fun processCommandApdu(commandApdu: ByteArray, ndefMessageBytes: ByteArray): ApduResponse {
        if (commandApdu.size < 4) {
            return ApduResponse(SW_WRONG_LENGTH, "UNKNOWN", "67 00", false, "Command length too short")
        }

        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = commandApdu[2].toInt() and 0xFF
        val p2 = commandApdu[3].toInt() and 0xFF

        // Check SELECT command (INS = 0xA4)
        if (ins == 0xA4) {
            return handleSelectCommand(commandApdu, p1, p2)
        }

        // Check READ BINARY command (INS = 0xB0)
        if (ins == 0xB0) {
            return handleReadBinaryCommand(commandApdu, p1, p2, ndefMessageBytes)
        }

        // Check UPDATE BINARY command (INS = 0xD6)
        if (ins == 0xD6) {
            return handleUpdateBinaryCommand(commandApdu, p1, p2, ndefMessageBytes)
        }

        return ApduResponse(SW_INS_NOT_SUPPORTED, "INS_0x%02X".format(ins), "6D 00", false, "Unsupported APDU instruction")
    }

    private fun handleSelectCommand(apdu: ByteArray, p1: Int, p2: Int): ApduResponse {
        // SELECT Application by Name (P1 = 0x04)
        if (p1 == 0x04) {
            if (apdu.size >= 5 + NDEF_AID_V2.size) {
                val requestedAid = apdu.copyOfRange(5, 5 + NDEF_AID_V2.size)
                if (Arrays.equals(requestedAid, NDEF_AID_V2)) {
                    isAppSelected = true
                    selectedFile = SelectedFile.NONE
                    return ApduResponse(SW_SUCCESS, "SELECT NDEF AID v2", "90 00", true, "NDEF v2 Application Selected (D2760000850101)")
                }
                if (Arrays.equals(requestedAid, NDEF_AID_V1)) {
                    isAppSelected = true
                    selectedFile = SelectedFile.NONE
                    return ApduResponse(SW_SUCCESS, "SELECT NDEF AID v1", "90 00", true, "NDEF v1 Application Selected (D2760000850100)")
                }
            }
            return ApduResponse(SW_FILE_NOT_FOUND, "SELECT AID", "6A 82", false, "AID Not Found")
        }

        // SELECT File by ID (P1 = 0x00: Select MF, DF, or EF; P1 = 0x02: Select EF under current DF)
        // Standard readers (including iOS CoreNFC, Proxmark, ACR122U) often use P1 = 0x02 or 0x00
        if (apdu.size >= 7) {
            val fileId = apdu.copyOfRange(5, 7)
            if (Arrays.equals(fileId, CC_FILE_ID)) {
                selectedFile = SelectedFile.CC_FILE
                return ApduResponse(SW_SUCCESS, "SELECT CC FILE", "90 00", true, "Capability Container Selected (E103)")
            } else if (Arrays.equals(fileId, NDEF_FILE_ID)) {
                selectedFile = SelectedFile.NDEF_FILE
                return ApduResponse(SW_SUCCESS, "SELECT NDEF FILE", "90 00", true, "NDEF Data File Selected (E104)")
            } else if (Arrays.equals(fileId, MF_FILE_ID)) {
                // Master file selected
                return ApduResponse(SW_SUCCESS, "SELECT MF", "90 00", true, "Master File Selected (3F00)")
            }
        }

        return ApduResponse(SW_FILE_NOT_FOUND, "SELECT FILE", "6A 82", false, "File ID Not Found")
    }

    private fun handleReadBinaryCommand(
        apdu: ByteArray,
        p1: Int,
        p2: Int,
        ndefMessageBytes: ByteArray
    ): ApduResponse {
        val offset = ((p1 and 0xFF) shl 8) or (p2 and 0xFF)
        val le = if (apdu.size >= 5) {
            val rawLe = apdu[4].toInt() and 0xFF
            if (rawLe == 0) MAX_READ_CHUNK_SIZE else minOf(rawLe, MAX_READ_CHUNK_SIZE)
        } else {
            MAX_READ_CHUNK_SIZE
        }

        // If reader didn't issue an explicit SELECT FILE after AID select, default to CC_FILE
        val effectiveFile = if (selectedFile == SelectedFile.NONE) SelectedFile.CC_FILE else selectedFile

        when (effectiveFile) {
            SelectedFile.CC_FILE -> {
                if (offset >= CC_FILE.size) {
                    return ApduResponse(SW_WRONG_OFFSET, "READ BINARY (CC)", "6B 00", false, "Offset beyond CC file size ($offset >= ${CC_FILE.size})")
                }
                val lengthToRead = minOf(le, CC_FILE.size - offset)
                val out = ByteArrayOutputStream()
                out.write(CC_FILE, offset, lengthToRead)
                out.write(SW_SUCCESS)
                val bytes = out.toByteArray()
                return ApduResponse(bytes, "READ BINARY (CC)", "90 00", true, "Read $lengthToRead bytes of CC File (Offset: $offset)")
            }

            SelectedFile.NDEF_FILE -> {
                // NDEF File structure: 2 bytes NLEN (big-endian) + active NDEF payload bytes
                val activeBytes = pendingNdefBuffer ?: ndefMessageBytes
                val nlen = activeBytes.size
                val ndefFile = ByteArray(2 + nlen)
                ndefFile[0] = ((nlen shr 8) and 0xFF).toByte()
                ndefFile[1] = (nlen and 0xFF).toByte()
                System.arraycopy(activeBytes, 0, ndefFile, 2, nlen)

                if (offset >= ndefFile.size) {
                    return ApduResponse(SW_WRONG_OFFSET, "READ BINARY (NDEF)", "6B 00", false, "Offset beyond NDEF file size ($offset >= ${ndefFile.size})")
                }

                val lengthToRead = minOf(le, ndefFile.size - offset)
                val out = ByteArrayOutputStream()
                out.write(ndefFile, offset, lengthToRead)
                out.write(SW_SUCCESS)
                val bytes = out.toByteArray()
                return ApduResponse(bytes, "READ BINARY (NDEF)", "90 00", true, "Read $lengthToRead bytes of NDEF File (Offset: $offset, Total: ${ndefFile.size}B)")
            }

            SelectedFile.NONE -> {
                return ApduResponse(SW_FILE_NOT_FOUND, "READ BINARY", "6A 82", false, "No file currently selected")
            }
        }
    }

    private fun handleUpdateBinaryCommand(
        apdu: ByteArray,
        p1: Int,
        p2: Int,
        currentNdefBytes: ByteArray
    ): ApduResponse {
        val offset = ((p1 and 0xFF) shl 8) or (p2 and 0xFF)
        val lc = if (apdu.size >= 5) apdu[4].toInt() and 0xFF else 0

        if (apdu.size < 5 + lc) {
            return ApduResponse(SW_WRONG_LENGTH, "UPDATE BINARY", "67 00", false, "APDU length does not match Lc")
        }

        val data = if (lc > 0) apdu.copyOfRange(5, 5 + lc) else byteArrayOf()

        if (selectedFile == SelectedFile.NDEF_FILE) {
            // Case 1: Writing NLEN = 0 (reset/init write sequence)
            if (offset == 0 && lc == 2 && data[0] == 0.toByte() && data[1] == 0.toByte()) {
                pendingNdefBuffer = ByteArray(0)
                return ApduResponse(SW_SUCCESS, "UPDATE BINARY (RESET)", "90 00", true, "NDEF message reset (NLEN=0)")
            }

            // Case 2: Writing NDEF payload starting at offset 2
            if (offset >= 2 && lc > 0) {
                val current = pendingNdefBuffer ?: ByteArray(0)
                val newBufferOffset = offset - 2
                val requiredSize = maxOf(current.size, newBufferOffset + lc)
                val newBuffer = ByteArray(requiredSize)
                System.arraycopy(current, 0, newBuffer, 0, current.size)
                System.arraycopy(data, 0, newBuffer, newBufferOffset, lc)
                pendingNdefBuffer = newBuffer
                return ApduResponse(SW_SUCCESS, "UPDATE BINARY (DATA)", "90 00", true, "Wrote $lc bytes at offset $offset")
            }

            // Case 3: Finalizing write with final NLEN
            if (offset == 0 && lc == 2) {
                val finalLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                val committed = pendingNdefBuffer?.take(finalLength)?.toByteArray() ?: ByteArray(0)
                pendingNdefBuffer = committed
                return ApduResponse(
                    responseBytes = SW_SUCCESS,
                    commandName = "UPDATE BINARY (FINALIZE)",
                    statusCodeHex = "90 00",
                    isSuccess = true,
                    description = "Committed $finalLength bytes NDEF message",
                    updatedNdefBytes = committed
                )
            }

            return ApduResponse(SW_SUCCESS, "UPDATE BINARY", "90 00", true, "Processed update at offset $offset")
        }

        return ApduResponse(SW_SECURITY_STATUS_NOT_SATISFIED, "UPDATE BINARY", "69 82", false, "Write prohibited on selected file")
    }

    fun resetState() {
        selectedFile = SelectedFile.NONE
        isAppSelected = false
        pendingNdefBuffer = null
    }
}
