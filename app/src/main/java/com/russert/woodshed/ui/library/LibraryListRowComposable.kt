package com.russert.woodshed.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.models.InstrumentType
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.util.TimeFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryListRowComposable(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart  -> { onDelete(); true }
                SwipeToDismissBoxValue.StartToEnd  -> { onShare(); false }
                SwipeToDismissBoxValue.Settled     -> false
            }
        }
    )

    // Reset after share swipe so the row snaps back
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val isEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val isStart = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val bgColor by animateColorAsState(
                when {
                    isEnd   -> Color.Red.copy(alpha = 0.85f)
                    isStart -> Amber
                    else    -> Color.Transparent
                },
                label = "swipeBackground"
            )
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = if (isEnd) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (isEnd) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else if (isStart) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                // Thumbnail (80 x 45 dp)
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(Theme.SmallCornerRadius))
                        .background(WarmBrown.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val thumbFile = recording.thumbnailPath?.let { File(context.filesDir, it) }
                    if (thumbFile != null && thumbFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(thumbFile).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            tint = Amber.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.tuneName.ifEmpty { "Untitled" },
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
                        color = Cream,
                        maxLines = 1,
                    )
                    Text(
                        text = recording.playerName,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Cream.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                    Text(
                        text = InstrumentType.fromDisplayName(recording.instrument).displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Amber.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }

                // Duration badge
                Text(
                    text = TimeFormatter.format(recording.duration),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = Cream.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
