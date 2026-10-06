package com.example.core.hashing

import android.graphics.Bitmap
import android.graphics.Color
import java.io.InputStream
import java.security.MessageDigest

object HashEngine {

    /**
     * Fast 64-bit exact hash computed over stream or bytes (xxHash3 / FNV-1a hybrid).
     */
    fun computeExactHash(inputStream: InputStream): String {
        val buffer = ByteArray(8192)
        val digest = MessageDigest.getInstance("SHA-256")
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun computeExactHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute 64-bit Perceptual Hash (pHash / dHash) of a Bitmap for near-duplicate detection.
     * Downscales to 9x8 grayscale, computes horizontal gradient difference, and yields a 64-bit binary string.
     */
    fun computePerceptualHash(bitmap: Bitmap): String {
        val width = 9
        val height = 8
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val sb = StringBuilder(64)
        for (y in 0 until height) {
            for (x in 0 until 8) {
                val leftPixel = pixels[y * width + x]
                val rightPixel = pixels[y * width + x + 1]

                val leftLum = (Color.red(leftPixel) * 299 + Color.green(leftPixel) * 587 + Color.blue(leftPixel) * 114) / 1000
                val rightLum = (Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 114) / 1000

                sb.append(if (leftLum > rightLum) '1' else '0')
            }
        }
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return sb.toString()
    }

    /**
     * Calculates the Hamming distance between two 64-bit binary perceptual hashes.
     * Distance <= 4 indicates very high visual similarity (near-identical screenshot or burst photo).
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length || hash1.isEmpty()) return 64
        var distance = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) {
                distance++
            }
        }
        return distance
    }

    fun areNearDuplicates(hash1: String?, hash2: String?, threshold: Int = 4): Boolean {
        if (hash1 == null || hash2 == null) return false
        return hammingDistance(hash1, hash2) <= threshold
    }
}
