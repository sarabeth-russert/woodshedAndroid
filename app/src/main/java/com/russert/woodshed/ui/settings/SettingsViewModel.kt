package com.russert.woodshed.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russert.woodshed.data.db.RecordingDao
import com.russert.woodshed.data.file.FileService
import com.russert.woodshed.data.preferences.LibrarySortOption
import com.russert.woodshed.data.preferences.LibraryViewMode
import com.russert.woodshed.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val dao: RecordingDao,
    private val fileService: FileService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val sortOption = prefs.sortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibrarySortOption.DATE_NEWEST)

    val viewMode = prefs.viewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryViewMode.LIST)

    private val _storageBytes = MutableStateFlow(-1L)
    val storageBytes = _storageBytes.asStateFlow()

    init {
        refreshStorageSize()
    }

    fun setSortOption(option: LibrarySortOption) {
        viewModelScope.launch { prefs.setSortOption(option) }
    }

    fun setViewMode(mode: LibraryViewMode) {
        viewModelScope.launch { prefs.setViewMode(mode) }
    }

    fun deleteAllRecordings() {
        viewModelScope.launch {
            dao.deleteAll()
            withContext(Dispatchers.IO) {
                fileService.videosDir.listFiles()?.forEach { it.delete() }
                fileService.thumbnailsDir.listFiles()?.forEach { it.delete() }
            }
            _storageBytes.value = 0L
        }
    }

    val appVersion: String get() = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    } catch (_: Exception) { "—" }

    val buildNumber: String get() = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString()
    } catch (_: Exception) { "—" }

    private fun refreshStorageSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = fileService.videosDir
            _storageBytes.value = if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        }
    }
}
