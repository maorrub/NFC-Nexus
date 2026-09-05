package com.example.nfcnexus.data.repository

import com.example.nfcnexus.data.local.TagDao
import com.example.nfcnexus.data.local.TagEntity
import com.example.nfcnexus.data.model.NfcTagData
import com.example.nfcnexus.data.model.ParsedRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagRepository(private val tagDao: TagDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()
    val favoriteTags: Flow<List<TagEntity>> = tagDao.getFavoriteTags()

    fun getTagsByCategory(category: String): Flow<List<TagEntity>> =
        tagDao.getTagsByCategory(category)

    fun searchTags(query: String): Flow<List<TagEntity>> =
        tagDao.searchTags(query)

    suspend fun getTagById(id: Long): TagEntity? = withContext(Dispatchers.IO) {
        tagDao.getTagById(id)
    }

    suspend fun saveScannedTag(tagData: NfcTagData, customTitle: String? = null): Long {
        val recordsJson = json.encodeToString(tagData.records)
        val techListJson = json.encodeToString(tagData.techList)
        val title = customTitle ?: generateTagTitle(tagData)

        val entity = TagEntity(
            title = title,
            tagUid = tagData.uidHex,
            tagType = tagData.tagStandard,
            category = "SCANNED",
            timestamp = System.currentTimeMillis(),
            recordsJson = recordsJson,
            techListJson = techListJson,
            rawNdefHex = tagData.rawPayloadHex ?: "",
            memorySize = tagData.maxNdefSize,
            isReadOnly = !tagData.isWritable
        )
        return withContext(Dispatchers.IO) { tagDao.insertTag(entity) }
    }

    suspend fun saveTemplate(title: String, tagType: String, records: List<ParsedRecord>): Long {
        val recordsJson = json.encodeToString(records)
        val ndefBytes = com.example.nfcnexus.nfc.builder.NdefPayloadBuilder.buildNdefMessageFromRecords(records).toByteArray()
        val rawNdefHex = ndefBytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        val entity = TagEntity(
            title = title,
            tagUid = "CUSTOM_TEMPLATE",
            tagType = tagType,
            category = "TEMPLATE",
            timestamp = System.currentTimeMillis(),
            recordsJson = recordsJson,
            techListJson = json.encodeToString(listOf("android.nfc.tech.Ndef")),
            rawNdefHex = rawNdefHex,
            memorySize = 1024,
            isReadOnly = false
        )
        return withContext(Dispatchers.IO) { tagDao.insertTag(entity) }
    }

    suspend fun toggleFavorite(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.updateTag(tag.copy(isFavorite = !tag.isFavorite))
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tag)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        tagDao.deleteById(id)
    }

    suspend fun clearScannedHistory() = withContext(Dispatchers.IO) {
        tagDao.clearScannedHistory()
    }

    fun parseRecords(entity: TagEntity): List<ParsedRecord> {
        return try {
            json.decodeFromString<List<ParsedRecord>>(entity.recordsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseTechList(entity: TagEntity): List<String> {
        return try {
            json.decodeFromString<List<String>>(entity.techListJson)
        } catch (e: Exception) {
            listOf("android.nfc.tech.Ndef")
        }
    }

    fun exportToJson(tag: TagEntity): String {
        return json.encodeToString(tag)
    }

    fun exportAllToJson(tags: List<TagEntity>): String {
        return json.encodeToString(tags)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Check if array or single object
            if (jsonString.trimStart().startsWith("[")) {
                val list = json.decodeFromString<List<TagEntity>>(jsonString)
                val cleanList = list.map { it.copy(id = 0, timestamp = System.currentTimeMillis()) }
                tagDao.insertAll(cleanList)
                Result.success(cleanList.size)
            } else {
                val item = json.decodeFromString<TagEntity>(jsonString)
                tagDao.insertTag(item.copy(id = 0, timestamp = System.currentTimeMillis()))
                Result.success(1)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateTagTitle(tagData: NfcTagData): String {
        val firstRecord = tagData.records.firstOrNull()
        return when (firstRecord) {
            is ParsedRecord.Text -> firstRecord.text.take(24)
            is ParsedRecord.Uri -> firstRecord.title.ifEmpty { firstRecord.uri.take(24) }
            is ParsedRecord.Wifi -> "Wi-Fi: ${firstRecord.ssid}"
            is ParsedRecord.VCard -> "vCard: ${firstRecord.formattedName}"
            is ParsedRecord.Mime -> "MIME: ${firstRecord.mimeType}"
            is ParsedRecord.Aar -> "App: ${firstRecord.packageName.substringAfterLast('.')}"
            else -> "Tag ${tagData.uidHex.take(8)}"
        }
    }
}
