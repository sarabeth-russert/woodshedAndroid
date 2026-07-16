package com.russert.woodshed.ui.record

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.util.TimeFormatter

@Composable
fun RecordingControlsComposable(
    isRecording: Boolean,
    elapsedSeconds: Double,
    onToggle: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(bottom = 32.dp),
    ) {
        // Timer — only visible while recording
        Text(
            text = TimeFormatter.format(elapsedSeconds),
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
            color = Color.Red,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .then(if (isRecording) Modifier else Modifier.size(0.dp)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flip camera
            IconButton(
                onClick = onFlip,
                enabled = !isRecording,
                modifier = Modifier.size(50.dp),
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip camera",
                    tint = Cream.copy(alpha = if (isRecording) 0.3f else 1f),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Record button
            RecordButton(
                isRecording = isRecording,
                pulseScale = pulseScale,
                pulseAlpha = pulseAlpha,
                onClick = onToggle,
            )

            // Spacer for symmetry
            Spacer(Modifier.size(50.dp))
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    pulseScale: Float,
    pulseAlpha: Float,
    onClick: () -> Unit,
) {
    // Outer ring
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(3.dp, Cream),
        modifier = Modifier.size(72.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                // Pulsing ring
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Color.Red.copy(alpha = pulseAlpha)),
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale),
                ) {}

                // Stop square
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red,
                    modifier = Modifier.size(28.dp),
                ) {}
            } else {
                // Red filled circle
                Surface(
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier.size(58.dp),
                ) {}
            }
        }
    }
}
