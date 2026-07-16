package com.russert.woodshed.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingEntity::class, VideoTimestampEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WoodshedDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
