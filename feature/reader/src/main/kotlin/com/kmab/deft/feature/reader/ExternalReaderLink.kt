package com.kmab.deft.feature.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kmab.deft.core.designsystem.DeftDialog
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal data class ExternalReaderLink(
    val address: String,
    val domain: String,
)

private const val MAX_WEB_PORT = 65535

/** A strict authority parser keeps the displayed destination identical to the browser destination. */
internal fun externalReaderLink(address: String): ExternalReaderLink? {
    val uri =
        try {
            URI(address)
        } catch (_: URISyntaxException) {
            return null
        }
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val host = uri.host?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
    return if (scheme in listOf("http", "https") && host != null && uri.hasValidAuthority()) {
        val domain = host + if (uri.port >= 0) ":${uri.port}" else ""
        ExternalReaderLink(uri.toASCIIString(), domain)
    } else {
        null
    }
}

private fun URI.hasValidAuthority(): Boolean = rawUserInfo == null && !isOpaque && port in -1..MAX_WEB_PORT

@Composable
internal fun ReaderExternalLinkDialog(
    href: String,
    onDismiss: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val destination = remember(href) { externalReaderLink(href) }
    val context = LocalContext.current
    if (destination == null) {
        LaunchedEffect(href) {
            onDismiss()
            onUnavailable()
        }
        return
    }
    DeftDialog(
        title = "open external link?",
        body = "Open ${destination.domain} in your browser?",
        confirmLabel = "open browser",
        onConfirm = {
            onDismiss()
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(destination.address)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
                    },
                )
            } catch (_: ActivityNotFoundException) {
                onUnavailable()
            } catch (_: SecurityException) {
                onUnavailable()
            }
        },
        onDismiss = onDismiss,
    )
}
