package com.example.edu_ai.data.local.cas

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cas_blobs")
data class CasBlobEntity(
    @PrimaryKey val hash: String,
    val compressedContent: ByteArray,
    val contentSize: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CasBlobEntity

        if (hash != other.hash) return false
        if (!compressedContent.contentEquals(other.compressedContent)) return false
        if (contentSize != other.contentSize) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + compressedContent.contentHashCode()
        result = 31 * result + contentSize.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
