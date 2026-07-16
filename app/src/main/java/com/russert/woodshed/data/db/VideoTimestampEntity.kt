package com.russert.woodshed.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "video_timestamps",
    foreignKeys = [ForeignKey(
        entity = RecordingEntity::class,
        parentColumns = ["id"],
        childColumns = ["recordingId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recordingId")]
)
data class VideoTimestampEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recordingId: String,
    val timeOffset: Double,
    val label: String,
)
