package com.russert.woodshed.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tuneName: String = "",
    val playerName: String = "",
    val instrument: String = "Other",
    val tuneOrigin: String = "",
    val tuning: String = "",
    val notes: String = "",
    val versionInfo: String = "",
    val tags: String = "",          // comma-separated, same as iOS
    val videoFilePath: String = "",
    val thumbnailPath: String? = null,
    val duration: Double = 0.0,
    val dateRecorded: Long = System.currentTimeMillis(),
) {
    val tagList: List<String>
        get() = tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
