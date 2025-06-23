package com.djatar.vidmax

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.djatar.vidmax.download.DownloadDialog
import com.djatar.vidmax.download.Downloader
import com.djatar.vidmax.ui.theme.VidMaxTheme
import com.djatar.vidmax.utils.InterstitialAdManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.regex.Pattern

class ShareActivity : ComponentActivity() {
    private var url: String = ""

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        window.run {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        handleShareIntent(intent)
        if (url.isEmpty()) {
            finish()
        }

        setContent {
            VidMaxTheme {
                val adManager = remember { InterstitialAdManager(this) }
                LaunchedEffect(Unit) {
                    adManager.loadAd()
                }

                val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else null

                LaunchedEffect(notificationPermission) {
                    if (notificationPermission?.status?.isGranted == false) {
                        notificationPermission.launchPermissionRequest()
                    }
                }

                var showDialog by remember { mutableStateOf(true) }

                DownloadDialog(
                    showDialog = showDialog,
                    onDownloadConfirm = {
                        adManager.showAdIfLoaded {
                            Downloader.downloadFromShared(url)
                            showDialog = false
                            this@ShareActivity.finish()
                        }
                    },
                    onDownloadCancel = {
                        showDialog = false
                        this@ShareActivity.finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        handleShareIntent(intent)
        super.onNewIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString?.let {
                    url = it
                }
            }

            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?.let { sharedContent ->
                        intent.removeExtra(Intent.EXTRA_TEXT)
                        matchUrlFromSharedText(sharedContent)
                            .let { matchedUrl ->
                                url = matchedUrl
                            }
                    }
            }
        }
    }

    private fun matchUrlFromSharedText(s: String): String {
        matchUrlFromString(s).run { return this }
    }

    private fun matchUrlFromString(s: String, isMatchingMultiLink: Boolean = false): String {
        val builder = StringBuilder()
        val pattern =
            Pattern.compile("(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?")
        with(pattern.matcher(s)) {
            if (isMatchingMultiLink)
                while (find()) {
                    if (builder.isNotEmpty())
                        builder.append("\n")
                    builder.append(group())
                }
            else if (find())
                builder.append(group())
        }
        return builder.toString()
    }

    companion object {
        private const val TAG = "ShareActivity"
    }
}