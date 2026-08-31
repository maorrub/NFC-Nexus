package com.example.nfcnexus.hce

import java.io.ByteArrayOutputStream
import java.util.Arrays

data class ApduResponse(
    val responseBytes: ByteArray,
    val commandName: String,
    val statusCodeHex: String,
    val isSuccess: Boolean,
    val description: String
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

    companion object {
        // NFC Forum Type 4 Tag v2.0 NDEF Application AID: D2 76 00 00 85 01 01
        val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // Capability Container (CC) File ID: E1 03
        val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())

        // NDEF Data File ID: E1 04
        val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // Status Words
        val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E.toByte(), 0x00.toByte())

        // Standard Capability Container (CC) 15-byte file for Type 4 Tag v2.0
        val CC_FILE = byteArrayOf(
            0x00.toByte(), 0x0F.toByte(), // CCLEN: 15 bytes
            0x20.toByte(),               // Mapping Version 2.0
            0x00.toByte(), 0xFF.toByte(), // MLe: Max Read Length (255 bytes)
            0x00.toByte(), 0xFF.toByte(), // MLc: Max Write Length (255 bytes)
            0x04.toByte(), 0x06.toByte(), // NDEF File Control TLV: T=04, L=06
            0xE1.toByte(), 0x04.toByte(), // NDEF File ID (E1 04)
            0x08.toByte(), 0x00.toByte(), // Max NDEF File Size (2048 bytes)
            0x00.toByte(),               // Read Access (00 = Free access)
            0x00.toByte()                // Write Access (00 = Free access / writable)
        )
    }

    fun processCommandApdu(commandApdu: ByteArray, ndefMessageBytes: ByteArray): ApduResponse {
        if (commandApdu.size < 4) {
            return ApduResponse(SW_WRONG_LENGTH, "UNKNOWN", "67 00", false, "Command length too short")
        }

        val cla = commandApdu[0].toInt() and 0xFF
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

        return ApduResponse(SW_INS_NOT_SUPPORTED, "INS_0x%02X".format(ins), "6D 00", false, "Unsupported APDU instruction")
    }

    private fun handleSelectCommand(apdu: ByteArray, p1: Int, p2: Int): ApduResponse {
        // SELECT Application by Name (P1 = 0x04)
        if (p1 == 0x04) {
            val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
            if (lc >= NDEF_AID.size && apdu.size >= 5 + NDEF_AID.size) {
                val requestedAid = apdu.copyOfRange(5, 5 + NDEF_AID.size)
                if (Arrays.equals(requestedAid, NDEF_AID)) {
                    isAppSelected = true
                    selectedFile = SelectedFile.NONE
                    return ApduResponse(SW_SUCCESS, "SELECT NDEF AID", "90 00", true, "NDEF Application Selected (D2760000850101)")
                }
            }
            return ApduResponse(SW_FILE_NOT_FOUND, "SELECT AID", "6A 82", false, "AID Not Found")
        }

        // SELECT File by ID (P1 = 0x00)
        if (p1 == 0x00) {
            val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
            if (lc == 2 && apdu.size >= 7) {
                val fileId = apdu.copyOfRange(5, 7)
                if (Arrays.equals(fileId, CC_FILE_ID)) {
                    selectedFile = SelectedFile.CC_FILE
                    return ApduResponse(SW_SUCCESS, "SELECT CC FILE", "90 00", true, "Capability Container Selected (E103)")
                } else if (Arrays.equals(fileId, NDEF_FILE_ID)) {
                    selectedFile = SelectedFile.NDEF_FILE
                    return ApduResponse(SW_SUCCESS, "SELECT NDEF FILE", "90 00", true, "NDEF Data File Selected (E104)")
                }
            }
            return ApduResponse(SW_FILE_NOT_FOUND, "SELECT FILE", "6A 82", false, "File ID Not Found")
        }

        return ApduResponse(SW_FILE_NOT_FOUND, "SELECT", "6A 82", false, "Unknown Selection Mode")
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
            if (rawLe == 0) 256 else rawLe
        } else {
            128
        }

        when (selectedFile) {
            SelectedFile.CC_FILE -> {
                if (offset >= CC_FILE.size) {
                    return ApduResponse(SW_WRONG_LENGTH, "READ BINARY (CC)", "67 00", false, "Offset beyond CC file size")
                }
                val lengthToRead = minOf(le, CC_FILE.size - offset)
                val out = ByteArrayOutputStream()
                out.write(CC_FILE, offset, lengthToRead)
                out.write(SW_SUCCESS)
                val bytes = out.toByteArray()
                return ApduResponse(bytes, "READ BINARY (CC)", "90 00", true, "Read $lengthToRead bytes of CC File (Offset: $offset)")
            }

            SelectedFile.NDEF_FILE -> {
                // NDEF File structure: 2 bytes NLEN (big-endian) + ndefMessageBytes
                val nlen = ndefMessageBytes.size
                val ndefFile = ByteArray(2 + nlen)
                ndefFile[0] = ((nlen shr 8) and 0xFF).toByte()
                ndefFile[1] = (nlen and 0xFF).toByte()
                System.arraycopy(ndefMessageBytes, 0, ndefFile, 2, nlen)

                if (offset >= ndefFile.size) {
                    return ApduResponse(SW_WRONG_LENGTH, "READ BINARY (NDEF)", "67 00", false, "Offset beyond NDEF file size")
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

    fun resetState() {
        selectedFile = SelectedFile.NONE
        isAppSelected = false
    }
}
