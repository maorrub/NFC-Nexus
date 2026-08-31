package com.example.nfcnexus.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException

sealed class NfcOperationResult {
    data class Success(val message: String, val bytesWritten: Int = 0) : NfcOperationResult()
    data class Error(val message: String, val exception: Throwable? = null) : NfcOperationResult()
}

object NfcWriter {

    fun writeNdefMessage(tag: Tag, ndefMessage: NdefMessage): NfcOperationResult {
        val messageBytes = ndefMessage.toByteArray()
        val size = messageBytes.size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) {
                    return NfcOperationResult.Error("Tag is locked and permanently read-only.")
                }
                if (ndef.maxSize < size) {
                    return NfcOperationResult.Error("Payload exceeds tag capacity (Tag: ${ndef.maxSize}B, Data: ${size}B).")
                }
                ndef.writeNdefMessage(ndefMessage)
                NfcOperationResult.Success("Successfully wrote $size bytes to NFC tag.", size)
            } catch (e: TagLostException) {
                NfcOperationResult.Error("Tag was removed before writing completed. Hold tag steady.", e)
            } catch (e: FormatException) {
                NfcOperationResult.Error("Tag formatting error: ${e.localizedMessage}", e)
            } catch (e: IOException) {
                NfcOperationResult.Error("Communication error while writing tag: ${e.localizedMessage}", e)
            } catch (e: Exception) {
                NfcOperationResult.Error("Unexpected write failure: ${e.localizedMessage}", e)
            } finally {
                try {
                    ndef.close()
                } catch (ignored: Exception) {}
            }
        }

        // Try NdefFormatable
        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null) {
            return try {
                ndefFormatable.connect()
                ndefFormatable.format(ndefMessage)
                NfcOperationResult.Success("Formatted unformatted tag and wrote $size bytes.", size)
            } catch (e: TagLostException) {
                NfcOperationResult.Error("Tag was moved during formatting.", e)
            } catch (e: Exception) {
                NfcOperationResult.Error("Failed to format raw tag: ${e.localizedMessage}", e)
            } finally {
                try {
                    ndefFormatable.close()
                } catch (ignored: Exception) {}
            }
        }

        return NfcOperationResult.Error("Tag does not support NDEF or NDEF Formatable technology.")
    }

    fun eraseTag(tag: Tag): NfcOperationResult {
        val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))))
        return writeNdefMessage(tag, emptyMessage)
    }

    fun makeReadOnly(tag: Tag): NfcOperationResult {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.canMakeReadOnly()) {
                    return NfcOperationResult.Error("This tag hardware does not support permanent locking.")
                }
                val success = ndef.makeReadOnly()
                if (success) {
                    NfcOperationResult.Success("Tag has been permanently locked as Read-Only.")
                } else {
                    NfcOperationResult.Error("Lock operation was rejected by the tag.")
                }
            } catch (e: TagLostException) {
                NfcOperationResult.Error("Tag was moved during lock operation.", e)
            } catch (e: Exception) {
                NfcOperationResult.Error("Failed to lock tag: ${e.localizedMessage}", e)
            } finally {
                try {
                    ndef.close()
                } catch (ignored: Exception) {}
            }
        }

        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null) {
            return try {
                ndefFormatable.connect()
                val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))))
                ndefFormatable.formatReadOnly(emptyMessage)
                NfcOperationResult.Success("Formatted and locked tag as Read-Only.")
            } catch (e: Exception) {
                NfcOperationResult.Error("Failed to format tag as read-only: ${e.localizedMessage}", e)
            } finally {
                try {
                    ndefFormatable.close()
                } catch (ignored: Exception) {}
            }
        }

        return NfcOperationResult.Error("Tag does not support NDEF locking operations.")
    }
}
