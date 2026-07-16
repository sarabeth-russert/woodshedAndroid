package com.russert.woodshed.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russert.woodshed.data.preferences.LibrarySortOption
import com.russert.woodshed.data.preferences.LibraryViewMode
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val storageBytes by viewModel.storageBytes.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete All Recordings?", fontFamily = FontFamily.Serif, color = Cream)
            },
            text = {
                Text(
                    "This will permanently delete all recordings and cannot be undone.",
                    fontFamily = FontFamily.Serif,
                    color = Cream.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllRecordings()
                    showDeleteDialog = false
                }) {
                    Text("Delete All", color = Color.Red, fontFamily = FontFamily.Serif)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Cream.copy(alpha = 0.7f), fontFamily = FontFamily.Serif)
                }
            },
            containerColor = WarmBrown,
        )
    }

    Scaffold(
        containerColor = DarkBrown,
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Cream)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBrown),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Theme.Padding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Library section
            SettingsSection(header = "Library") {
                SortPickerRow(
                    selected = sortOption,
                    onSelect = viewModel::setSortOption,
                )
                HorizontalDivider(color = WarmBrown.copy(alpha = 0.4f))
                ViewModeRow(
                    selected = viewMode,
                    onSelect = viewModel::setViewMode,
                )
            }

            // Storage section
            SettingsSection(header = "Storage") {
                StorageRow(bytes = storageBytes)
            }

            // Data section
            SettingsSection(header = "Data") {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        "Delete All Recordings",
                        color = Color.Red,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                    )
                }
            }

            // About section
            SettingsSection(header = "About") {
                AboutRow(label = "Version", value = viewModel.appVersion)
                HorizontalDivider(color = WarmBrown.copy(alpha = 0.4f))
                AboutRow(label = "Build", value = viewModel.buildNumber)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    header: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = header.uppercase(),
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            color = Cream.copy(alpha = 0.5f),
            letterSpacing = 0.8.sp,
        )
        Surface(
            color = WarmBrown.copy(alpha = 0.2f),
            shape = RoundedCornerShape(Theme.CornerRadius),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortPickerRow(
    selected: LibrarySortOption,
    onSelect: (LibrarySortOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Sort By",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = Cream,
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selected.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = WarmBrown.copy(alpha = 0.5f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    focusedContainerColor = WarmBrown.copy(alpha = 0.25f),
                    unfocusedContainerColor = WarmBrown.copy(alpha = 0.25f),
                    focusedTrailingIconColor = Amber,
                    unfocusedTrailingIconColor = Cream.copy(alpha = 0.6f),
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                ),
                singleLine = true,
                shape = RoundedCornerShape(Theme.SmallCornerRadius),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(0.55f),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = WarmBrown,
            ) {
                LibrarySortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.displayName,
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                color = if (option == selected) Amber else Cream,
                            )
                        },
                        onClick = { onSelect(option); expanded = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeRow(
    selected: LibraryViewMode,
    onSelect: (LibraryViewMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Default View",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = Cream,
        )

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = selected == LibraryViewMode.LIST,
                onClick = { onSelect(LibraryViewMode.LIST) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Amber,
                    activeContentColor = DarkBrown,
                    inactiveContainerColor = WarmBrown.copy(alpha = 0.3f),
                    inactiveContentColor = Cream,
                    activeBorderColor = Amber,
                    inactiveBorderColor = WarmBrown.copy(alpha = 0.5f),
                ),
                icon = {},
            ) {
                Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("List", fontFamily = FontFamily.Serif, fontSize = 13.sp)
            }

            SegmentedButton(
                selected = selected == LibraryViewMode.GRID,
                onClick = { onSelect(LibraryViewMode.GRID) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Amber,
                    activeContentColor = DarkBrown,
                    inactiveContainerColor = WarmBrown.copy(alpha = 0.3f),
                    inactiveContentColor = Cream,
                    activeBorderColor = Amber,
                    inactiveBorderColor = WarmBrown.copy(alpha = 0.5f),
                ),
                icon = {},
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Grid", fontFamily = FontFamily.Serif, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StorageRow(bytes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = Cream.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Videos stored locally",
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = Cream,
            )
        }

        Text(
            text = when {
                bytes < 0  -> "…"
                bytes == 0L -> "0 B"
                else -> android.text.format.Formatter.formatShortFileSize(
                    LocalContext.current,
                    bytes,
                )
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Cream.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream)
        Text(value, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream.copy(alpha = 0.5f))
    }
}
