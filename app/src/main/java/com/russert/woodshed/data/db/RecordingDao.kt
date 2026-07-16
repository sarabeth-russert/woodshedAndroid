package com.russert.woodshed.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY dateRecorded DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeById(id: String): Flow<RecordingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity)

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()

    // Timestamps

    @Query("SELECT * FROM video_timestamps WHERE recordingId = :recordingId ORDER BY timeOffset ASC")
    fun observeTimestamps(recordingId: String): Flow<List<VideoTimestampEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamp(timestamp: VideoTimestampEntity)

    @Delete
    suspend fun deleteTimestamp(timestamp: VideoTimestampEntity)

    @Query("DELETE FROM video_timestamps WHERE recordingId = :recordingId")
    suspend fun deleteTimestampsForRecording(recordingId: String)
}
