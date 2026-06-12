package com.ideasinc.followthrough.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local-only storage for a check-in's cue image. A picked gallery photo is copied
 * into app-internal storage (`filesDir/cues/<checkInId>.jpg`) — it is never
 * uploaded and leaves no content-URI dependency on the gallery. The notification
 * builder loads the bitmap back from that path.
 */
object CueImageStore {

    private fun cuesDir(context: Context): File =
        File(context.filesDir, "cues").apply { if (!exists()) mkdirs() }

    private fun fileFor(context: Context, checkInId: String): File =
        File(cuesDir(context), "$checkInId.jpg")

    /**
     * Copies the photo at [uri] into app-internal storage for [checkInId], returning
     * the absolute file path, or null if the copy failed. Runs off the main thread.
     */
    suspend fun copyFromUri(context: Context, checkInId: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dest = fileFor(context, checkInId)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                dest.absolutePath
            }.getOrNull()
        }

    /** Removes the cue image at [path], if any. Use this with a plan's stored path
     *  so migrated images (stored under the old goal-based name) are also removed. */
    fun deletePath(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /**
     * Decodes the cue bitmap at [path], down-sampled so its largest side is at most
     * [maxDim] px (notifications reject oversized bitmaps). Null if it can't load.
     */
    fun loadBitmap(path: String?, maxDim: Int = 1024): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > maxDim) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }

    fun uriOrNull(value: String?): Uri? =
        value?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
}
