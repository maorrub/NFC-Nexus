package com.example.nfcnexus.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcnexus.theme.DarkBackground
import com.example.nfcnexus.theme.NfcCyan
import com.example.nfcnexus.theme.NfcPurple

@Composable
fun NfcRadarOverlay(
    title: String,
    subtitle: String,
    isWriting: Boolean = false,
    accentColor: Color = if (isWriting) NfcPurple else NfcCyan,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAnimation")

    // Ripple 1
    val ripple1Radius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1"
    )
    val ripple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1Alpha"
    )

    // Ripple 2 (offset)
    val ripple2Radius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2Alpha"
    )

    // Pulse scale for center icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.94f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Radar Canvas with Center Target
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)

                    // Background concentric guide rings
                    for (r in listOf(60f, 120f, 180f, 240f)) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.12f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Expanding animated ripples
                    drawCircle(
                        color = accentColor.copy(alpha = ripple1Alpha.coerceIn(0f, 1f)),
                        radius = ripple1Radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = ripple2Alpha.coerceIn(0f, 1f)),
                        radius = ripple2Radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Center glowing NFC badge
                Surface(
                    modifier = Modifier
                        .size((76 * pulseScale).dp)
                        .clip(CircleShape),
                    color = accentColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, accentColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Target",
                            tint = accentColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Cancel Session")
            }
        }
    }
}
