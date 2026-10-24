package com.github.livingwithhippos.nullseek.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

fun saveFileToPublicDownloads(
    context: Context,
    fileName: String,
    bytes: ByteArray,
    mimeType: String = "application/x-bittorrent",
): Uri? =
    try {
        val sanitizedName =
            if (fileName.endsWith(".torrent", ignoreCase = true)) fileName else "$fileName.torrent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            val uri =
                context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues,
                )
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, sanitizedName)
            targetFile.writeBytes(bytes)
            Uri.fromFile(targetFile)
        }
    } catch (_: Exception) {
        null
    }
