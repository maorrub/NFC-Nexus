package com.example.nfcnexus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "saved_tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val tagUid: String,
    val tagType: String,
    val category: String, // "SCANNED", "TEMPLATE", "CLONED"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val recordsJson: String, // Serialized List<ParsedRecord>
    val techListJson: String, // Serialized List<String>
    val rawNdefHex: String = "",
    val memorySize: Int = 0,
    val isReadOnly: Boolean = false
)
