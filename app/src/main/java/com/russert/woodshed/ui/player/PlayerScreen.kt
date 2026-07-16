package com.russert.woodshed.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.russert.woodshed.ui.util.launchShareIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.models.InstrumentType
import com.russert.woodshed.ui.navigation.Screen
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recording = state.recording
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { file -> launchShareIntent(context, file) }
    }

    var isEditing by remember { mutableStateOf(false) }
    var showTimestampSheet by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val timestampSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Pause when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recording?", color = Cream, fontFamily = FontFamily.Serif) },
            text = { Text("This cannot be undone.", color = Cream.copy(alpha = 0.7f), fontFamily = FontFamily.Serif) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteRecording()
                    navController.popBackStack()
                }) { Text("Delete", color = Color.Red, fontFamily = FontFamily.Serif) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Cream.copy(alpha = 0.7f), fontFamily = FontFamily.Serif)
                }
            },
            containerColor = DarkBrown,
        )
    }

    if (showTimestampSheet) {
        AddTimestampSheet(
            currentTime = state.currentTime,
            sheetState = timestampSheetState,
            onDismiss = { showTimestampSheet = false },
            onSave = { label -> viewModel.addTimestamp(label) },
        )
    }

    // In edit mode the scrubber shows draggable lollipop handles for all section boundaries.
    // editMarkers = [sectionStart, splitPoint0, splitPoint1, ...]
    val editMarkers = if (state.sectionsEnabled && isEditing) {
        listOf(state.sectionStart) + state.splitPoints
    } else emptyList()

    Scaffold(
        containerColor = DarkBrown,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recording?.tuneName ?: "",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        color = Cream,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Cream,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTimestampSheet = true }) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = "Add chapter",
                            tint = Amber.copy(alpha = 0.8f),
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Cream.copy(alpha = 0.8f),
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false },
                            containerColor = WarmBrown,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Edit recording", color = Cream, fontFamily = FontFamily.Serif)
                                },
                                onClick = {
                                    showOverflow = false
                                    recording?.let {
                                        navController.navigate(Screen.EditRecording.createRoute(it.id))
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Share", color = Cream, fontFamily = FontFamily.Serif)
                                },
                                onClick = {
                                    showOverflow = false
                                    viewModel.shareRecording()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete", color = Color.Red, fontFamily = FontFamily.Serif)
                                },
                                onClick = {
                                    showOverflow = false
                                    showDeleteDialog = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBrown),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Video surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Theme.Padding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Recording metadata header
                if (recording != null) {
                    RecordingInfoHeader(recording = recording)
                }

                // Timeline scrubber
                ScrubberComposable(
                    currentTime = state.currentTime,
                    duration = state.duration,
                    timestamps = state.timestamps,
                    loopEnabled = state.loopEnabled,
                    loopStart = state.loopStart,
                    loopEnd = state.loopEnd,
                    splitPoints = state.splitPoints,
                    sectionStart = state.sectionStart,
                    sectionsEnabled = state.sectionsEnabled,
                    isQuickLoopActive = state.isQuickLoopActive,
                    quickLoopIn = state.quickLoopIn,
                    quickLoopOut = state.quickLoopOut,
                    isEditMode = isEditing,
                    editMarkers = editMarkers,
                    onSeek = { viewModel.seek(it) },
                    onScrubbingChanged = { viewModel.setScrubbing(it) },
                    onQuickLoopInMoved = { viewModel.setQuickLoopIn(it) },
                    onQuickLoopOutMoved = { viewModel.setQuickLoopOut(it) },
                    onDeactivateQuickLoop = { viewModel.deactivateQuickLoop() },
                    onMoveMarker = { index, time ->
                        if (index == 0) viewModel.setSectionStart(time)
                        else viewModel.setSplitPoint(index - 1, time)
                    },
                )

                // Transport controls
                PlayerControlsComposable(
                    isPlaying = state.isPlaying,
                    onSkipBack = { viewModel.skipBackward() },
                    onTogglePlay = { viewModel.togglePlayback() },
                    onSkipForward = { viewModel.skipForward() },
                )

                // Playback speed
                SpeedControlComposable(
                    speed = state.playbackSpeed,
                    onSpeedChange = { viewModel.setSpeed(it) },
                )

                HorizontalDivider(color = WarmBrown.copy(alpha = 0.3f))

                // A/B section loop controls
                ABLoopControlComposable(
                    state = state,
                    isEditing = isEditing,
                    onToggleLoop = { viewModel.toggleLoop() },
                    onToggleQuickLoop = { viewModel.toggleQuickLoop() },
                    onAddSection = { viewModel.addSplitPoint() },
                    onMoveSectionBoundary = { sectionIndex ->
                        viewModel.moveSectionBoundary(sectionIndex)
                    },
                    onDeleteSection = { viewModel.deleteSection(it) },
                    onSelectSection = { viewModel.setActiveSection(it) },
                    onClear = { viewModel.clearLoop() },
                    onEditingChange = { isEditing = it },
                )

                HorizontalDivider(color = WarmBrown.copy(alpha = 0.3f))

                // Chapter timestamps
                TimestampListComposable(
                    timestamps = state.timestamps,
                    currentTime = state.currentTime,
                    onSeek = { viewModel.seekToTimestamp(it) },
                    onDelete = { viewModel.deleteTimestamp(it) },
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun RecordingInfoHeader(recording: RecordingEntity) {
    val instrument = InstrumentType.fromDisplayName(recording.instrumentType)
    val dateStr = remember(recording.dateRecorded) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(recording.dateRecorded)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Instrument emoji + player name
        if (recording.playerName.isNotBlank() || instrument != InstrumentType.OTHER) {
            Text(
                text = buildString {
                    if (instrument != InstrumentType.OTHER) append("${instrument.emoji}  ")
                    if (recording.playerName.isNotBlank()) append(recording.playerName)
                    else append(instrument.displayName)
                },
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                color = Cream.copy(alpha = 0.7f),
            )
        }

        // Tuning
        if (recording.tuning.isNotBlank()) {
            Text(
                text = recording.tuning,
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = Amber.copy(alpha = 0.8f),
            )
        }

        // Date + notes preview
        Text(
            text = dateStr,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Cream.copy(alpha = 0.4f),
        )

        if (recording.notes.isNotBlank()) {
            Text(
                text = recording.notes,
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = Cream.copy(alpha = 0.55f),
                maxLines = 3,
            )
        }

        // Tags
        val tags = recording.tagList
        if (tags.isNotEmpty()) {
            Text(
                text = tags.joinToString("  ·  ") { "#$it" },
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = Amber.copy(alpha = 0.5f),
            )
        }
    }
}
