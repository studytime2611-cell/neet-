package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SavedLocalFile(
    val localFilePath: String,
    val fileName: String,
    val fileType: String
)

object LocalFileHelper {
    suspend fun copyUriToVault(context: Context, uri: Uri): Result<SavedLocalFile> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                var displayName = "document"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
                val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
                val safeExt = when {
                    mimeType.contains("pdf") || displayName.endsWith(".pdf", true) -> ".pdf"
                    mimeType.contains("png") || displayName.endsWith(".png", true) -> ".png"
                    mimeType.contains("webp") || displayName.endsWith(".webp", true) -> ".webp"
                    else -> ".jpg"
                }
                val destFile = File(vaultDir, "${UUID.randomUUID()}$safeExt")
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))

                Result.success(
                    SavedLocalFile(
                        localFilePath = destFile.absolutePath,
                        fileName = displayName,
                        fileType = mimeType
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun deleteLocalFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val f = File(path)
            if (f.exists()) f.delete()
        } catch (_: Exception) {
        }
    }

    fun openExternally(context: Context, path: String, mimeType: String?) {
        try {
            val file = File(path)
            if (!file.exists()) return
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
        }
    }

    fun shareExternally(context: Context, path: String, mimeType: String?, title: String) {
        try {
            val file = File(path)
            if (!file.exists()) return
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share / Open with"))
        } catch (_: Exception) {
        }
    }

    suspend fun renderPdfPages(path: String, maxWidthPx: Int = 1080): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) return@withContext emptyList()
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pages = mutableListOf<Bitmap>()
            try {
                val count = minOf(renderer.pageCount, 30)
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    val scale = maxWidthPx.toFloat() / maxOf(1, page.width)
                    val w = maxOf(1, (page.width * scale).toInt())
                    val h = maxOf(1, (page.height * scale).toInt())
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pages.add(bitmap)
                }
            } finally {
                renderer.close()
                pfd.close()
            }
            pages
        }
}
