package com.russert.woodshed.ui.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.russert.woodshed.data.db.RecordingDao
import com.russert.woodshed.data.db.VideoTimestampEntity
import com.russert.woodshed.data.file.FileService
import com.russert.woodshed.data.file.WoodshedFileService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: RecordingDao,
    private val fileService: FileService,
    private val woodshedFileService: WoodshedFileService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _shareEvent = MutableSharedFlow<File>(extraBufferCapacity = 1)
    val shareEvent = _shareEvent.asSharedFlow()

    private val recordingId: String = savedStateHandle["recordingId"] ?: ""

    private val _state = MutableStateFlow(PlayerUiState())
    val uiState = _state.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .build()

    private var pollJob: Job? = null
    private var loopSaveJob: Job? = null
    private val quickLoopPrefs = context.getSharedPreferences("woodshed_quick_loops", Context.MODE_PRIVATE)

    init {
        setupPlayerListener()

        viewModelScope.launch {
            val recording = dao.getById(recordingId) ?: return@launch
            _state.update { it.copy(recording = recording, duration = recording.duration.coerceAtLeast(1.0)) }

            // Observe timestamps reactively
            launch {
                dao.observeTimestamps(recordingId).collect { ts ->
                    _state.update { it.copy(timestamps = ts) }
                }
            }

            // Load video
            val videoFile = fileService.resolveVideoFile(recording.videoFilePath)
            if (videoFile != null) {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                player.setMediaItem(mediaItem)
                player.prepare()
            }

            loadLoopState()
            loadQuickLoopState()
        }
    }

    // MARK: - Playback controls

    fun play() { player.play() }
    fun pause() { player.pause() }

    fun togglePlayback() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seek(to: Double) {
        player.seekTo((to * 1000).toLong())
        _state.update { it.copy(currentTime = to) }
    }

    fun skipBackward(seconds: Double = 5.0) {
        val newTime = (_state.value.currentTime - seconds).coerceAtLeast(0.0)
        seek(newTime)
    }

    fun skipForward(seconds: Double = 5.0) {
        val newTime = (_state.value.currentTime + seconds).coerceAtMost(_state.value.duration)
        seek(newTime)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun setScrubbing(active: Boolean) {
        _state.update { it.copy(isScrubbing = active) }
    }

    // MARK: - Section loop

    fun addSplitPoint() {
        val s = _state.value
        if (!s.sectionsEnabled) {
            _state.update { it.copy(sectionsEnabled = true) }
            scheduleSaveLoopState()
            return
        }
        if (s.splitPoints.size >= 7) return
        val newPoints = (s.splitPoints + s.currentTime).sorted()
        _state.update { it.copy(splitPoints = newPoints) }
        scheduleSaveLoopState()
    }

    fun setSectionStart(to: Double) {
        val s = _state.value
        val hi = s.splitPoints.firstOrNull()?.minus(1.0) ?: (s.duration - 1.0)
        _state.update { it.copy(sectionStart = to.coerceIn(0.0, hi)) }
        scheduleSaveLoopState()
    }

    fun setSplitPoint(at: Int, to: Double) {
        val s = _state.value
        if (at >= s.splitPoints.size) return
        val lo = if (at > 0) s.splitPoints[at - 1] + 1.0 else s.sectionStart + 1.0
        val hi = if (at < s.splitPoints.size - 1) s.splitPoints[at + 1] - 1.0 else s.duration - 1.0
        val updated = s.splitPoints.toMutableList()
        updated[at] = to.coerceIn(lo, hi)
        _state.update { it.copy(splitPoints = updated) }
        scheduleSaveLoopState()
    }

    fun moveSectionBoundary(at: Int) {
        val s = _state.value
        if (at == 0) {
            val cap = s.splitPoints.firstOrNull() ?: s.duration
            _state.update { it.copy(sectionStart = s.currentTime.coerceIn(0.0, cap)) }
        } else {
            val splitIdx = at - 1
            if (splitIdx >= s.splitPoints.size) return
            val updated = s.splitPoints.toMutableList()
            updated[splitIdx] = s.currentTime
            _state.update { it.copy(splitPoints = updated.sorted()) }
        }
        scheduleSaveLoopState()
    }

    fun deleteSection(at: Int) {
        val s = _state.value
        if (!s.sectionsEnabled) return
        if (s.splitPoints.isEmpty()) {
            _state.update { it.copy(sectionsEnabled = false, activeSection = null, loopEnabled = false) }
            scheduleSaveLoopState()
            return
        }
        val splitIdx = (at - 1).coerceAtLeast(0)
        if (splitIdx >= s.splitPoints.size) return
        val updated = s.splitPoints.toMutableList().also { it.removeAt(splitIdx) }
        val newCount = updated.size + 1
        val newActive = s.activeSection?.let { if (it >= newCount) newCount - 1 else it }
        _state.update { it.copy(splitPoints = updated, activeSection = newActive) }
        scheduleSaveLoopState()
    }

    fun setActiveSection(index: Int) {
        val s = _state.value
        if (s.activeSection == index) {
            _state.update { it.copy(activeSection = null, loopEnabled = false) }
        } else {
            _state.update { it.copy(isQuickLoopActive = false, activeSection = index, loopEnabled = true) }
            seek(_state.value.loopStart)
        }
        scheduleSaveLoopState()
    }

    fun toggleLoop() {
        val s = _state.value
        if (!s.sectionsEnabled) return
        val newEnabled = !s.loopEnabled
        if (newEnabled && s.isQuickLoopActive) {
            _state.update { it.copy(isQuickLoopActive = false) }
        }
        val newActive = if (newEnabled && s.activeSection == null) 0 else if (!newEnabled) null else s.activeSection
        _state.update { it.copy(loopEnabled = newEnabled, activeSection = newActive) }
        scheduleSaveLoopState()
    }

    fun clearLoop() {
        _state.update {
            it.copy(
                sectionsEnabled = false,
                sectionStart = 0.0,
                splitPoints = emptyList(),
                activeSection = null,
                loopEnabled = false,
            )
        }
        scheduleSaveLoopState()
    }

    // MARK: - Quick loop

    fun toggleQuickLoop() {
        if (_state.value.isQuickLoopActive) deactivateQuickLoop() else activateQuickLoop()
    }

    private fun activateQuickLoop() {
        val s = _state.value
        val inPoint = s.duration * 0.35
        val outPoint = s.duration * 0.65
        _state.update {
            it.copy(
                activeSection = null,
                loopEnabled = false,
                isQuickLoopActive = true,
                quickLoopIn = inPoint,
                quickLoopOut = outPoint,
            )
        }
        seek(inPoint)
        saveQuickLoopState()
    }

    fun deactivateQuickLoop() {
        _state.update { it.copy(isQuickLoopActive = false) }
    }

    fun setQuickLoopIn(time: Double) {
        val s = _state.value
        val clamped = time.coerceIn(0.0, s.quickLoopOut - 0.5)
        _state.update { it.copy(quickLoopIn = clamped) }
        saveQuickLoopState()
    }

    fun setQuickLoopOut(time: Double) {
        val s = _state.value
        val clamped = time.coerceIn(s.quickLoopIn + 0.5, s.duration)
        _state.update { it.copy(quickLoopOut = clamped) }
        saveQuickLoopState()
    }

    // MARK: - Recording management

    fun shareRecording() {
        viewModelScope.launch {
            try {
                val recording = _state.value.recording ?: return@launch
                val file = woodshedFileService.export(recording)
                _shareEvent.emit(file)
            } catch (_: Exception) {}
        }
    }

    fun deleteRecording() {
        viewModelScope.launch {
            val recording = _state.value.recording ?: return@launch
            dao.delete(recording)
            fileService.deleteRecordingFiles(recording.id, recording.videoFilePath, recording.thumbnailPath)
        }
    }

    // MARK: - Timestamps

    fun addTimestamp(label: String) {
        viewModelScope.launch {
            val ts = VideoTimestampEntity(
                recordingId = recordingId,
                timeOffset = _state.value.currentTime,
                label = label,
            )
            dao.insertTimestamp(ts)
        }
    }

    fun deleteTimestamp(timestamp: VideoTimestampEntity) {
        viewModelScope.launch { dao.deleteTimestamp(timestamp) }
    }

    fun seekToTimestamp(timestamp: VideoTimestampEntity) {
        _state.update { it.copy(activeSection = null, loopEnabled = false, isQuickLoopActive = false) }
        seek(timestamp.timeOffset)
    }

    // MARK: - Player listener

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPolling() else pollJob?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val durationMs = player.duration
                    if (durationMs > 0) {
                        _state.update { it.copy(duration = durationMs / 1000.0) }
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    val s = _state.value
                    when {
                        s.isQuickLoopActive -> { seek(s.quickLoopIn); player.play() }
                        s.loopEnabled       -> { seek(s.loopStart); player.play() }
                        else                -> { seek(0.0); _state.update { it.copy(isPlaying = false) } }
                    }
                }
            }
        })
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val s = _state.value
                if (!s.isScrubbing) {
                    val currentMs = player.currentPosition
                    val durationMs = player.duration.takeIf { it > 0 } ?: (s.duration * 1000).toLong()
                    val currentTime = currentMs / 1000.0
                    _state.update { it.copy(currentTime = currentTime, duration = durationMs / 1000.0) }
                    enforceLoopBoundary(currentTime)
                }
                delay(50)
            }
        }
    }

    private fun enforceLoopBoundary(currentTime: Double) {
        val s = _state.value
        when {
            s.isQuickLoopActive && currentTime >= s.quickLoopOut ->
                player.seekTo((s.quickLoopIn * 1000).toLong())

            s.loopEnabled && s.loopEnd > s.loopStart && currentTime >= s.loopEnd ->
                player.seekTo((s.loopStart * 1000).toLong())
        }
    }

    // MARK: - Loop state persistence

    private fun scheduleSaveLoopState() {
        loopSaveJob?.cancel()
        loopSaveJob = viewModelScope.launch {
            delay(500)
            saveLoopState()
        }
    }

    private fun saveLoopState() {
        val s = _state.value
        val json = JSONObject().apply {
            put("sectionsEnabled", s.sectionsEnabled)
            put("sectionStart", s.sectionStart)
            put("splitPoints", JSONArray(s.splitPoints))
            put("activeSection", s.activeSection ?: JSONObject.NULL)
            put("loopEnabled", s.loopEnabled)
        }
        try {
            fileService.sectionStateFile(recordingId).writeText(json.toString())
        } catch (_: Exception) {}
    }

    private fun loadLoopState() {
        val file = fileService.sectionStateFile(recordingId)
        if (!file.exists()) return
        try {
            val json = JSONObject(file.readText())
            val splits = json.getJSONArray("splitPoints")
            val splitList = (0 until splits.length()).map { splits.getDouble(it) }
            val activeSection = if (json.isNull("activeSection")) null else json.getInt("activeSection")
            _state.update {
                it.copy(
                    sectionsEnabled = json.getBoolean("sectionsEnabled"),
                    sectionStart = json.getDouble("sectionStart"),
                    splitPoints = splitList,
                    activeSection = activeSection,
                    loopEnabled = json.getBoolean("loopEnabled"),
                )
            }
        } catch (_: Exception) {}
    }

    // MARK: - Quick loop persistence (device-local SharedPreferences)

    private fun saveQuickLoopState() {
        val s = _state.value
        quickLoopPrefs.edit()
            .putFloat("ql_in_$recordingId", s.quickLoopIn.toFloat())
            .putFloat("ql_out_$recordingId", s.quickLoopOut.toFloat())
            .apply()
    }

    private fun loadQuickLoopState() {
        val inPoint = quickLoopPrefs.getFloat("ql_in_$recordingId", 0f).toDouble()
        val outPoint = quickLoopPrefs.getFloat("ql_out_$recordingId", 0f).toDouble()
        _state.update { it.copy(quickLoopIn = inPoint, quickLoopOut = outPoint) }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        loopSaveJob?.cancel()
        player.release()
    }
}
