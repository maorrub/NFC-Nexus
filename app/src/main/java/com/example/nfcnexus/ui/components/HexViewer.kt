package com.example.nfcnexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nfcnexus.data.model.HexMemoryBlock
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcPurple

@Composable
fun MemoryHexInspectorDialog(
    blocks: List<HexMemoryBlock>,
    tagStandard: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Memory Inspector",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        )
                        Text(
                            text = "$tagStandard (${blocks.size} blocks)",
                            style = MaterialTheme.typography.bodySmall.copy(color = NfcCyan)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PAGE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkTextSecondary), modifier = Modifier.width(48.dp))
                    Text("HEX BYTES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkTextSecondary), modifier = Modifier.weight(1f))
                    Text("ASCII", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkTextSecondary), modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (blocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No direct sector memory readable on this tag protocol.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DarkTextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(blocks) { block ->
                            MemoryBlockRow(block = block)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryBlockRow(block: HexMemoryBlock) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Page / Block Index
        Row(
            modifier = Modifier.width(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0x%02X".format(block.index),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NfcCyan
                )
            )
            if (block.isReadOnly) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Read Only",
                    tint = NfcPurple,
                    modifier = Modifier.padding(1.dp)
                )
            }
        }

        // Hex Bytes
        Text(
            text = block.hexString,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = DarkTextPrimary,
                fontSize = 12.sp
            ),
            modifier = Modifier.weight(1f)
        )

        // ASCII representation
        Text(
            text = block.asciiString,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = DarkTextSecondary,
                fontSize = 12.sp
            ),
            modifier = Modifier.width(48.dp)
        )
    }
}
