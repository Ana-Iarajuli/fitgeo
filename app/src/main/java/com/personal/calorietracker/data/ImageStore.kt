package com.personal.calorietracker.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Wishlist photos are copied into private app storage so they survive gallery clean-ups. */
object ImageStore {
    private fun dir(context: Context): File =
        File(context.filesDir, "wishlist").apply { mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(dir(context), "photo_${UUID.randomUUID()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a picked image (gallery / document) into app storage. Call off the main thread. */
    fun copyFromUri(context: Context, uri: Uri): File? = runCatching {
        val target = newPhotoFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }
}
