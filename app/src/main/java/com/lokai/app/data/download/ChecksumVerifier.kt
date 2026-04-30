package com.lokai.app.data.download

import android.util.Log
import java.io.File
import java.security.MessageDigest

private const val TAG = "ChecksumVerifier"

/**
 * Verifies the SHA-256 checksum of a downloaded file.
 *
 * If [expected] is blank, verification is skipped and true is returned.
 * Progress callback is optional — called with 0.0–1.0 as the file is hashed.
 */
class ChecksumVerifier {

    /**
     * @param file     File to verify
     * @param expected Expected SHA-256 hex string (lowercase). Empty = skip.
     * @param onProgress Optional lambda called with progress 0.0–1.0
     * @return true if verified (or skipped), false if mismatch
     */
    fun verify(
        file: File,
        expected: String,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        if (expected.isBlank()) {
            Log.i(TAG, "No checksum provided for ${file.name} — skipping verification")
            return true
        }

        if (!file.exists()) {
            Log.e(TAG, "File not found for checksum: ${file.absolutePath}")
            return false
        }

        return try {
            val digest  = MessageDigest.getInstance("SHA-256")
            val buffer  = ByteArray(8192)
            val total   = file.length()
            var read    = 0L

            file.inputStream().use { stream ->
                var bytes: Int
                while (stream.read(buffer).also { bytes = it } != -1) {
                    digest.update(buffer, 0, bytes)
                    read += bytes
                    onProgress?.invoke(if (total > 0) read.toFloat() / total else 0f)
                }
            }

            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val match  = actual.equals(expected.trim(), ignoreCase = true)

            if (match) {
                Log.i(TAG, "Checksum verified OK for ${file.name}")
            } else {
                Log.e(TAG, "Checksum MISMATCH for ${file.name}")
                Log.e(TAG, "  Expected: $expected")
                Log.e(TAG, "  Actual:   $actual")
            }

            match
        } catch (e: Exception) {
            Log.e(TAG, "Checksum error for ${file.name}: ${e.message}")
            false
        }
    }
}
