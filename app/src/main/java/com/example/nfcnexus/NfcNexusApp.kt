package com.example.nfcnexus

import android.app.Application
import com.example.nfcnexus.data.local.NfcDatabase
import com.example.nfcnexus.data.repository.TagRepository
import com.example.nfcnexus.nfc.NfcManager

class NfcNexusApp : Application() {

    val database by lazy { NfcDatabase.getDatabase(this) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
    val nfcManager by lazy { NfcManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NfcNexusApp
            private set
    }
}
