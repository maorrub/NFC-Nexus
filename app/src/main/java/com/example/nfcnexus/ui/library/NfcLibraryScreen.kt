package com.example.nfcnexus.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.data.local.TagEntity
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcAmber
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcGreen
import com.example.nfcnexus.theme.NfcPurple
import com.example.nfcnexus.ui.clone.TagCloneViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NfcLibraryScreen(
    viewModel: NfcLibraryViewModel,
    cloneViewModel: TagCloneViewModel,
    onNavigateToEmulate: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val tags by viewModel.tagsList.collectAsState()
    val context = LocalContext.current
    var importInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Tags & Library",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                )
                Text(
                    text = "${tags.size} stored tag profiles & history",
                    style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.showImportDialog(true) }) {
                    Icon(Icons.Default.Download, contentDescription = "Import", tint = NfcCyan)
                }
                IconButton(onClick = { viewModel.exportAll() }) {
                    Icon(Icons.Default.Share, contentDescription = "Export All", tint = NfcPurple)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkTextSecondary)
                    }
                }
            },
            placeholder = { Text("Search tags, UIDs, or records...", color = DarkTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NfcCyan,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = DarkTextPrimary,
                unfocusedTextColor = DarkTextPrimary,
                cursorColor = NfcCyan
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Category Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.entries.forEach { filter ->
                val isSelected = state.selectedFilter == filter
                Surface(
                    color = if (isSelected) NfcCyan.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NfcCyan else DarkCardBorder),
                    modifier = Modifier.clickable { viewModel.setFilter(filter) }
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NfcCyan else DarkTextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tags List
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved tags in this category.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DarkTextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tags, key = { it.id }) { tag ->
                    val parsedRecords = viewModel.parseRecords(tag)
                    LibraryTagCard(
                        tag = tag,
                        records = parsedRecords,
                        onToggleFavorite = { viewModel.toggleFavorite(tag) },
                        onClone = {
                            val rawBytes = if (tag.rawNdefHex.isNotEmpty()) {
                                hexStringToByteArray(tag.rawNdefHex)
                            } else byteArrayOf()
                            cloneViewModel.stageDirectPayload(tag.title, parsedRecords, rawBytes)
                            Toast.makeText(context, "Staged for cloning!", Toast.LENGTH_SHORT).show()
                        },
                        onEmulate = {
                            val rawBytes = if (tag.rawNdefHex.isNotEmpty()) {
                                hexStringToByteArray(tag.rawNdefHex)
                            } else byteArrayOf()
                            cloneViewModel.stageDirectPayload(tag.title, parsedRecords, rawBytes)
                            cloneViewModel.emulateStagedTag()
                            onNavigateToEmulate()
                        },
                        onExport = { viewModel.exportTag(tag) },
                        onDelete = { viewModel.deleteTag(tag) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Export JSON Dialog
    state.exportJsonDialogContent?.let { jsonStr ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            title = { Text("Exported JSON Data", color = DarkTextPrimary) },
            text = {
                Column {
                    Text("Copy exported JSON payload:", color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    ) {
                        Text(
                            text = jsonStr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = DarkTextPrimary
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("NFC Export", jsonStr))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        viewModel.dismissExportDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NfcCyan, contentColor = Color.Black)
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportDialog() }) {
                    Text("Close", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Import JSON Dialog
    if (state.showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showImportDialog(false) },
            title = { Text("Import Tag Profiles", color = DarkTextPrimary) },
            text = {
                Column {
                    Text("Paste JSON array or single tag object below:", color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("Paste JSON here...", color = DarkTextSecondary) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NfcCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            cursorColor = NfcCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            viewModel.importFromJson(importInputText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) importInputText = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NfcCyan, contentColor = Color.Black)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showImportDialog(false) }) {
                    Text("Cancel", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun LibraryTagCard(
    tag: TagEntity,
    records: List<ParsedRecord>,
    onToggleFavorite: () -> Unit,
    onClone: () -> Unit,
    onEmulate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(tag.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title, Category Badge, Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when (tag.category) {
                            "TEMPLATE" -> NfcPurple.copy(alpha = 0.2f)
                            "CLONED" -> NfcGreen.copy(alpha = 0.2f)
                            else -> NfcCyan.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (tag.category) {
                                "TEMPLATE" -> NfcPurple
                                "CLONED" -> NfcGreen
                                else -> NfcCyan
                            }
                        )
                    ) {
                        Text(
                            text = tag.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (tag.category) {
                                    "TEMPLATE" -> NfcPurple
                                    "CLONED" -> NfcGreen
                                    else -> NfcCyan
                                },
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tag.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (tag.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (tag.isFavorite) NfcAmber else DarkTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle info: Type + Date
            Text(
                text = "${tag.tagType} • $dateString",
                style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary, fontSize = 11.sp)
            )

            // Records preview summary
            if (records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                records.take(2).forEach { record ->
                    Text(
                        text = "• ${record.recordTypeName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkTextPrimary, fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Card Action Buttons: Clone, Emulate, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onClone,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clone", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onEmulate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Emulate", fontSize = 12.sp)
                    }
                }

                Row {
                    IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Export JSON", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun hexStringToByteArray(s: String): ByteArray {
    val clean = s.replace(":", "").replace(" ", "").replace("\n", "")
    val len = clean.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
        i += 2
    }
    return data
}
