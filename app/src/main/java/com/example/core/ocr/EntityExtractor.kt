package com.example.core.ocr

import com.example.data.model.EntityFilter
import com.example.data.model.ExtractedEntity
import java.util.regex.Pattern

object EntityExtractor {
    private val URL_PATTERN = Pattern.compile(
        """(https?://[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+[a-zA-Z0-9/_])""",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}\b"""
    )

    private val PHONE_PATTERN = Pattern.compile(
        """(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b"""
    )

    private val IP_PATTERN = Pattern.compile(
        """(?<![a-zA-Z0-9_])\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b(?![a-zA-Z0-9_])"""
    )

    private val INVOICE_PATTERN = Pattern.compile(
        """(INV[-#\w]{4,}|Total(?:\s+Amount)?(?:\s+Due)?\s*[:=]?\s*[$€£¥₹]\s*[\d,]+\.?\d*|[$€£¥₹]\s*[\d,]+\.\d{2}|Amount\s+Due\s*[:=]?\s*[$€£¥₹]?\s*[\d,]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val CREDENTIALS_PATTERN = Pattern.compile(
        """(?:Password|Wi-Fi|SSID|API[ _-]KEY|Bearer)\s*[:=]\s*([^\s,;]+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun extractEntities(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()

        // URLs
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            results.add(ExtractedEntity(EntityFilter.URL, urlMatcher.group(1).orEmpty(), urlMatcher.group()))
        }

        // Emails
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        while (emailMatcher.find()) {
            results.add(ExtractedEntity(EntityFilter.EMAIL, emailMatcher.group(), emailMatcher.group()))
        }

        // Phone numbers
        val phoneMatcher = PHONE_PATTERN.matcher(text)
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group().trim()
            if (phone.length >= 7) {
                results.add(ExtractedEntity(EntityFilter.PHONE, phone, phone))
            }
        }

        // IP Addresses
        val ipMatcher = IP_PATTERN.matcher(text)
        while (ipMatcher.find()) {
            results.add(ExtractedEntity(EntityFilter.IP, ipMatcher.group(), ipMatcher.group()))
        }

        // Invoices / Totals
        val invMatcher = INVOICE_PATTERN.matcher(text)
        while (invMatcher.find()) {
            results.add(ExtractedEntity(EntityFilter.INVOICE, invMatcher.group(), invMatcher.group()))
        }

        // Credentials / Wi-Fi
        val credMatcher = CREDENTIALS_PATTERN.matcher(text)
        while (credMatcher.find()) {
            results.add(ExtractedEntity(EntityFilter.CREDENTIALS, credMatcher.group(), credMatcher.group()))
        }

        return results.distinctBy { it.value }
    }
}
