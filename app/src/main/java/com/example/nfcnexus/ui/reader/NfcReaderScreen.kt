package com.example.nfcnexus.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.data.model.NfcTagData
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcGreen
import com.example.nfcnexus.theme.NfcPurple
import com.example.nfcnexus.ui.clone.TagCloneViewModel
import com.example.nfcnexus.ui.components.MemoryHexInspectorDialog
import com.example.nfcnexus.ui.components.NfcRadarOverlay
import com.example.nfcnexus.ui.components.NfcStatusBanner
import com.example.nfcnexus.ui.components.RecordCardView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcReaderScreen(
    viewModel: NfcReaderViewModel,
    cloneViewModel: TagCloneViewModel,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onNavigateToEmulate: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tag Reader & Inspector",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    )
                    Text(
                        text = "Real-time NFC hardware and NDEF inspector",
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                    )
                }

                if (state.scannedTag != null) {
                    IconButton(onClick = { viewModel.clearScannedTag() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = DarkTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware Status Banner
            NfcStatusBanner(
                isNfcSupported = isNfcSupported,
                isNfcEnabled = isNfcEnabled,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Main Content Area
            if (state.scannedTag == null) {
                EmptyReaderPlaceholder(
                    isScanning = state.isScanning,
                    onScanClick = { viewModel.startScanning() }
                )
            } else {
                val tag = state.scannedTag!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Tag Hardware Summary Card
                    item {
                        TagHardwareSummaryCard(
                            tag = tag,
                            hasMemoryDump = state.memoryBlocks.isNotEmpty(),
                            onOpenMemoryInspector = { viewModel.setShowMemoryInspector(true) }
                        )
                    }

                    // 2. Action Bar: Clone, Emulate, Save
                    item {
                        TagActionBar(
                            tag = tag,
                            isSaved = state.isSavedToLibrary,
                            onClone = { cloneViewModel.stageTagForCloning(tag) },
                            onEmulate = {
                                cloneViewModel.stageTagForCloning(tag)
                                cloneViewModel.emulateStagedTag()
                                onNavigateToEmulate()
                            },
                            onSave = { viewModel.saveTagToLibrary() }
                        )
                    }

                    // 3. Section Title: NDEF Records
                    item {
                        Text(
                            text = "NDEF Payload Records (${tag.records.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // 4. Record Cards List
                    if (tag.records.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No NDEF message found on this tag (Raw or formatted empty tag).",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = DarkTextSecondary)
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(tag.records) { index, record ->
                            RecordCardView(record = record, index = index)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Active Scanning Radar Overlay
        if (state.isScanning) {
            NfcRadarOverlay(
                title = "Scanning for NFC Tag",
                subtitle = "Hold the tag firmly against the NFC antenna on the back of your device.",
                onCancel = { viewModel.stopScanning() }
            )
        }

        // Memory Hex Inspector Dialog
        if (state.showMemoryInspector && state.scannedTag != null) {
            MemoryHexInspectorDialog(
                blocks = state.memoryBlocks,
                tagStandard = state.scannedTag!!.tagStandard,
                onDismiss = { viewModel.setShowMemoryInspector(false) }
            )
        }
    }
}

@Composable
private fun EmptyReaderPlaceholder(
    isScanning: Boolean,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                color = NfcCyan.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(2.dp, NfcCyan.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "Scan NFC",
                        tint = NfcCyan,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ready to Inspect Tags",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap the button below or approach any NFC tag, card, or sticker to inspect hardware metadata and payload records.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkTextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = NfcCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tag Scan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagHardwareSummaryCard(
    tag: NfcTagData,
    hasMemoryDump: Boolean,
    onOpenMemoryInspector: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Tag Type & Lock status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tag.tagStandard,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NfcCyan
                        )
                    )
                    Text(
                        text = "UID: ${tag.uidHex}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    )
                }

                Surface(
                    color = if (tag.isWritable) NfcGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (tag.isWritable) NfcGreen else Color(0xFFFF5252)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (tag.isWritable) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (tag.isWritable) NfcGreen else Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (tag.isWritable) "Writable" else "Locked",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (tag.isWritable) NfcGreen else Color(0xFFFF5252)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Tech List Chips
            Text("Supported Technologies:", style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tag.techList.forEach { tech ->
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = tech.substringAfterLast('.'),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NfcPurple
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware details grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                tag.sak?.let {
                    Column {
                        Text("SAK", style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary))
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = DarkTextPrimary))
                    }
                }
                tag.atqa?.let {
                    Column {
                        Text("ATQA", style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary))
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = DarkTextPrimary))
                    }
                }
                Column {
                    Text("Memory Size", style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary))
                    Text(
                        if (tag.maxNdefSize > 0) "${tag.currentNdefSize} / ${tag.maxNdefSize} B" else "Unknown",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = DarkTextPrimary)
                    )
                }
            }

            if (hasMemoryDump) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onOpenMemoryInspector,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DataArray, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect Memory Pages (Hex)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TagActionBar(
    tag: NfcTagData,
    isSaved: Boolean,
    onClone: () -> Unit,
    onEmulate: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClone,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NfcPurple, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Tag", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        FilledTonalButton(
            onClick = onEmulate,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Emulate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = onSave,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint = if (isSaved) NfcGreen else DarkTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
