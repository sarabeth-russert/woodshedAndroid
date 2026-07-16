package com.russert.woodshed.ui.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.russert.woodshed.ui.navigation.Screen
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MetadataFormScreen(
    navController: NavController,
    viewModel: MetadataFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.previewPlayer.pause() }
    }

    LaunchedEffect(state.savedAndDone) {
        if (state.savedAndDone) {
            navController.navigate(Screen.Library.route) {
                popUpTo(Screen.Record.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = DarkBrown,
        topBar = {
            TopAppBar(
                title = {
                    Text("New Recording", fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Cream)
                },
                navigationIcon = {
                    TextButton(onClick = {
                        viewModel.discardVideo()
                        navController.popBackStack()
                    }) {
                        Text("Discard", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Color.Red)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = state.isValid && !state.isSaving,
                    ) {
                        Text(
                            text = if (state.isSaving) "Saving…" else "Save",
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            color = if (state.isValid && !state.isSaving) Amber else Amber.copy(alpha = 0.4f),
                        )
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
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.previewPlayer
                        useController = true
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
                    .padding(Theme.Padding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FormSection {
                    FormLabel("Tune Name *")
                    FormTextField(
                        value = state.tuneName,
                        onValueChange = viewModel::setTuneName,
                        imeAction = ImeAction.Next,
                    )
                }

                FormSection {
                    FormLabel("Player Name *")
                    FormTextField(
                        value = state.playerName,
                        onValueChange = viewModel::setPlayerName,
                        imeAction = ImeAction.Next,
                    )
                }

                FormSection {
                    FormLabel("Instrument")
                    InstrumentPicker(
                        selected = state.instrument,
                        onSelect = viewModel::setInstrument,
                    )
                }

                FormSection {
                    FormLabel("Tune Origin")
                    FormTextField(
                        value = state.tuneOrigin,
                        onValueChange = viewModel::setTuneOrigin,
                        imeAction = ImeAction.Next,
                    )
                }

                FormSection {
                    FormLabel("Tuning")
                    FormTextField(
                        value = state.tuning,
                        onValueChange = viewModel::setTuning,
                        placeholder = "e.g. Open G, DADGAD, Standard",
                        imeAction = ImeAction.Next,
                    )
                }

                FormSection {
                    FormLabel("Learned From")
                    FormTextArea(
                        value = state.versionInfo,
                        onValueChange = viewModel::setVersionInfo,
                    )
                }

                FormSection {
                    FormLabel("Tags")
                    TagEditor(
                        tags = state.tags,
                        onAdd = viewModel::addTag,
                        onRemove = viewModel::removeTag,
                    )
                }

                FormSection {
                    FormLabel("Notes")
                    FormTextArea(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                    )
                }

                if (state.error != null) {
                    Text(
                        text = state.error ?: "",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color.Red.copy(alpha = 0.8f),
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
