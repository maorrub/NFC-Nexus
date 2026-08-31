package com.example.nfcnexus

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.ComponentName
import android.nfc.cardemulation.CardEmulation
import com.example.nfcnexus.hce.NfcNexusApduService
import com.example.nfcnexus.nfc.NfcReader
import com.example.nfcnexus.nfc.NfcSessionMode
import com.example.nfcnexus.theme.DarkBackground
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NFCNexusTheme
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcPurple
import com.example.nfcnexus.ui.clone.TagCloneViewModel
import com.example.nfcnexus.ui.components.TagCloneBanner
import com.example.nfcnexus.ui.emulation.NfcEmulateScreen
import com.example.nfcnexus.ui.emulation.NfcEmulateViewModel
import com.example.nfcnexus.ui.library.NfcLibraryScreen
import com.example.nfcnexus.ui.library.NfcLibraryViewModel
import com.example.nfcnexus.ui.reader.NfcReaderScreen
import com.example.nfcnexus.ui.reader.NfcReaderViewModel
import com.example.nfcnexus.ui.writer.NfcWriterScreen
import com.example.nfcnexus.ui.writer.NfcWriterViewModel

enum class AppTab(val label: String, val icon: ImageVector) {
    READ("Read", Icons.Default.Nfc),
    WRITE("Write", Icons.Default.Edit),
    EMULATE("Emulate", Icons.Default.SimCard),
    SAVED("Saved", Icons.Default.Bookmark)
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as NfcNexusApp }

    private val readerViewModel: NfcReaderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NfcReaderViewModel(app.tagRepository, app.nfcManager) as T
            }
        }
    }

    private val writerViewModel: NfcWriterViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NfcWriterViewModel(app.tagRepository, app.nfcManager) as T
            }
        }
    }

    private val emulateViewModel: NfcEmulateViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NfcEmulateViewModel(app.tagRepository) as T
            }
        }
    }

    private val libraryViewModel: NfcLibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NfcLibraryViewModel(app.tagRepository) as T
            }
        }
    }

    private val cloneViewModel: TagCloneViewModel by viewModels()

    private var currentAppTab by mutableStateOf(AppTab.READ)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NFCNexusTheme {
                MainAppShell(
                    readerViewModel = readerViewModel,
                    writerViewModel = writerViewModel,
                    emulateViewModel = emulateViewModel,
                    libraryViewModel = libraryViewModel,
                    cloneViewModel = cloneViewModel,
                    isNfcSupported = app.nfcManager.isNfcSupported,
                    isNfcEnabled = app.nfcManager.isNfcEnabled,
                    currentTab = currentAppTab,
                    onTabSelected = { tab ->
                        currentAppTab = tab
                        updateReaderMode()
                    }
                )
            }
        }

        handleNfcIntent(intent)
    }

    private fun updateReaderMode() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val componentName = ComponentName(this, NfcNexusApduService::class.java)

        if (nfcAdapter != null) {
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            if (currentAppTab == AppTab.EMULATE) {
                app.nfcManager.disableReaderMode(this)
                cardEmulation.setPreferredService(this, componentName)
            } else {
                cardEmulation.unsetPreferredService(this)
                app.nfcManager.enableReaderMode(this)
            }
        } else {
            if (currentAppTab == AppTab.EMULATE) {
                app.nfcManager.disableReaderMode(this)
            } else {
                app.nfcManager.enableReaderMode(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateReaderMode()
    }

    override fun onPause() {
        super.onPause()
        app.nfcManager.disableReaderMode(this)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            cardEmulation.unsetPreferredService(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED
        ) {
            val tag = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                app.nfcManager.onTagDiscovered(tag)
            }
        }
    }
}

@Composable
fun MainAppShell(
    readerViewModel: NfcReaderViewModel,
    writerViewModel: NfcWriterViewModel,
    emulateViewModel: NfcEmulateViewModel,
    libraryViewModel: NfcLibraryViewModel,
    cloneViewModel: TagCloneViewModel,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val stagedCloneTag by cloneViewModel.stagedTag.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = DarkBackground,
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Floating Staged Clone Bar
                    TagCloneBanner(
                        stagedTag = stagedCloneTag,
                        onCloneToTag = {
                            val msg = cloneViewModel.getStagedNdefMessage()
                            if (msg != null) {
                                writerViewModel.startCloneWriteSession(msg)
                                onTabSelected(AppTab.WRITE)
                            }
                        },
                        onEmulate = {
                            cloneViewModel.emulateStagedTag()
                            onTabSelected(AppTab.EMULATE)
                        },
                        onClear = { cloneViewModel.clearStagedTag() }
                    )

                    // Material 3 Bottom Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = DarkTextPrimary,
                        tonalElevation = 8.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onTabSelected(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NfcCyan,
                                    indicatorColor = NfcCyan,
                                    unselectedIconColor = DarkTextSecondary,
                                    unselectedTextColor = DarkTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    AppTab.READ -> NfcReaderScreen(
                        viewModel = readerViewModel,
                        cloneViewModel = cloneViewModel,
                        isNfcSupported = isNfcSupported,
                        isNfcEnabled = isNfcEnabled,
                        onNavigateToEmulate = { onTabSelected(AppTab.EMULATE) }
                    )
                    AppTab.WRITE -> NfcWriterScreen(
                        viewModel = writerViewModel
                    )
                    AppTab.EMULATE -> NfcEmulateScreen(
                        viewModel = emulateViewModel
                    )
                    AppTab.SAVED -> NfcLibraryScreen(
                        viewModel = libraryViewModel,
                        cloneViewModel = cloneViewModel,
                        onNavigateToEmulate = { onTabSelected(AppTab.EMULATE) }
                    )
                }
            }
        }
    }
}
