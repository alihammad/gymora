package com.gymora.data.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.gymora.account.AccountFiles
import com.gymora.account.ActiveAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Demo images and GIFs attached to exercises. Files live in the active account's
 * [DIRECTORY]; exercises store only the file name, so backups restore them on any device.
 */
@Singleton
class ExerciseMediaStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val directory: File get() = directory(context)

    /** Copies the picked image into app storage and returns its new file name. */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        require(size <= MAX_BYTES) { "That image is too large (max ${MAX_BYTES / ONE_MB} MB)." }
        val extension = resolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: "img"
        val name = "${UUID.randomUUID()}.$extension"
        directory.mkdirs()
        resolver.openInputStream(uri)?.use { input ->
            File(directory, name).outputStream().use { input.copyTo(it) }
        } ?: error("Could not read the selected image")
        name
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        File(directory, name).delete()
    }

    companion object {
        const val DIRECTORY = "exercise_media"
        private const val ONE_MB = 1024L * 1024L
        private const val MAX_BYTES = 20 * ONE_MB

        /** The active account's media folder; see AccountFiles. */
        fun directory(context: Context): File = AccountFiles.mediaDir(context, ActiveAccount.id(context))

        fun file(context: Context, name: String): File = File(directory(context), name)
    }
}
