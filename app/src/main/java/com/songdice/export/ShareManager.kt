package com.songdice.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Share & Export Helper for managing Android system Share Sheet (Intent.ACTION_SEND)
 * with FileProvider URI generation for .zip export bundle sharing.
 */
class ShareManager {

    /**
     * Writes raw .zip bytes to a cached file in context.cacheDir and launches the system Share Sheet.
     */
    fun shareZipBundle(context: Context, fileName: String, zipBytes: ByteArray): Intent {
        val file = saveZipToCache(context, fileName, zipBytes)
        val shareIntent = createShareIntent(context, file)
        val chooserIntent = Intent.createChooser(shareIntent, "Share Song Dice Bundle").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
        return chooserIntent
    }

    /**
     * Saves raw .zip bytes to context.cacheDir/exports directory.
     */
    fun saveZipToCache(context: Context, fileName: String, zipBytes: ByteArray): File {
        val exportsDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val file = File(exportsDir, fileName)
        file.writeBytes(zipBytes)
        return file
    }

    /**
     * Constructs the Intent.ACTION_SEND with FileProvider URI and grant permissions.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Song Dice Bundle Export: ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is my Song Dice generated arrangement bundle (.zip).")
            clipData = ClipData.newRawUri(file.name, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
