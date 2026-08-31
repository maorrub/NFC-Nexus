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

        val activeCard = EmulationRepository.currentCard.value
        if (!activeCard.isEnabled) {
            return ApduProtocolHandler.SW_FILE_NOT_FOUND
        }

        val commandHex = commandApdu.joinToString(" ") { "%02X".format(it) }
        val response = protocolHandler.processCommandApdu(commandApdu, activeCard.ndefBytes)
        val responseHex = response.responseBytes.joinToString(" ") { "%02X".format(it) }

        // Log transaction to live repository feed
        EmulationRepository.logApduTransaction(
            commandName = response.commandName,
            commandHex = commandHex,
            responseHex = responseHex,
            statusCodeHex = response.statusCodeHex,
            isSuccess = response.isSuccess,
            description = response.description
        )

        return response.responseBytes
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
