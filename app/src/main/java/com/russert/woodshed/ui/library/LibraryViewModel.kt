package com.russert.woodshed.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russert.woodshed.data.db.RecordingDao
import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.data.file.FileService
import com.russert.woodshed.data.file.WoodshedFileService
import com.russert.woodshed.data.preferences.LibrarySortOption
import com.russert.woodshed.data.preferences.LibraryViewMode
import com.russert.woodshed.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dao: RecordingDao,
    private val prefs: UserPreferencesRepository,
    private val fileService: FileService,
    private val woodshedFileService: WoodshedFileService,
) : ViewModel() {

    private val _shareEvent = MutableSharedFlow<File>(extraBufferCapacity = 1)
    val shareEvent = _shareEvent.asSharedFlow()

    private val _searchText = MutableStateFlow("")

    val uiState = combine(
        dao.observeAll(),
        _searchText.debounce(300),
        prefs.sortOption,
        prefs.viewMode,
    ) { all, search, sort, viewMode ->
        computeState(all, search, sort, viewMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val next = if (uiState.value.viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID
                       else LibraryViewMode.LIST
            prefs.setViewMode(next)
        }
    }

    fun shareRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            try {
                val file = woodshedFileService.export(recording)
                _shareEvent.emit(file)
            } catch (_: Exception) {}
        }
    }

    fun delete(recording: RecordingEntity) {
        viewModelScope.launch {
            dao.delete(recording)
            fileService.deleteRecordingFiles(recording.id, recording.videoFilePath, recording.thumbnailPath)
        }
    }

    // MARK: - State computation

    private fun computeState(
        all: List<RecordingEntity>,
        searchText: String,
        sort: LibrarySortOption,
        viewMode: LibraryViewMode,
    ): LibraryUiState {
        if (searchText.isNotEmpty()) {
            val text = searchText.lowercase()
            val instrumentMatches = all.filter { it.instrument.lowercase().contains(text) }
            val instrumentIds = instrumentMatches.map { it.id }.toSet()
            val keywordMatches = all.filter { r ->
                r.id !in instrumentIds &&
                    (r.tuneName.lowercase().contains(text) ||
                     r.playerName.lowercase().contains(text) ||
                     r.tags.lowercase().contains(text) ||
                     r.versionInfo.lowercase().contains(text) ||
                     r.tuning.lowercase().contains(text))
            }
            return LibraryUiState(
                recordings = instrumentMatches + keywordMatches,
                instrumentMatches = instrumentMatches,
                keywordMatches = keywordMatches,
                searchText = searchText,
                viewMode = viewMode,
                sortOption = sort,
            )
        }

        val sorted = applySortOrder(all, sort)

        return if (sort == LibrarySortOption.TUNING) {
            val byTuning = sorted.groupBy { it.tuning.ifEmpty { null } }
            val named = byTuning.entries
                .filter { it.key != null }
                .sortedBy { it.key!!.lowercase() }
                .map { TuningGroup(it.key!!, it.value) }
            val other = byTuning[null]?.let { listOf(TuningGroup("Other", it)) } ?: emptyList()
            LibraryUiState(
                recordings = sorted,
                tuningGroups = named + other,
                viewMode = viewMode,
                sortOption = sort,
            )
        } else {
            LibraryUiState(
                recordings = sorted,
                viewMode = viewMode,
                sortOption = sort,
            )
        }
    }

    private fun applySortOrder(recordings: List<RecordingEntity>, sort: LibrarySortOption) =
        when (sort) {
            LibrarySortOption.DATE_NEWEST  -> recordings.sortedByDescending { it.dateRecorded }
            LibrarySortOption.DATE_OLDEST  -> recordings.sortedBy { it.dateRecorded }
            LibrarySortOption.TUNE_NAME    -> recordings.sortedBy { it.tuneName.lowercase() }
            LibrarySortOption.PLAYER_NAME  -> recordings.sortedBy { it.playerName.lowercase() }
            LibrarySortOption.TUNING       -> recordings.sortedBy { it.tuneName.lowercase() }
        }
}
