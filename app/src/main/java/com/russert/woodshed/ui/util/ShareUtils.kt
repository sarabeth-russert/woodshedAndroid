package com.russert.woodshed.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun launchShareIntent(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "com.russert.woodshed.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Recording"))
}
