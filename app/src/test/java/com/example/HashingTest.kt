package com.example

import com.example.core.hashing.HashEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingTest {

    @Test
    fun testExactHashDeterministic() {
        val data1 = "LensVault Offline OCR Test Data".toByteArray()
        val data2 = "LensVault Offline OCR Test Data".toByteArray()
        val data3 = "Different Content".toByteArray()

        val hash1 = HashEngine.computeExactHash(data1)
        val hash2 = HashEngine.computeExactHash(data2)
        val hash3 = HashEngine.computeExactHash(data3)

        assertEquals(hash1, hash2)
        assertTrue(hash1 != hash3)
    }

    @Test
    fun testHammingDistance() {
        val h1 = "1010101010101010101010101010101010101010101010101010101010101010"
        val h2 = "1010101010101010101010101010101010101010101010101010101010101010"
        val h3 = "1010101010101010101010101010101010101010101010101010101010101111" // 2 bits diff

        assertEquals(0, HashEngine.hammingDistance(h1, h2))
        assertEquals(2, HashEngine.hammingDistance(h1, h3))
        assertTrue(HashEngine.areNearDuplicates(h1, h3, threshold = 4))
        assertFalse(HashEngine.areNearDuplicates(h1, h3, threshold = 1))
    }
}
