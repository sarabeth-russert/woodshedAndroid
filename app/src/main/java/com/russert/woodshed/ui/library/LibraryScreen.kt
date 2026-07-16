package com.russert.woodshed.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.russert.woodshed.ui.util.launchShareIntent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.data.preferences.LibraryViewMode
import com.russert.woodshed.ui.navigation.Screen
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { file -> launchShareIntent(context, file) }
    }

    Scaffold(
        containerColor = DarkBrown,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Woodshed",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                            color = Cream,
                        )
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleViewMode) {
                            Icon(
                                imageVector = if (state.viewMode == LibraryViewMode.LIST) Icons.Default.GridView
                                              else Icons.Default.ViewList,
                                contentDescription = "Toggle view",
                                tint = Amber,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBrown),
                )
                SearchField(
                    text = state.searchText,
                    onTextChange = viewModel::setSearchText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.Padding)
                        .padding(bottom = Theme.SmallPadding),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.recordings.isEmpty() && !state.isSearching -> EmptyStateView()
                state.isSearching -> SearchResultsContent(state, navController, viewModel)
                state.sortOption.key == "tuning" -> TuningGroupContent(state, navController, viewModel)
                else -> FlatContent(state, navController, viewModel)
            }
        }
    }
}

// MARK: - Search results

@Composable
private fun SearchResultsContent(
    state: LibraryUiState,
    navController: NavController,
    viewModel: LibraryViewModel,
) {
    if (state.viewMode == LibraryViewMode.LIST) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (state.instrumentMatches.isNotEmpty()) {
                item { SectionDivider("Instrument") }
                items(state.instrumentMatches, key = { it.id }) { recording ->
                    LibraryListRowComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                        modifier = Modifier.padding(horizontal = Theme.Padding),
                    )
                    RowDivider()
                }
            }
            if (state.keywordMatches.isNotEmpty()) {
                item { SectionDivider("Keywords") }
                items(state.keywordMatches, key = { it.id }) { recording ->
                    LibraryListRowComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                        modifier = Modifier.padding(horizontal = Theme.Padding),
                    )
                    RowDivider()
                }
            }
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(Theme.Padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.instrumentMatches.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionDivider("Instrument") }
                items(state.instrumentMatches, key = { it.id }) { recording ->
                    LibraryCardComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                    )
                }
            }
            if (state.keywordMatches.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionDivider("Keywords") }
                items(state.keywordMatches, key = { it.id }) { recording ->
                    LibraryCardComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                    )
                }
            }
        }
    }
}

// MARK: - Tuning group content

@Composable
private fun TuningGroupContent(
    state: LibraryUiState,
    navController: NavController,
    viewModel: LibraryViewModel,
) {
    if (state.viewMode == LibraryViewMode.LIST) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            state.tuningGroups.forEach { group ->
                item(key = "header_${group.tuning}") { SectionDivider(group.tuning) }
                items(group.recordings, key = { it.id }) { recording ->
                    LibraryListRowComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                        modifier = Modifier.padding(horizontal = Theme.Padding),
                    )
                    RowDivider()
                }
            }
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(Theme.Padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            state.tuningGroups.forEach { group ->
                item(key = "header_${group.tuning}", span = StaggeredGridItemSpan.FullLine) {
                    SectionDivider(group.tuning)
                }
                items(group.recordings, key = { it.id }) { recording ->
                    LibraryCardComposable(
                        recording = recording,
                        onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                        onDelete = { viewModel.delete(recording) },
                        onShare = { viewModel.shareRecording(recording) },
                    )
                }
            }
        }
    }
}

// MARK: - Flat content (all other sort options)

@Composable
private fun FlatContent(
    state: LibraryUiState,
    navController: NavController,
    viewModel: LibraryViewModel,
) {
    if (state.viewMode == LibraryViewMode.LIST) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.recordings, key = { it.id }) { recording ->
                LibraryListRowComposable(
                    recording = recording,
                    onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                    onDelete = { viewModel.delete(recording) },
                    onShare = { viewModel.shareRecording(recording) },
                    modifier = Modifier.padding(horizontal = Theme.Padding),
                )
                RowDivider()
            }
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(Theme.Padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.recordings, key = { it.id }) { recording ->
                LibraryCardComposable(
                    recording = recording,
                    onClick = { navController.navigate(Screen.Player.createRoute(recording.id)) },
                    onDelete = { viewModel.delete(recording) },
                    onShare = { viewModel.shareRecording(recording) },
                )
            }
        }
    }
}

// MARK: - Shared sub-composables

@Composable
private fun SectionDivider(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.Padding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = WarmBrown.copy(alpha = 0.4f),
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
            color = Cream.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = WarmBrown.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Theme.Padding),
        color = WarmBrown.copy(alpha = 0.4f),
    )
}

@Composable
private fun SearchField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = WarmBrown.copy(alpha = 0.25f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Cream.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Search tunes, players, or instruments",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = Cream.copy(alpha = 0.35f),
                        ),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Cream,
                    ),
                    cursorBrush = SolidColor(Amber),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = { onTextChange("") },
                    modifier = Modifier.size(18.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = Cream.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.Padding),
            modifier = Modifier.padding(Theme.Padding),
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Amber.copy(alpha = 0.6f),
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = "No recordings yet",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                color = Cream,
            )
            Text(
                text = "Tap Record to capture your first session",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = Cream.copy(alpha = 0.6f),
            )
        }
    }
}
