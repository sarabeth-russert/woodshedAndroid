package com.russert.woodshed.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.data.db.VideoTimestampEntity
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.util.TimeFormatter
import kotlin.math.abs

@Composable
fun TimestampListComposable(
    timestamps: List<VideoTimestampEntity>,
    currentTime: Double,
    onSeek: (VideoTimestampEntity) -> Unit,
    onDelete: (VideoTimestampEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "CHAPTERS",
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = Cream.copy(alpha = 0.5f),
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (timestamps.isEmpty()) {
            Text(
                text = "No chapters yet — pause and tap the bookmark icon to add one",
                fontSize = 14.sp,
                color = Cream.copy(alpha = 0.4f),
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            timestamps.forEach { ts ->
                TimestampRow(
                    timestamp = ts,
                    isActive = abs(currentTime - ts.timeOffset) < 2.0,
                    onSeek = { onSeek(ts) },
                    onDelete = { onDelete(ts) },
                )
            }
        }
    }
}

@Composable
private fun TimestampRow(
    timestamp: VideoTimestampEntity,
    isActive: Boolean,
    onSeek: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Theme.SmallCornerRadius),
        color = if (isActive) Amber.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSeek() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
        ) {
            // Active indicator dot
            Surface(
                shape = CircleShape,
                color = if (isActive) Amber else WarmBrown.copy(alpha = 0.4f),
                modifier = Modifier.size(8.dp),
            ) {}

            Spacer(Modifier.width(8.dp))

            Text(
                text = TimeFormatter.format(timestamp.timeOffset),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) Amber else Cream.copy(alpha = 0.6f),
                modifier = Modifier.width(50.dp),
            )

            Text(
                text = timestamp.label,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = if (isActive) Cream else Cream.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete chapter",
                    tint = WarmBrown.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
