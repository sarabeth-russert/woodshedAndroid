package com.russert.woodshed.ui.metadata

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russert.woodshed.data.db.RecordingDao
import com.russert.woodshed.models.InstrumentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditRecordingUiState(
    val tuneName: String = "",
    val playerName: String = "",
    val instrument: InstrumentType = InstrumentType.BANJO,
    val tuneOrigin: String = "",
    val tuning: String = "",
    val versionInfo: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() = tuneName.isNotBlank() && playerName.isNotBlank()
}

@HiltViewModel
class EditRecordingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: RecordingDao,
) : ViewModel() {

    private val recordingId: String = checkNotNull(savedStateHandle["recordingId"])

    private val _uiState = MutableStateFlow(EditRecordingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val recording = dao.getById(recordingId) ?: return@launch
            _uiState.update {
                it.copy(
                    tuneName    = recording.tuneName,
                    playerName  = recording.playerName,
                    instrument  = InstrumentType.fromDisplayName(recording.instrument),
                    tuneOrigin  = recording.tuneOrigin,
                    tuning      = recording.tuning,
                    versionInfo = recording.versionInfo,
                    tags        = recording.tagList,
                    notes       = recording.notes,
                    isLoading   = false,
                )
            }
        }
    }

    fun setTuneName(v: String)          { _uiState.update { it.copy(tuneName = v) } }
    fun setPlayerName(v: String)        { _uiState.update { it.copy(playerName = v) } }
    fun setInstrument(v: InstrumentType){ _uiState.update { it.copy(instrument = v) } }
    fun setTuneOrigin(v: String)        { _uiState.update { it.copy(tuneOrigin = v) } }
    fun setTuning(v: String)            { _uiState.update { it.copy(tuning = v) } }
    fun setVersionInfo(v: String)       { _uiState.update { it.copy(versionInfo = v) } }
    fun setNotes(v: String)             { _uiState.update { it.copy(notes = v) } }
    fun addTag(tag: String)             { _uiState.update { it.copy(tags = it.tags + tag) } }
    fun removeTag(tag: String)          { _uiState.update { it.copy(tags = it.tags - tag) } }

    fun save() {
        val s = _uiState.value
        if (!s.isValid) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val existing = dao.getById(recordingId) ?: return@launch
                dao.update(
                    existing.copy(
                        tuneName    = s.tuneName.trim(),
                        playerName  = s.playerName.trim(),
                        instrument  = s.instrument.displayName,
                        tuneOrigin  = s.tuneOrigin.trim(),
                        tuning      = s.tuning.trim(),
                        versionInfo = s.versionInfo.trim(),
                        tags        = s.tags.joinToString(","),
                        notes       = s.notes.trim(),
                    )
                )
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }
}
