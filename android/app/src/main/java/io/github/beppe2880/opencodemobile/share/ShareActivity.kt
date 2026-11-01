package io.github.beppe2880.opencodemobile.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.beppe2880.opencodemobile.MainActivity
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ShareRepository
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_share_nothing
import io.github.beppe2880.opencodemobile.feature.chat.readSharedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject

/**
 * The entry of Android's share menu (enabled by the Settings switch). Without UI: it reads the
 * shared text and copies the images at once, while the sender's read permission lasts, keeps
 * them as the waiting share and brings the app forward, which opens the new session sheet.
 * Nothing shared is logged.
 */
class ShareActivity : ComponentActivity() {

    private val shares: ShareRepository by inject()
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = if (settings.settings.value.showInShareMenu) parse(intent) else null
        lifecycleScope.launch {
            if (request == null) {
                nothingToShare()
                return@launch
            }
            val images = withContext(Dispatchers.IO) { request.images.map { readSharedImage(contentResolver, Uri.parse(it)) } }
            val content = SharedContent(
                text = request.text,
                attachments = images.filterNotNull(),
                skipped = request.rejected + images.count { it == null },
                truncated = request.truncated,
            )
            if (content.isEmpty) {
                nothingToShare()
                return@launch
            }
            shares.offer(content)
            startActivity(
                Intent(this@ShareActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_SHARED, true),
            )
            finish()
        }
    }

    private suspend fun nothingToShare() {
        Toast.makeText(this, getString(Res.string.oc_share_nothing), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun parse(intent: Intent?): ShareRequest? {
        intent ?: return null
        return try {
            ShareIntents.parse(
                action = intent.action,
                type = intent.type,
                text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT),
                subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT),
                streams = streams(intent),
                ownPackage = packageName,
            )
        } catch (_: RuntimeException) {
            // Malformed extras (wrong types, unparcelable values) from the other app.
            null
        }
    }

    private fun streams(intent: Intent): List<String?> {
        val uris: List<Parcelable?> = if (intent.action == ShareIntents.ACTION_SEND_MULTIPLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
            }.orEmpty()
        } else {
            listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                },
            )
        }
        return uris.map { (it as? Uri)?.toString() }
    }
}
