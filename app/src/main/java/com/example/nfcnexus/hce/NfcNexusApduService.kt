package com.example.nfcnexus.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.example.nfcnexus.data.repository.EmulationRepository

class NfcNexusApduService : HostApduService() {

    private val protocolHandler = ApduProtocolHandler()

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.isEmpty()) {
            return ApduProtocolHandler.SW_WRONG_LENGTH
        }

        val commandHex = commandApdu.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
        val activeCard = EmulationRepository.currentCard.value

        if (!activeCard.isEnabled) {
            EmulationRepository.logApduTransaction(
                commandName = "BLOCKED (PAUSED)",
                commandHex = commandHex,
                responseHex = "6A 82",
                statusCodeHex = "6A 82",
                isSuccess = false,
                description = "Emulation is paused. Tap 'START EMULATING' to respond to readers."
            )
            return ApduProtocolHandler.SW_FILE_NOT_FOUND
        }

        return try {
            val response = protocolHandler.processCommandApdu(commandApdu, activeCard.ndefBytes)
            val responseHex = response.responseBytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

            // If an external NFC writer updated the NDEF payload, commit to repository
            if (response.updatedNdefBytes != null) {
                EmulationRepository.setEmulatedPayload(
                    title = "Updated Virtual Card",
                    subtitle = "${response.updatedNdefBytes.size} bytes updated via NFC write",
                    payloadType = "NDEF Written",
                    ndefBytes = response.updatedNdefBytes
                )
            }

            // Log transaction to live repository feed
            EmulationRepository.logApduTransaction(
                commandName = response.commandName,
                commandHex = commandHex,
                responseHex = responseHex,
                statusCodeHex = response.statusCodeHex,
                isSuccess = response.isSuccess,
                description = response.description
            )

            response.responseBytes
        } catch (e: Exception) {
            EmulationRepository.logApduTransaction(
                commandName = "ERROR",
                commandHex = commandHex,
                responseHex = "6F 00",
                statusCodeHex = "6F 00",
                isSuccess = false,
                description = "APDU handling error: ${e.localizedMessage}"
            )
            ApduProtocolHandler.SW_FILE_NOT_FOUND
        }
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Link Loss (Reader disconnected)"
            DEACTIVATION_DESELECTED -> "Deselected by Reader"
            else -> "Reason $reason"
        }
        protocolHandler.resetState()
        EmulationRepository.logApduTransaction(
            commandName = "SESSION END",
            commandHex = "",
            responseHex = "",
            statusCodeHex = "--",
            isSuccess = true,
            description = "NFC field deactivated: $reasonStr"
        )
    }
}
