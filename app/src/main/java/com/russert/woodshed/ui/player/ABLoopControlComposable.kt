package com.russert.woodshed.ui.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.MutedGreen
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.util.TimeFormatter

@Composable
fun ABLoopControlComposable(
    state: PlayerUiState,
    isEditing: Boolean,
    onToggleLoop: () -> Unit,
    onToggleQuickLoop: () -> Unit,
    onAddSection: () -> Unit,
    onMoveSectionBoundary: (Int) -> Unit,
    onDeleteSection: (Int) -> Unit,
    onSelectSection: (Int) -> Unit,
    onClear: () -> Unit,
    onEditingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete all sections?") },
            text = { Text("This will remove all section markers.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onEditingChange(false)
                    onClear()
                }) { Text("Delete All", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            containerColor = DarkBrown,
            titleContentColor = Cream,
            textContentColor = Cream.copy(alpha = 0.7f),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Row 1: Add section button
        val atLimit = state.sectionCount > 0 && state.splitPoints.size >= 7
        val addLabel = if (state.sectionCount == 0) "Add Section"
                       else "Add Section at ${TimeFormatter.format(state.currentTime)}"

        Surface(
            onClick = onAddSection,
            shape = CircleShape,
            color = Amber.copy(alpha = if (atLimit) 0.05f else 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = if (atLimit) 0.15f else 0.4f)),
            enabled = !atLimit,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Amber.copy(alpha = if (atLimit) 0.3f else 1f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = addLabel,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = Amber.copy(alpha = if (atLimit) 0.3f else 1f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // Row 2: Loop + Quick Loop + Edit + Clear
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Loop toggle
            val loopActive = state.loopEnabled && state.sectionCount > 0
            LoopToggleButton(
                label = "Loop",
                active = loopActive,
                enabled = state.sectionCount > 0,
                onClick = onToggleLoop,
            )

            // Quick loop toggle
            LoopToggleButton(
                label = "Quick Loop",
                active = state.isQuickLoopActive,
                enabled = true,
                onClick = onToggleQuickLoop,
            )

            Spacer(Modifier.weight(1f))

            if (state.sectionCount > 0) {
                IconButton(
                    onClick = { onEditingChange(!isEditing) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (isEditing) "Stop editing" else "Edit sections",
                        tint = if (isEditing) Amber else Cream.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear all sections",
                        tint = Cream.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Row 3: Section pills
        if (state.sectionCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = if (isEditing) 10.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isEditing) 20.dp else 8.dp),
            ) {
                for (i in 0 until state.sectionCount) {
                    SectionPill(
                        index = i,
                        state = state,
                        isEditing = isEditing,
                        onSelect = { if (isEditing) onMoveSectionBoundary(i) else onSelectSection(i) },
                        onDelete = { onDeleteSection(i) },
                    )
                }
            }

            if (isEditing) {
                Text(
                    text = "Tap section to move boundary · Drag handles on timeline",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = Cream.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun LoopToggleButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) MutedGreen.copy(alpha = 0.2f) else WarmBrown.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) MutedGreen.copy(alpha = 0.6f) else WarmBrown.copy(alpha = if (enabled) 0.4f else 0.2f),
        ),
        enabled = enabled,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = if (!enabled) Cream.copy(alpha = 0.3f)
                    else if (active) MutedGreen
                    else Cream.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionPill(
    index: Int,
    state: PlayerUiState,
    isEditing: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val label = PlayerUiState.SECTION_LABELS.getOrElse(index) { "?" }
    val (start, end) = state.sectionRange(index)
    val isActive = state.activeSection == index

    Box {
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(8.dp),
            color = if (isActive) MutedGreen else WarmBrown.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when {
                    isEditing -> Amber.copy(alpha = 0.5f)
                    isActive  -> MutedGreen
                    else      -> WarmBrown.copy(alpha = 0.4f)
                },
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Amber.copy(alpha = 0.9f),
                            modifier = Modifier.size(8.dp).padding(end = 2.dp),
                        )
                    }
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isActive) DarkBrown else Cream,
                    )
                }
                Text(
                    text = "${TimeFormatter.format(start)}–${TimeFormatter.format(end)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = if (isActive) DarkBrown.copy(alpha = 0.7f) else Cream.copy(alpha = 0.5f),
                )
            }
        }

        // Delete badge (edit mode)
        if (isEditing) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp),
            ) {
                Surface(shape = CircleShape, color = DarkBrown) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete section",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
