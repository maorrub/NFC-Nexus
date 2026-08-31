package com.example.nfcnexus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TagEntity::class], version = 1, exportSchema = false)
abstract class NfcDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: NfcDatabase? = null

        fun getDatabase(context: Context): NfcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NfcDatabase::class.java,
                    "nfc_nexus_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialTemplates(database.tagDao())
                    }
                }
            }
        }

        private suspend fun populateInitialTemplates(dao: TagDao) {
            val templates = listOf(
                TagEntity(
                    title = "Guest Wi-Fi Access",
                    tagUid = "TEMPLATE_WIFI",
                    tagType = "NFC Forum Type 2",
                    category = "TEMPLATE",
                    isFavorite = true,
                    recordsJson = """[{"type":"com.example.nfcnexus.data.model.ParsedRecord.Wifi","ssid":"Nexus-Guest","authType":"WPA2-Personal","encryptionType":"AES","networkKey":"NexusGuestPass123!","macAddress":"","rawBytesHex":""}]""",
                    techListJson = """["android.nfc.tech.Ndef","android.nfc.tech.NfcA"]""",
                    memorySize = 504
                ),
                TagEntity(
                    title = "Developer Portfolio Link",
                    tagUid = "TEMPLATE_PORTFOLIO",
                    tagType = "NFC Forum Type 2",
                    category = "TEMPLATE",
                    isFavorite = true,
                    recordsJson = """[{"type":"com.example.nfcnexus.data.model.ParsedRecord.Uri","uri":"https://github.com/developer/portfolio","title":"My Portfolio","scheme":"https://","rawBytesHex":""}]""",
                    techListJson = """["android.nfc.tech.Ndef","android.nfc.tech.NfcA"]""",
                    memorySize = 504
                ),
                TagEntity(
                    title = "Executive Business Card",
                    tagUid = "TEMPLATE_VCARD",
                    tagType = "NFC Forum Type 4",
                    category = "TEMPLATE",
                    isFavorite = false,
                    recordsJson = """[{"type":"com.example.nfcnexus.data.model.ParsedRecord.VCard","formattedName":"Alex Nexus","organization":"Nexus Technologies","title":"Chief Solutions Architect","phoneNumbers":["+1-555-0199"],"emails":["alex@nexus-tech.io"],"urls":["https://nexus-tech.io"],"note":"NFC Architect & Engineer","address":"San Francisco, CA","rawVCard":"BEGIN:VCARD\nVERSION:3.0\nFN:Alex Nexus\nORG:Nexus Technologies\nTITLE:Chief Solutions Architect\nTEL:+1-555-0199\nEMAIL:alex@nexus-tech.io\nURL:https://nexus-tech.io\nNOTE:NFC Architect & Engineer\nADR:;;San Francisco;CA;;;USA\nEND:VCARD","rawBytesHex":""}]""",
                    techListJson = """["android.nfc.tech.IsoDep","android.nfc.tech.Ndef"]""",
                    memorySize = 2048
                )
            )
            dao.insertAll(templates)
        }
    }
}
