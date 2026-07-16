package com.russert.woodshed.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.WarmBrown

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f)

@Composable
fun SpeedControlComposable(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = Cream.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 4.dp),
        )

        SPEEDS.forEach { s ->
            val selected = speed == s
            Surface(
                onClick = { onSpeedChange(s) },
                shape = CircleShape,
                color = if (selected) Amber else WarmBrown.copy(alpha = 0.3f),
                contentColor = if (selected) DarkBrown else Cream.copy(alpha = 0.7f),
            ) {
                Text(
                    text = if (s == 1.0f) "1×" else "${s}×",
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
