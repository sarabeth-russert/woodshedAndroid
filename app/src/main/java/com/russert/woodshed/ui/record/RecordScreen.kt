package com.russert.woodshed.ui.record

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.russert.woodshed.ui.navigation.Screen
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecordScreen(
    navController: NavController,
    viewModel: RecordingSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity

    // -- Permission state --
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionsGranted = cameraGranted && audioGranted

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        cameraGranted = grants[Manifest.permission.CAMERA] == true
        audioGranted  = grants[Manifest.permission.RECORD_AUDIO] == true
    }

    // Request on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // -- Camera setup --
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(permissionsGranted, lensFacing) {
        if (!permissionsGranted) return@LaunchedEffect

        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        cameraProvider = provider

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        val capture = VideoCapture.withOutput(recorder)
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            videoCapture = capture
        } catch (e: Exception) {
            viewModel.onRecordingError("Could not open camera: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    // -- Keep screen on while recording --
    DisposableEffect(state.isRecording) {
        if (state.isRecording) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // -- Video import --
    var isImporting by remember { mutableStateOf(false) }
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        isImporting = false
        if (uri != null) {
            navController.navigate(
                Screen.MetadataForm.createRoute(Uri.encode(uri.toString()), 0.0),
            )
        }
    }

    // -- Navigate to MetadataForm when recording is done --
    val recordedUri = state.recordedVideoUri
    LaunchedEffect(recordedUri) {
        if (recordedUri != null) {
            navController.navigate(
                Screen.MetadataForm.createRoute(Uri.encode(recordedUri.toString()), state.elapsedSeconds),
            )
            viewModel.onNavigatedToMetadata()
        }
    }

    // -- Error alert (show as overlay; dismiss clears it) --

    // -- UI --
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionsGranted) {
            // Camera preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            // Gradient overlay for controls legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))),
                    ),
            )

            // Recording controls
            RecordingControlsComposable(
                isRecording = state.isRecording,
                elapsedSeconds = state.elapsedSeconds,
                onToggle = {
                    if (state.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        val capture = videoCapture ?: return@RecordingControlsComposable
                        val file = viewModel.newTempFile()
                        val outputOptions = FileOutputOptions.Builder(file).build()
                        val executor = ContextCompat.getMainExecutor(context)
                        val recording = capture.output
                            .prepareRecording(context, outputOptions)
                            .withAudioEnabled()
                            .start(executor) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start    -> viewModel.onRecordingReady()
                                    is VideoRecordEvent.Finalize -> {
                                        if (event.hasError()) {
                                            viewModel.onRecordingError("Recording error (${event.error})")
                                        } else {
                                            viewModel.onRecordingComplete(event.outputResults.outputUri)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        viewModel.onRecordingStarted(recording)
                    }
                },
                onFlip = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            // Permissions denied
            PermissionsDeniedContent(
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onImport = {
                    isImporting = true
                    videoPicker.launch("video/*")
                },
            )
        }

        // Import button — hidden while recording
        if (!state.isRecording) {
            Surface(
                onClick = {
                    isImporting = true
                    videoPicker.launch("video/*")
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = Theme.Padding, top = Theme.SmallPadding),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Cream,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Import",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        color = Cream,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        // Importing overlay
        if (isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(40.dp))
                    Text("Importing video…", fontSize = 14.sp, fontFamily = FontFamily.Serif, color = Cream.copy(alpha = 0.8f))
                }
            }
        }

        // Error overlay
        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(Theme.Padding * 2),
                ) {
                    Text(
                        text = state.error ?: "",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        color = Cream,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { viewModel.clearError() },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = DarkBrown),
                    ) {
                        Text("OK", fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsDeniedContent(
    onOpenSettings: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.Padding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.VideocamOff,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(60.dp),
        )

        Spacer(Modifier.height(Theme.Padding))

        Text(
            text = "Camera & Microphone Access Required",
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            color = Cream,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Theme.SmallPadding))

        Text(
            text = "Woodshed needs camera and microphone access to record your performances.",
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            color = Cream.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Theme.Padding))

        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = DarkBrown),
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Open Settings", fontFamily = FontFamily.Serif)
        }

        Spacer(Modifier.height(Theme.SmallPadding))

        Text(
            text = "or",
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            color = Cream.copy(alpha = 0.4f),
        )

        Spacer(Modifier.height(Theme.SmallPadding))

        OutlinedButton(
            onClick = onImport,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
            border = BorderStroke(1.dp, Amber),
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Import Existing Video", fontFamily = FontFamily.Serif)
        }
    }
}
