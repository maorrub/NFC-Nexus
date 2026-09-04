package com.example.nfcnexus

import com.example.nfcnexus.hce.ApduProtocolHandler
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class ApduProtocolHandlerTest {

    private lateinit var handler: ApduProtocolHandler
    private val testNdefPayload = "Hello NFC Nexus!".toByteArray(StandardCharsets.UTF_8)

    @Before
    fun setup() {
        handler = ApduProtocolHandler()
    }

    @Test
    fun testSelectNdefApplicationV2Success() {
        // SELECT AID D2760000850101 (v2)
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x01.toByte(), 0x00.toByte()
        )

        val response = handler.processCommandApdu(selectAidApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)
        assertArrayEquals(ApduProtocolHandler.SW_SUCCESS, response.responseBytes)
    }

    @Test
    fun testSelectNdefApplicationV1Success() {
        // SELECT AID D2760000850100 (v1)
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x00.toByte()
        )

        val response = handler.processCommandApdu(selectAidApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)
        assertArrayEquals(ApduProtocolHandler.SW_SUCCESS, response.responseBytes)
    }

    @Test
    fun testSelectCapabilityContainerFileP1ZeroAndP1Two() {
        // First select AID
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x01.toByte()
        )
        handler.processCommandApdu(selectAidApdu, testNdefPayload)

        // SELECT CC File with P1 = 0x00
        val selectCcApduP1Zero = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x03.toByte()
        )
        val response1 = handler.processCommandApdu(selectCcApduP1Zero, testNdefPayload)
        assertTrue(response1.isSuccess)
        assertEquals("90 00", response1.statusCodeHex)

        // SELECT CC File with P1 = 0x02 (standard ISO 7816-4 EF under DF, used by iOS CoreNFC)
        val selectCcApduP1Two = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x02.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x03.toByte()
        )
        val response2 = handler.processCommandApdu(selectCcApduP1Two, testNdefPayload)
        assertTrue(response2.isSuccess)
        assertEquals("90 00", response2.statusCodeHex)

        // Read Binary CC File (Offset 0, Length 15)
        val readCcApdu = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte()
        )
        val readResponse = handler.processCommandApdu(readCcApdu, testNdefPayload)
        assertTrue(readResponse.isSuccess)
        assertEquals("90 00", readResponse.statusCodeHex)
        assertEquals(17, readResponse.responseBytes.size) // 15 bytes CC + 2 bytes SW 9000
    }

    @Test
    fun testSelectAndReadNdefDataFileWithP1Two() {
        // First select AID
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x01.toByte()
        )
        handler.processCommandApdu(selectAidApdu, testNdefPayload)

        // SELECT NDEF Data File (E1 04) with P1 = 0x02
        val selectNdefApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x02.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x04.toByte()
        )
        val response = handler.processCommandApdu(selectNdefApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)

        // Step 1: Read NLEN (Offset 0, Le = 2)
        val readNlenApdu = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte()
        )
        val nlenResponse = handler.processCommandApdu(readNlenApdu, testNdefPayload)
        assertTrue(nlenResponse.isSuccess)
        val nlenBytes = nlenResponse.responseBytes
        val nlen = ((nlenBytes[0].toInt() and 0xFF) shl 8) or (nlenBytes[1].toInt() and 0xFF)
        assertEquals(testNdefPayload.size, nlen)

        // Step 2: Read payload (Offset 2, Le = nlen)
        val readPayloadApdu = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x02.toByte(), testNdefPayload.size.toByte()
        )
        val payloadResponse = handler.processCommandApdu(readPayloadApdu, testNdefPayload)
        assertTrue(payloadResponse.isSuccess)
        val returnedPayload = payloadResponse.responseBytes.copyOf(testNdefPayload.size)
        assertArrayEquals(testNdefPayload, returnedPayload)
    }

    @Test
    fun testSelectMasterFileSuccess() {
        val selectMfApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(),
            0x3F.toByte(), 0x00.toByte()
        )
        val response = handler.processCommandApdu(selectMfApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)
    }

    @Test
    fun testReadBinaryOffsetBeyondFileSizeReturns6B00() {
        // Select CC
        val selectCcApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x03.toByte()
        )
        handler.processCommandApdu(selectCcApdu, testNdefPayload)

        // Read offset 20 (CC is 15 bytes)
        val readBeyondApdu = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x20.toByte(), 0x02.toByte()
        )
        val response = handler.processCommandApdu(readBeyondApdu, testNdefPayload)
        assertEquals("6B 00", response.statusCodeHex)
        assertArrayEquals(ApduProtocolHandler.SW_WRONG_OFFSET, response.responseBytes)
    }

    @Test
    fun testUpdateBinaryWriteWorkflow() {
        // Select NDEF
        val selectNdefApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x04.toByte()
        )
        handler.processCommandApdu(selectNdefApdu, testNdefPayload)

        // 1. Reset NLEN to 0
        val resetNlenApdu = byteArrayOf(
            0x00.toByte(), 0xD6.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x00.toByte()
        )
        val r1 = handler.processCommandApdu(resetNlenApdu, testNdefPayload)
        assertTrue(r1.isSuccess)

        // 2. Write payload at offset 2
        val newText = "Updated NFC Nexus".toByteArray(StandardCharsets.UTF_8)
        val writeDataApdu = byteArrayOf(
            0x00.toByte(), 0xD6.toByte(), 0x00.toByte(), 0x02.toByte(), newText.size.toByte()
        ) + newText
        val r2 = handler.processCommandApdu(writeDataApdu, testNdefPayload)
        assertTrue(r2.isSuccess)

        // 3. Finalize NLEN
        val finalizeApdu = byteArrayOf(
            0x00.toByte(), 0xD6.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(),
            ((newText.size shr 8) and 0xFF).toByte(),
            (newText.size and 0xFF).toByte()
        )
        val r3 = handler.processCommandApdu(finalizeApdu, testNdefPayload)
        assertTrue(r3.isSuccess)
        assertArrayEquals(newText, r3.updatedNdefBytes)
    }

    @Test
    fun testInvalidAidReturnsFileNotFound() {
        val invalidAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(),
            0xFF.toByte(), 0x00.toByte()
        )
        val response = handler.processCommandApdu(invalidAidApdu, testNdefPayload)
        assertEquals("6A 82", response.statusCodeHex)
        assertArrayEquals(ApduProtocolHandler.SW_FILE_NOT_FOUND, response.responseBytes)
    }
}
