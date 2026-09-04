package com.example.nfcnexus

import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.nfc.builder.NdefPayloadBuilder
import com.example.nfcnexus.nfc.builder.WifiTlvEncoder
import com.example.nfcnexus.nfc.parser.NdefMessageParser
import com.example.nfcnexus.nfc.parser.TextRecordParser
import com.example.nfcnexus.nfc.parser.UriRecordParser
import com.example.nfcnexus.nfc.parser.VCardParser
import com.example.nfcnexus.nfc.parser.WifiRecordParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NdefParserAndBuilderTest {

    @Test
    fun testTextRecordEncodingAndParsing() {
        val testText = "Hello NFC Nexus!"
        val testLang = "en"
        val record = NdefPayloadBuilder.createTextRecord(testText, testLang)

        val parsed = TextRecordParser.parse(record)
        assertEquals(testText, parsed.text)
        assertEquals(testLang, parsed.languageCode)
        assertEquals("UTF-8", parsed.encoding)
    }

    @Test
    fun testUriRecordCompressionAndParsing() {
        val url = "https://github.com/developer/portfolio"
        val record = NdefPayloadBuilder.createUriRecord(url)

        val parsed = UriRecordParser.parse(record)
        assertEquals(url, parsed.uri)
        assertTrue(parsed.scheme.startsWith("https"))
    }

    @Test
    fun testWifiTlvEncodingAndParsing() {
        val ssid = "Nexus-Mesh"
        val password = "SuperSecretPassword123"
        val record = NdefPayloadBuilder.createWifiRecord(
            ssid = ssid,
            password = password,
            authType = WifiTlvEncoder.AuthType.WPA2_PERSONAL
        )

        val parsed = WifiRecordParser.parse(record)
        assertEquals(ssid, parsed.ssid)
        assertEquals(password, parsed.networkKey)
        assertTrue(parsed.authType.contains("WPA2") || parsed.authType.contains("0x0020"))
    }

    @Test
    fun testVCardRecordCreationAndParsing() {
        val name = "Jane Doe"
        val org = "Nexus Corp"
        val phone = "+1-555-0100"
        val email = "jane@nexus.io"

        val record = NdefPayloadBuilder.createVCardRecord(
            fullName = name,
            organization = org,
            phone = phone,
            email = email
        )

        val parsed = VCardParser.parse(record)
        assertEquals(name, parsed.formattedName)
        assertEquals(org, parsed.organization)
        assertTrue(parsed.phoneNumbers.contains(phone))
        assertTrue(parsed.emails.contains(email))
    }
}
