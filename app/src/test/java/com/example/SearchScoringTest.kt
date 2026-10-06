package com.example

import com.example.core.ocr.EntityExtractor
import com.example.data.model.EntityFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScoringTest {

    @Test
    fun testEntityExtractor() {
        val testText = """
            Starbucks Store #4821
            Total: $14.85
            Wi-Fi SSID: Office_Fast
            Password: SecretPassword123!
            Router IP: 192.168.1.1
            Firmware Version: v2.14.0.1
            Support: help@lensvault.app
            Visit https://lensvault.app/docs
        """.trimIndent()

        val entities = EntityExtractor.extractEntities(testText)

        val urls = entities.filter { it.type == EntityFilter.URL }
        val emails = entities.filter { it.type == EntityFilter.EMAIL }
        val ips = entities.filter { it.type == EntityFilter.IP }
        val credentials = entities.filter { it.type == EntityFilter.CREDENTIALS }

        assertTrue(urls.any { it.value.contains("lensvault.app") })
        assertTrue(emails.any { it.value == "help@lensvault.app" })
        assertTrue(ips.any { it.value == "192.168.1.1" })
        // Verify v2.14.0.1 was not captured as an IP
        assertTrue(ips.none { it.value.contains("2.14.0.1") })
        assertTrue(credentials.any { it.value.contains("SecretPassword123!") })
    }

    @Test
    fun testUnicodeNormalization() {
        val raw = "Café München Total: €42.50"
        val normalized = com.example.core.ocr.OcrEngine.normalizeText(raw)
        assertTrue(normalized.contains("café"))
        assertTrue(normalized.contains("münchen"))
        assertTrue(normalized.contains("€42.50"))
    }
}
