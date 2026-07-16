package com.russert.woodshed.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream

@Composable
fun PlayerControlsComposable(
    isPlaying: Boolean,
    onSkipBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Replay5,
                contentDescription = "Skip back 5 seconds",
                tint = Cream,
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onTogglePlay, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Amber,
                modifier = Modifier.size(64.dp),
            )
        }

        IconButton(onClick = onSkipForward, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Forward5,
                contentDescription = "Skip forward 5 seconds",
                tint = Cream,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
