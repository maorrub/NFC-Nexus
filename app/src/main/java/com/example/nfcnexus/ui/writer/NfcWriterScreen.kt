package com.example.nfcnexus.ui.writer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.nfc.builder.WifiTlvEncoder
import com.example.nfcnexus.theme.DarkCard
import com.example.nfcnexus.theme.DarkCardBorder
import com.example.nfcnexus.theme.DarkSurface
import com.example.nfcnexus.theme.DarkTextPrimary
import com.example.nfcnexus.theme.DarkTextSecondary
import com.example.nfcnexus.theme.NfcAmber
import com.example.nfcnexus.theme.NfcCoral
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcGreen
import com.example.nfcnexus.theme.NfcPurple
import com.example.nfcnexus.ui.components.NfcRadarOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcWriterScreen(
    viewModel: NfcWriterViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateTitleInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Header
            Text(
                text = "Tag Writer & Formatter",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary
                )
            )
            Text(
                text = "Assemble standard NDEF records, format tags or lock them permanently",
                style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payload Type Selector (Horizontal scrollable chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WritePayloadType.entries.forEach { type ->
                    val isSelected = state.selectedType == type
                    Surface(
                        color = if (isSelected) NfcCyan.copy(alpha = 0.2f) else DarkCard,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NfcCyan else DarkCardBorder
                        ),
                        modifier = Modifier.clickable { viewModel.setPayloadType(type) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (type) {
                                WritePayloadType.TEXT -> Icons.AutoMirrored.Filled.Notes
                                WritePayloadType.URL -> Icons.Default.Link
                                WritePayloadType.WIFI -> Icons.Default.Wifi
                                WritePayloadType.VCARD -> Icons.Default.Person
                                WritePayloadType.MIME -> Icons.Default.Code
                                WritePayloadType.AAR -> Icons.Default.Android
                            }

                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) NfcCyan else DarkTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NfcCyan else DarkTextPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Form Editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure ${state.selectedType.label}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    when (state.selectedType) {
                        WritePayloadType.TEXT -> {
                            OutlinedTextField(
                                value = state.textContent,
                                onValueChange = { viewModel.updateTextFields(it) },
                                label = { Text("Note / Plain Text") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = customTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.textLanguage,
                                onValueChange = { viewModel.updateTextFields(state.textContent, it) },
                                label = { Text("Language Code (e.g. en, he, es)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }

                        WritePayloadType.URL -> {
                            OutlinedTextField(
                                value = state.urlContent,
                                onValueChange = { viewModel.updateUrl(it) },
                                label = { Text("Full URL or URI (https://, tel:, mailto:)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }

                        WritePayloadType.WIFI -> {
                            var passVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = state.wifiSsid,
                                onValueChange = { viewModel.updateWifi(it, state.wifiPassword, state.wifiAuthType) },
                                label = { Text("Network SSID (Name)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Security Dropdown
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = state.wifiAuthType.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Security Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = customTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    WifiTlvEncoder.AuthType.entries.forEach { auth ->
                                        DropdownMenuItem(
                                            text = { Text(auth.name) },
                                            onClick = {
                                                viewModel.updateWifi(state.wifiSsid, state.wifiPassword, auth)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.wifiPassword,
                                onValueChange = { viewModel.updateWifi(state.wifiSsid, it, state.wifiAuthType) },
                                label = { Text("Network Password (WPA Key)") },
                                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passVisible = !passVisible }) {
                                        Icon(
                                            if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = DarkTextSecondary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }

                        WritePayloadType.VCARD -> {
                            OutlinedTextField(
                                value = state.vcardName,
                                onValueChange = { viewModel.updateVCard(it, state.vcardOrg, state.vcardTitle, state.vcardPhone, state.vcardEmail, state.vcardUrl, state.vcardAddress, state.vcardNote) },
                                label = { Text("Full Name *") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.vcardOrg,
                                    onValueChange = { viewModel.updateVCard(state.vcardName, it, state.vcardTitle, state.vcardPhone, state.vcardEmail, state.vcardUrl, state.vcardAddress, state.vcardNote) },
                                    label = { Text("Company") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = state.vcardTitle,
                                    onValueChange = { viewModel.updateVCard(state.vcardName, state.vcardOrg, it, state.vcardPhone, state.vcardEmail, state.vcardUrl, state.vcardAddress, state.vcardNote) },
                                    label = { Text("Job Title") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.vcardPhone,
                                    onValueChange = { viewModel.updateVCard(state.vcardName, state.vcardOrg, state.vcardTitle, it, state.vcardEmail, state.vcardUrl, state.vcardAddress, state.vcardNote) },
                                    label = { Text("Phone") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = state.vcardEmail,
                                    onValueChange = { viewModel.updateVCard(state.vcardName, state.vcardOrg, state.vcardTitle, state.vcardPhone, it, state.vcardUrl, state.vcardAddress, state.vcardNote) },
                                    label = { Text("Email") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.vcardUrl,
                                onValueChange = { viewModel.updateVCard(state.vcardName, state.vcardOrg, state.vcardTitle, state.vcardPhone, state.vcardEmail, it, state.vcardAddress, state.vcardNote) },
                                label = { Text("Website") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }

                        WritePayloadType.MIME -> {
                            OutlinedTextField(
                                value = state.mimeType,
                                onValueChange = { viewModel.updateMime(it, state.mimeContent) },
                                label = { Text("MIME Type (e.g. application/json, text/plain)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.mimeContent,
                                onValueChange = { viewModel.updateMime(state.mimeType, it) },
                                label = { Text("Payload String / Content") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }

                        WritePayloadType.AAR -> {
                            OutlinedTextField(
                                value = state.aarPackageName,
                                onValueChange = { viewModel.updateAar(it) },
                                label = { Text("Android Package Name (e.g. com.example.app)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startWriteSession() },
                            colors = ButtonDefaults.buttonColors(containerColor = NfcCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Write to Tag", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                templateTitleInput = "${state.selectedType.label} Template"
                                showSaveTemplateDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Template")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Operations Card (Format, Lock)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tag Maintenance & Security",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.startFormatEraseSession() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = NfcAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Format / Erase", color = NfcAmber)
                        }

                        OutlinedButton(
                            onClick = { viewModel.requestLockTag() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NfcCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Make Read-Only", color = NfcCoral)
                        }
                    }
                }
            }

            // Status message toast / alert if any
            state.statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (state.isSuccess) NfcGreen.copy(alpha = 0.15f) else NfcCoral.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isSuccess) NfcGreen else NfcCoral),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (state.isSuccess) NfcGreen else NfcCoral,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Active Writing Radar Overlay
        if (state.isWriting) {
            NfcRadarOverlay(
                title = state.writeModeName,
                subtitle = state.statusMessage ?: "Hold target tag steady against back of device.",
                isWriting = true,
                onCancel = { viewModel.cancelSession() }
            )
        }

        // Save Template Dialog
        if (showSaveTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
                title = { Text("Save as Reusable Template", color = DarkTextPrimary) },
                text = {
                    Column {
                        Text("Give your template a title to easily access it from the Library tab:", color = DarkTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = templateTitleInput,
                            onValueChange = { templateTitleInput = it },
                            label = { Text("Template Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveAsTemplate(templateTitleInput.ifEmpty { "Saved Template" })
                            showSaveTemplateDialog = false
                            Toast.makeText(context, "Template saved to Library!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NfcCyan, contentColor = Color.Black)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) {
                        Text("Cancel", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Make Read-Only Warning Confirmation Dialog
        if (state.showLockConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissLockDialog() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = NfcCoral, modifier = Modifier.size(36.dp)) },
                title = { Text("Permanently Lock Tag?", fontWeight = FontWeight.Bold, color = DarkTextPrimary) },
                text = {
                    Text(
                        "WARNING: Making an NFC tag Read-Only is a permanent, irreversible hardware lock. You will NEVER be able to rewrite or erase this tag again.\n\nAre you sure you want to proceed?",
                        color = DarkTextSecondary,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmLockSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = NfcCoral, contentColor = Color.White)
                    ) {
                        Text("Yes, Permanently Lock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissLockDialog() }) {
                        Text("Cancel", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NfcCyan,
    unfocusedBorderColor = DarkCardBorder,
    focusedLabelColor = NfcCyan,
    unfocusedLabelColor = DarkTextSecondary,
    focusedTextColor = DarkTextPrimary,
    unfocusedTextColor = DarkTextPrimary,
    cursorColor = NfcCyan
)
