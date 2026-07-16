package com.russert.woodshed.ui.record

import android.net.Uri
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russert.woodshed.data.file.FileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingSessionViewModel @Inject constructor(
    private val fileService: FileService,
) : ViewModel() {

    data class UiState(
        val isRecording: Boolean = false,
        val elapsedSeconds: Double = 0.0,
        val error: String? = null,
        val recordedVideoUri: Uri? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var activeRecording: Recording? = null
    private var timerJob: Job? = null

    fun newTempFile(): File = fileService.newTempVideoFile()

    // Called from the composable immediately after CameraX .start() returns
    fun onRecordingStarted(recording: Recording) {
        activeRecording = recording
    }

    // Called from VideoRecordEvent.Start — recording is actually capturing frames
    fun onRecordingReady() {
        _uiState.update { it.copy(isRecording = true, elapsedSeconds = 0.0) }
        startTimer()
    }

    // Called from VideoRecordEvent.Finalize (success path)
    fun onRecordingComplete(uri: Uri) {
        stopTimer()
        _uiState.update { it.copy(isRecording = false, recordedVideoUri = uri) }
    }

    // Called from VideoRecordEvent.Finalize (error path) or camera binding failure
    fun onRecordingError(msg: String) {
        stopTimer()
        activeRecording = null
        _uiState.update { it.copy(isRecording = false, error = msg) }
    }

    fun stopRecording() {
        activeRecording?.stop()
        // State update (isRecording = false) comes from VideoRecordEvent.Finalize
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Called after navigating to MetadataForm so we don't re-trigger navigation
    fun onNavigatedToMetadata() {
        activeRecording = null
        _uiState.update { it.copy(recordedVideoUri = null, elapsedSeconds = 0.0) }
    }

    fun discardAndReset() {
        activeRecording?.stop()
        activeRecording = null
        stopTimer()
        _uiState.update { it.copy(isRecording = false, recordedVideoUri = null, elapsedSeconds = 0.0) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1.0) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        activeRecording?.stop()
    }
}
