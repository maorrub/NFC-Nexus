package com.example.nfcnexus.ui.emulation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.data.local.TagEntity
import com.example.nfcnexus.data.model.ApduLogEntry
import com.example.nfcnexus.data.repository.EmulatedCardProfile
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcGreen
import com.example.nfcnexus.theme.NfcPurple
import com.example.nfcnexus.theme.NfcPurpleDark

@Composable
fun NfcEmulateScreen(
    viewModel: NfcEmulateViewModel
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val apduLogs by viewModel.apduLogs.collectAsState()
    val totalTransactions by viewModel.totalTransactions.collectAsState()
    val savedTags by viewModel.savedTags.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Host Card Emulation",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                )
                Text(
                    text = "ISO-DEP Type 4 Tag Virtual Emulation",
                    style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                )
            }

            Surface(
                color = if (currentCard.isEnabled) NfcGreen.copy(alpha = 0.15f) else DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (currentCard.isEnabled) NfcGreen else DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentCard.isEnabled) Icons.Default.PlayArrow else Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = if (currentCard.isEnabled) NfcGreen else DarkTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentCard.isEnabled) "ACTIVE" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (currentCard.isEnabled) NfcGreen else DarkTextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Virtual Digital NFC Card Mockup
        DigitalNfcCard(card = currentCard, onToggleActive = { viewModel.toggleEmulation() })

        Spacer(modifier = Modifier.height(16.dp))

        // BIG TOGGLE BUTTON
        Button(
            onClick = { viewModel.toggleEmulation() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentCard.isEnabled) Color(0xFFFF5252).copy(alpha = 0.2f) else NfcCyan.copy(alpha = 0.2f),
                contentColor = if (currentCard.isEnabled) Color(0xFFFF5252) else NfcCyan
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (currentCard.isEnabled) Color(0xFFFF5252) else NfcCyan)
        ) {
            Icon(
                imageVector = if (currentCard.isEnabled) Icons.Default.PauseCircle else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (currentCard.isEnabled) "STOP EMULATING" else "START EMULATING",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Switcher from Saved Tags / Templates
        if (savedTags.isNotEmpty()) {
            Text(
                text = "Select Virtual Payload:",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedTags.take(8).forEach { tag ->
                    QuickCardPickerChip(
                        tag = tag,
                        isSelected = currentCard.title == tag.title,
                        onSelect = { viewModel.selectTagToEmulate(tag) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live APDU Transaction Feed Terminal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = NfcCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "APDU Transaction Log ($totalTransactions)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                )
            }

            if (apduLogs.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Log List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F)),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (apduLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Waiting for external NFC reader...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = DarkTextSecondary
                            )
                        )
                        Text(
                            "Tap this device against another NFC phone or reader.",
                            style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary.copy(alpha = 0.6f))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(apduLogs, key = { it.id }) { log ->
                        ApduLogItem(log = log)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun DigitalNfcCard(
    card: EmulatedCardProfile,
    onToggleActive: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CardPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F2027),
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (card.isEnabled) listOf(NfcCyan, NfcPurple) else listOf(DarkCardBorder, DarkCardBorder)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: App brand + Emulation switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = if (card.isEnabled) NfcCyan.copy(alpha = pulseAlpha) else DarkTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NFC NEXUS VIRTUAL CARD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (card.isEnabled) NfcCyan else DarkTextSecondary,
                                letterSpacing = 1.5.sp
                            )
                        )
                    }

                    Switch(
                        checked = card.isEnabled,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NfcCyan,
                            uncheckedThumbColor = DarkTextSecondary,
                            uncheckedTrackColor = DarkCard
                        )
                    )
                }

                // Middle: Card Title & Subtitle
                Column {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom row: Payload Type & Standard AID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.payloadType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NfcGreen
                        )
                    )
                    Text(
                        text = "AID: D2760000850101",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCardPickerChip(
    tag: TagEntity,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        color = if (isSelected) NfcPurple.copy(alpha = 0.25f) else DarkCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NfcPurple else DarkCardBorder),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SimCard,
                contentDescription = null,
                tint = if (isSelected) NfcPurple else DarkTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tag.title.take(20),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) NfcPurple else DarkTextPrimary
                )
            )
        }
    }
}

@Composable
fun ApduLogItem(log: ApduLogEntry) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (log.isSuccess) NfcGreen else Color(0xFFFF5252),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.commandName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NfcCyan
                        )
                    )
                }

                Surface(
                    color = if (log.isSuccess) NfcGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = log.statusCodeHex,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (log.isSuccess) NfcGreen else Color(0xFFFF5252),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (log.commandApduHex.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CMD: ${log.commandApduHex}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            if (log.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DarkTextPrimary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
