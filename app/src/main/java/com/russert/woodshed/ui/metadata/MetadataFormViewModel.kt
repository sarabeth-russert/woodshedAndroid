package com.russert.woodshed.ui.metadata

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.russert.woodshed.data.db.RecordingDao
import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.data.file.FileService
import com.russert.woodshed.data.file.ThumbnailService
import com.russert.woodshed.models.InstrumentType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

data class MetadataFormUiState(
    val tuneName: String = "",
    val playerName: String = "",
    val instrument: InstrumentType = InstrumentType.BANJO,
    val tuneOrigin: String = "",
    val tuning: String = "",
    val versionInfo: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedAndDone: Boolean = false,
) {
    val isValid: Boolean get() = tuneName.isNotBlank() && playerName.isNotBlank()
}

@HiltViewModel
class MetadataFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: RecordingDao,
    private val fileService: FileService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // videoPath arrives URL-encoded from the nav route; decode it to get the real URI string.
    private val rawVideoPath: String = Uri.decode(savedStateHandle.get<String>("videoPath") ?: "")
    private val passedDuration: Double = savedStateHandle.get<String>("duration")?.toDoubleOrNull() ?: 0.0

    val videoUri: Uri get() = Uri.parse(rawVideoPath)

    private val _uiState = MutableStateFlow(MetadataFormUiState())
    val uiState = _uiState.asStateFlow()

    val previewPlayer: ExoPlayer = ExoPlayer.Builder(context).build().also { player ->
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.prepare()
    }

    fun setTuneName(v: String)     { _uiState.update { it.copy(tuneName = v) } }
    fun setPlayerName(v: String)   { _uiState.update { it.copy(playerName = v) } }
    fun setInstrument(v: InstrumentType) { _uiState.update { it.copy(instrument = v) } }
    fun setTuneOrigin(v: String)   { _uiState.update { it.copy(tuneOrigin = v) } }
    fun setTuning(v: String)       { _uiState.update { it.copy(tuning = v) } }
    fun setVersionInfo(v: String)  { _uiState.update { it.copy(versionInfo = v) } }
    fun setNotes(v: String)        { _uiState.update { it.copy(notes = v) } }
    fun addTag(tag: String)        { _uiState.update { it.copy(tags = it.tags + tag) } }
    fun removeTag(tag: String)     { _uiState.update { it.copy(tags = it.tags - tag) } }
    fun clearError()               { _uiState.update { it.copy(error = null) } }

    fun save() {
        val s = _uiState.value
        if (!s.isValid) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val permanentFile = withContext(Dispatchers.IO) {
                    val dest = fileService.newPermanentVideoFile()
                    moveVideoToPermanent(videoUri, dest)
                    dest
                }

                val actualDuration = withContext(Dispatchers.IO) {
                    if (passedDuration > 0) passedDuration
                    else ThumbnailService.getVideoDuration(Uri.fromFile(permanentFile), context) ?: 0.0
                }

                val thumbFile = fileService.newThumbnailFile(permanentFile.name)
                val thumbGenerated = withContext(Dispatchers.IO) {
                    ThumbnailService.generateThumbnail(permanentFile, thumbFile)
                }

                val recording = RecordingEntity(
                    tuneName    = s.tuneName.trim(),
                    playerName  = s.playerName.trim(),
                    instrument  = s.instrument.displayName,
                    tuneOrigin  = s.tuneOrigin.trim(),
                    tuning      = s.tuning.trim(),
                    notes       = s.notes.trim(),
                    versionInfo = s.versionInfo.trim(),
                    tags        = s.tags.joinToString(","),
                    videoFilePath  = permanentFile.name,
                    thumbnailPath  = if (thumbGenerated) fileService.thumbnailRelativePath(permanentFile.name) else null,
                    duration       = actualDuration,
                )
                dao.insert(recording)
                _uiState.update { it.copy(isSaving = false, savedAndDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun discardVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (videoUri.scheme == "file") {
                    File(videoUri.path ?: return@launch).delete()
                }
                // content:// URIs belong to the media store — don't delete them
            } catch (_: Exception) {}
        }
    }

    private fun moveVideoToPermanent(source: Uri, dest: File) {
        when (source.scheme) {
            "file" -> {
                val src = File(requireNotNull(source.path) { "null file path" })
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
            else -> {
                val stream = context.contentResolver.openInputStream(source)
                    ?: throw IOException("Cannot open video stream: $source")
                stream.use { input -> dest.outputStream().use { input.copyTo(it) } }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer.release()
    }
}
