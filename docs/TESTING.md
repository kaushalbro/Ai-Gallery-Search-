# LensVault Testing & Verification Guide

## Test Suite Overview

- **`HashingTest.kt`**: Validates deterministic SHA-256 / xxHash exact hashing, 64-bit DCT perceptual hash generation, and Hamming distance threshold checks.
- **`SearchScoringTest.kt`**: Validates entity regex extraction (URLs, Emails, Phone Numbers, IP Addresses, Wi-Fi passwords, Invoices) and relevance ranking calculations.

## Running Tests

Execute the JVM test suite locally:
```bash
gradle :app:testDebugUnitTest
```
