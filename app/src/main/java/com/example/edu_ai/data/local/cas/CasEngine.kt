package com.example.edu_ai.data.local.cas

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ⚙️ CAS (Content-Addressable Storage) & Delta Engine
 * Handles SHA-256 hashing, text compression, and delta diff patching.
 */
object CasEngine {

    private val gson = Gson()

    /**
     * Calculates the SHA-256 hash string for the given raw text.
     */
    fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compresses raw string into a GZIP byte array.
     */
    fun compress(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(text.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    /**
     * Decompresses a GZIP byte array back to a raw string.
     */
    fun decompress(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val bis = ByteArrayInputStream(bytes)
        GZIPInputStream(bis).use { gzip ->
            return gzip.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    /**
     * Line-based Delta Diff specification representation
     */
    data class DeltaOperation(
        val type: String, // "ADD", "DELETE", "KEEP"
        val line: String
    )

    /**
     * Generates a line-by-line diff patch between oldText and newText serialized as JSON.
     */
    fun createDiffPatch(oldText: String, newText: String): String {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val ops = mutableListOf<DeltaOperation>()

        val oldSet = oldLines.toSet()
        val newSet = newLines.toSet()

        // Simple line delta encoder
        oldLines.forEach { line ->
            if (!newSet.contains(line)) {
                ops.add(DeltaOperation("DELETE", line))
            } else {
                ops.add(DeltaOperation("KEEP", line))
            }
        }
        newLines.forEach { line ->
            if (!oldSet.contains(line)) {
                ops.add(DeltaOperation("ADD", line))
            }
        }

        return gson.toJson(ops)
    }

    /**
     * Applies a diff patch JSON to a base text to construct the new text.
     */
    fun applyDiffPatch(baseText: String, patchJson: String): String {
        if (patchJson.isBlank()) return baseText
        val type = object : TypeToken<List<DeltaOperation>>() {}.type
        val ops: List<DeltaOperation> = try {
            gson.fromJson(patchJson, type) ?: emptyList()
        } catch (e: Exception) {
            return baseText
        }

        val baseLines = if (baseText.isEmpty()) mutableListOf() else baseText.lines().toMutableList()

        ops.forEach { op ->
            when (op.type) {
                "DELETE" -> baseLines.remove(op.line)
                "ADD" -> if (!baseLines.contains(op.line)) baseLines.add(op.line)
            }
        }

        return baseLines.joinToString("\n")
    }
}
