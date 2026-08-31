package com.example.nfcnexus.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcAmber
import com.example.nfcnexus.theme.NfcCoral

@Composable
fun NfcStatusBanner(
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!isNfcSupported) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcCoral.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "NFC Unsupported",
                    tint = NfcCoral,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NFC Hardware Unavailable",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    )
                    Text(
                        text = "This device does not have an NFC chip. You can still test templates & payload builders.",
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                    )
                }
            }
        }
    } else if (!isNfcEnabled) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcAmber.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Disabled",
                        tint = NfcAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "NFC is Turned Off",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        )
                        Text(
                            text = "Turn on NFC to scan and write tags.",
                            style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NfcAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Turn On", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
