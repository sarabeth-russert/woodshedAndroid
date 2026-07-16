package com.russert.woodshed.data.file

import android.content.Context
import com.russert.woodshed.data.db.RecordingEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WoodshedFileService @Inject constructor(
    private val fileService: FileService,
    @ApplicationContext private val context: Context,
) {
    private val magic = "WOODSHED".toByteArray(Charsets.UTF_8)
    private val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun export(recording: RecordingEntity): File = withContext(Dispatchers.IO) {
        val videoFile = fileService.resolveVideoFile(recording.videoFilePath)
            ?: error("Video file not found for recording ${recording.id}")

        val json = JSONObject().apply {
            put("tuneName",     recording.tuneName)
            put("playerName",   recording.playerName)
            put("instrument",   recording.instrument)
            put("tuneOrigin",   recording.tuneOrigin)
            put("notes",        recording.notes)
            put("duration",     recording.duration)
            put("dateRecorded", iso8601.format(Date(recording.dateRecorded)))
            put("versionInfo",  recording.versionInfo)
            put("tags",         JSONArray(recording.tagList))
            put("tuning",       recording.tuning)
        }
        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)

        // 4-byte little-endian JSON length
        val lenBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(jsonBytes.size)
            .array()

        val safeName = "${recording.tuneName}-${recording.playerName}"
            .replace("/", "-")
            .replace(":", "-")
            .take(120)
        val outFile = File(context.cacheDir, "$safeName.woodshed")

        FileOutputStream(outFile).use { out ->
            out.write(magic)
            out.write(lenBytes)
            out.write(jsonBytes)
            videoFile.inputStream().use { it.copyTo(out) }
        }

        outFile
    }
}
