package com.example.nfcnexus.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiPassword
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.data.model.ParsedRecord
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcGreen
import com.example.nfcnexus.theme.NfcPurple

@Composable
fun RecordCardView(
    record: ParsedRecord,
    index: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Record Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecordTypeBadge(record = record)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Record #${index + 1}: ${record.recordTypeName}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Body based on record type
            when (record) {
                is ParsedRecord.Text -> TextRecordContent(record, context)
                is ParsedRecord.Uri -> UriRecordContent(record, context)
                is ParsedRecord.Wifi -> WifiRecordContent(record, context)
                is ParsedRecord.VCard -> VCardRecordContent(record, context)
                is ParsedRecord.Mime -> MimeRecordContent(record, context)
                is ParsedRecord.Aar -> AarRecordContent(record, context)
                is ParsedRecord.Unknown -> UnknownRecordContent(record, context)
            }
        }
    }
}

@Composable
fun RecordTypeBadge(record: ParsedRecord) {
    val (icon, color) = when (record) {
        is ParsedRecord.Text -> Icons.AutoMirrored.Filled.Notes to NfcCyan
        is ParsedRecord.Uri -> Icons.Default.Link to NfcPurple
        is ParsedRecord.Wifi -> Icons.Default.Wifi to NfcGreen
        is ParsedRecord.VCard -> Icons.Default.Person to Color(0xFFFF80AB)
        is ParsedRecord.Mime -> Icons.Default.Code to Color(0xFFFFD54F)
        is ParsedRecord.Aar -> Icons.Default.Android to Color(0xFF69F0AE)
        is ParsedRecord.Unknown -> Icons.Default.Code to Color(0xFFB0BEC5)
    }


    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TextRecordContent(record: ParsedRecord.Text, context: Context) {
    Column {
        Text(
            text = record.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = DarkTextPrimary,
                lineHeight = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Language: ${record.languageCode.uppercase()}  |  Encoding: ${record.encoding}",
                style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
            )
            IconButton(
                onClick = {
                    copyToClipboard(context, "NFC Text", record.text)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NfcCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UriRecordContent(record: ParsedRecord.Uri, context: Context) {
    Column {
        Text(
            text = record.uri,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = NfcCyan,
                fontFamily = FontFamily.Monospace
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.uri))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open URI: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Link")
            }
            OutlinedButton(
                onClick = { copyToClipboard(context, "NFC Link", record.uri) }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WifiRecordContent(record: ParsedRecord.Wifi, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SSID / Network Name:", style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary))
            Text(record.ssid, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DarkTextPrimary))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Security Auth:", style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary))
            Text(record.authType, style = MaterialTheme.typography.bodySmall.copy(color = NfcGreen))
        }
        if (record.networkKey.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Password:", style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary))
                Text(
                    text = record.networkKey,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = DarkTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    copyToClipboard(context, "Wi-Fi Password", record.networkKey)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.WifiPassword, contentDescription = "Copy Password", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Password")
            }
        }
    }
}

@Composable
private fun VCardRecordContent(record: ParsedRecord.VCard, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = record.formattedName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )
        )
        if (record.title.isNotEmpty() || record.organization.isNotEmpty()) {
            Text(
                text = listOf(record.title, record.organization).filter { it.isNotEmpty() }.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
            )
        }
        if (record.phoneNumbers.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = NfcCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(record.phoneNumbers.joinToString(", "), style = MaterialTheme.typography.bodySmall.copy(color = DarkTextPrimary))
            }
        }
        if (record.emails.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = NfcPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(record.emails.joinToString(", "), style = MaterialTheme.typography.bodySmall.copy(color = DarkTextPrimary))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { copyToClipboard(context, "vCard Data", record.rawVCard) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy vCard Raw Text")
        }
    }
}

@Composable
private fun MimeRecordContent(record: ParsedRecord.Mime, context: Context) {
    Column {
        Text(
            text = "MIME Type: ${record.mimeType}",
            style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (record.contentString != null) {
            Text(
                text = record.contentString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = DarkTextPrimary
                )
            )
        } else {
            Text(
                text = record.payloadHex.take(64) + if (record.payloadHex.length > 64) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = DarkTextSecondary
                )
            )
        }
    }
}

@Composable
private fun AarRecordContent(record: ParsedRecord.Aar, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Target Android Application:", style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary))
            Text(
                text = record.packageName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NfcGreen
                )
            )
        }
        OutlinedButton(
            onClick = {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(record.packageName)
                    if (intent != null) context.startActivity(intent)
                    else {
                        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${record.packageName}"))
                        context.startActivity(playIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open package", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Launch")
        }
    }
}

@Composable
private fun UnknownRecordContent(record: ParsedRecord.Unknown, context: Context) {
    Column {
        Text("Type: ${record.tnfName}", style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Payload (Hex): ${record.payloadHex.take(64)}${if (record.payloadHex.length > 64) "..." else ""}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = DarkTextSecondary
            )
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
