package com.russert.woodshed.data.preferences

enum class LibrarySortOption(val displayName: String, val key: String) {
    DATE_NEWEST("Date (Newest)", "date_newest"),
    DATE_OLDEST("Date (Oldest)", "date_oldest"),
    TUNE_NAME("Tune Name", "tune_name"),
    PLAYER_NAME("Player Name", "player_name"),
    TUNING("Tuning", "tuning");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DATE_NEWEST
    }
}

enum class LibraryViewMode(val key: String) {
    LIST("list"),
    GRID("grid");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: LIST
    }
}
