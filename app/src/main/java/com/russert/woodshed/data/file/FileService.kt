package com.russert.woodshed.data.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // External storage is preferred for videos; internal is the fallback.
    val videosDir: File get() = context.getExternalFilesDir("Videos") ?: File(context.filesDir, "Videos")
    val thumbnailsDir: File get() = File(context.filesDir, "Thumbnails")
    val sectionStatesDir: File get() = File(context.filesDir, "SectionStates")

    fun resolveVideoFile(relativePath: String): File? {
        if (relativePath.isEmpty()) return null
        val filename = File(relativePath).name
        val external = File(videosDir, filename)
        if (external.exists()) return external
        val internal = File(File(context.filesDir, "Videos"), filename)
        if (internal.exists()) return internal
        return null
    }

    fun thumbnailFile(relativePath: String?): File? {
        if (relativePath.isNullOrEmpty()) return null
        return File(context.filesDir, relativePath)
    }

    fun sectionStateFile(recordingId: String): File {
        sectionStatesDir.mkdirs()
        return File(sectionStatesDir, "$recordingId.json")
    }

    fun newThumbnailFile(videoFileName: String): File {
        thumbnailsDir.mkdirs()
        val base = File(videoFileName).nameWithoutExtension
        return File(thumbnailsDir, "$base.jpg")
    }

    fun thumbnailRelativePath(videoFileName: String): String {
        val base = File(videoFileName).nameWithoutExtension
        return "Thumbnails/$base.jpg"
    }

    fun newTempVideoFile(): File = File(context.cacheDir, "${UUID.randomUUID()}.mp4")

    fun newPermanentVideoFile(): File {
        videosDir.mkdirs()
        return File(videosDir, "${UUID.randomUUID()}.mp4")
    }

    fun deleteRecordingFiles(id: String, videoPath: String, thumbnailPath: String?) {
        resolveVideoFile(videoPath)?.delete()
        thumbnailPath?.let { thumbnailFile(it)?.delete() }
        sectionStateFile(id).delete()
    }
}
