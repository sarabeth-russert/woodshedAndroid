package com.russert.woodshed.data.file

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ThumbnailService {

    fun generateThumbnail(videoFile: File, thumbnailFile: File): Boolean {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame ?: return false

            val scaled = Bitmap.createScaledBitmap(frame, 640, 360, true)
            thumbnailFile.parentFile?.mkdirs()
            FileOutputStream(thumbnailFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            frame.recycle()
            scaled.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getVideoDuration(uri: Uri, context: Context): Double? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            ms?.let { it / 1000.0 }
        } catch (_: Exception) {
            null
        }
    }
}
