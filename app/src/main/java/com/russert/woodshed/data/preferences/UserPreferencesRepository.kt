package com.russert.woodshed.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SORT_OPTION         = stringPreferencesKey("sort_option")
        val VIEW_MODE           = stringPreferencesKey("view_mode")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    val sortOption: Flow<LibrarySortOption> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> LibrarySortOption.fromKey(prefs[Keys.SORT_OPTION] ?: LibrarySortOption.DATE_NEWEST.key) }

    val viewMode: Flow<LibraryViewMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> LibraryViewMode.fromKey(prefs[Keys.VIEW_MODE] ?: LibraryViewMode.LIST.key) }

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.HAS_SEEN_ONBOARDING] ?: false }

    suspend fun setSortOption(option: LibrarySortOption) {
        dataStore.edit { it[Keys.SORT_OPTION] = option.key }
    }

    suspend fun setViewMode(mode: LibraryViewMode) {
        dataStore.edit { it[Keys.VIEW_MODE] = mode.key }
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = value }
    }
}
