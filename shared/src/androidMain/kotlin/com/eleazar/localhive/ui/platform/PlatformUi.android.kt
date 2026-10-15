package com.eleazar.localhive.ui.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMediaPicker(kind: MediaKind, onPicked: (PickedMedia) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
        onPicked(PickedMedia(uri.toString(), isVideo))
    }
    val type = when (kind) {
        MediaKind.IMAGE -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaKind.IMAGE_OR_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }
    return remember(launcher, type) { { launcher.launch(PickVisualMediaRequest(type)) } }
}

@Composable
actual fun rememberVideoFrame(uri: String?): Any? {
    val context = LocalContext.current
    val frame by produceState<Bitmap?>(null, uri) {
        value = uri?.let { firstFrame(context, Uri.parse(it)) }
    }
    return frame
}

private suspend fun firstFrame(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

@Composable
actual fun rememberPlatformActions(): PlatformActions {
    val context = LocalContext.current
    return remember(context) { AndroidPlatformActions(context) }
}

private class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override fun copyToClipboard(label: String, text: String, confirmation: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
    }

    override fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    override fun shareFile(fileName: String, mimeType: String, content: String, title: String) {
        // In the cache folder the app's FileProvider serves; overwritten by the next export.
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    override val appVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
}
