package com.russert.woodshed.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryCardComposable(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(Theme.CornerRadius),
            color = WarmBrown.copy(alpha = 0.3f),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                ),
        ) {
            Column {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(
                            RoundedCornerShape(
                                topStart = Theme.CornerRadius,
                                topEnd = Theme.CornerRadius,
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val thumbFile = recording.thumbnailPath?.let { File(context.filesDir, it) }
                    if (thumbFile != null && thumbFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(thumbFile).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Surface(color = WarmBrown.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Amber.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.padding(10.dp),
                ) {
                    Text(
                        text = recording.tuneName.ifEmpty { "Untitled" },
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(fontSize = 15.sp),
                        color = Cream,
                        maxLines = 1,
                    )
                    Row(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = recording.playerName,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Cream.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = TimeFormatter.format(recording.duration),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Cream.copy(alpha = 0.5f),
                        )
                    }
                    Text(
                        text = InstrumentType.fromDisplayName(recording.instrument).displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Amber.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { showMenu = false; onShare() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }
}
