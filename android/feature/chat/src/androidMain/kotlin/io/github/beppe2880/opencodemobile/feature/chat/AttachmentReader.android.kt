package io.github.beppe2880.opencodemobile.feature.chat

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max

@Composable
internal actual fun rememberAttachmentPicker(onRead: (List<PromptAttachment?>) -> Unit): AttachmentPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnRead by rememberUpdatedState(onRead)
    val readAttachments: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                currentOnRead(withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(context.contentResolver, it) } })
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(AttachmentLimits.MAX_PICKED),
        readAttachments,
    )
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), readAttachments)
    return object : AttachmentPicker {
        override fun pickPhotos() = photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        override fun pickFiles() = filePicker.launch(arrayOf("*/*"))
    }
}

/**
 * Turns files picked from the system pickers into `data:` URI attachments, which the
 * server accepts inline. Large photos are downscaled; other files are capped in size.
 * Blocking: call off the main thread.
 */
internal object AttachmentReader {
    private const val MAX_BYTES = AttachmentLimits.MAX_BYTES
    private const val MAX_IMAGE_SIDE = AttachmentLimits.MAX_IMAGE_SIDE
    private const val KEEP_IMAGE_BYTES = AttachmentLimits.KEEP_IMAGE_BYTES
    private const val JPEG_QUALITY = AttachmentLimits.JPEG_QUALITY

    /** Null when the file cannot be read or is too large. */
    fun read(resolver: ContentResolver, uri: Uri): PromptAttachment? = try {
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = displayName(resolver, uri) ?: uri.lastPathSegment ?: "file"
        val original = readBounded(resolver, uri) ?: return null
        val (bytes, type, fileName) = if (mime.startsWith("image/") && mime != "image/gif" && needsDownscale(original)) {
            Triple(downscale(resolver, uri, original), "image/jpeg", name.substringBeforeLast('.') + ".jpg")
        } else {
            Triple(original, mime, name)
        }
        PromptAttachment(
            uri = "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP),
            name = fileName,
            mime = type,
        )
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    /** The file content, or null beyond [MAX_BYTES]. */
    private fun readBounded(resolver: ContentResolver, uri: Uri): ByteArray? =
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_BYTES) return null
            }
            out.toByteArray()
        }

    private fun needsDownscale(bytes: ByteArray): Boolean {
        if (bytes.size > KEEP_IMAGE_BYTES) return true
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return max(bounds.outWidth, bounds.outHeight) > MAX_IMAGE_SIDE
    }

    private fun downscale(resolver: ContentResolver, uri: Uri, bytes: ByteArray): ByteArray {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies the EXIF rotation of camera photos.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val side = max(info.size.width, info.size.height)
                if (side > MAX_IMAGE_SIDE) {
                    val scale = MAX_IMAGE_SIDE.toFloat() / side
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: throw IOException("Unsupported image")
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}

/**
 * Reads an image another app shared, as the photo picker's are read (downscaled, at most
 * [AttachmentLimits.MAX_BYTES]). The sender is untrusted: the provider must call it an image,
 * whatever the share said. Null otherwise or when it cannot be read. Blocking.
 */
fun readSharedImage(resolver: ContentResolver, uri: Uri): PromptAttachment? {
    // Whatever the other app's provider throws, the share only loses this image.
    return try {
        val type = resolver.getType(uri)
        if (type == null || !type.lowercase().startsWith("image/")) null else AttachmentReader.read(resolver, uri)
    } catch (_: RuntimeException) {
        null
    }
}
