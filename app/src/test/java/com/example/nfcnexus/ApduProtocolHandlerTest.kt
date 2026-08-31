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
    fun testSelectNdefApplicationSuccess() {
        // SELECT AID D2760000850101
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
    fun testSelectCapabilityContainerFile() {
        // First select AID
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x01.toByte()
        )
        handler.processCommandApdu(selectAidApdu, testNdefPayload)

        // SELECT CC File (E1 03)
        val selectCcApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x03.toByte()
        )
        val response = handler.processCommandApdu(selectCcApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)

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
    fun testSelectAndReadNdefDataFile() {
        // First select AID
        val selectAidApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
            0x01.toByte(), 0x01.toByte()
        )
        handler.processCommandApdu(selectAidApdu, testNdefPayload)

        // SELECT NDEF Data File (E1 04)
        val selectNdefApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x04.toByte()
        )
        val response = handler.processCommandApdu(selectNdefApdu, testNdefPayload)
        assertTrue(response.isSuccess)
        assertEquals("90 00", response.statusCodeHex)

        // Read Binary NDEF (Offset 0, Le = 2 + payload length)
        val readNdefApdu = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), (2 + testNdefPayload.size).toByte()
        )
        val readResponse = handler.processCommandApdu(readNdefApdu, testNdefPayload)
        assertTrue(readResponse.isSuccess)
        assertEquals("90 00", readResponse.statusCodeHex)

        // Check NLEN header
        val respBytes = readResponse.responseBytes
        val nlen = ((respBytes[0].toInt() and 0xFF) shl 8) or (respBytes[1].toInt() and 0xFF)
        assertEquals(testNdefPayload.size, nlen)
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
