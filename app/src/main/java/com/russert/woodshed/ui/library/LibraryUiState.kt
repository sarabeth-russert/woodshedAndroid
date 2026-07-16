package com.russert.woodshed.ui.library

import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.data.preferences.LibrarySortOption
import com.russert.woodshed.data.preferences.LibraryViewMode

data class TuningGroup(val tuning: String, val recordings: List<RecordingEntity>)

data class LibraryUiState(
    val recordings: List<RecordingEntity> = emptyList(),
    val instrumentMatches: List<RecordingEntity> = emptyList(),
    val keywordMatches: List<RecordingEntity> = emptyList(),
    val tuningGroups: List<TuningGroup> = emptyList(),
    val searchText: String = "",
    val viewMode: LibraryViewMode = LibraryViewMode.LIST,
    val sortOption: LibrarySortOption = LibrarySortOption.DATE_NEWEST,
) {
    val isSearching: Boolean get() = searchText.isNotEmpty()
}
