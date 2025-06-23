package com.djatar.vidmax.download

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djatar.vidmax.R
import com.djatar.vidmax.components.ActionButton
import com.djatar.vidmax.utils.InterstitialAdManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

private const val TAG = "DownloadScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DownloadScreen(
    paddingValues: PaddingValues,
    onNavigateToSetting: () -> Unit = {}
) {
    val context = LocalContext.current
    val adManager = remember { InterstitialAdManager(context) }

    LaunchedEffect(Unit) {
        adManager.loadAd()
    }

    val uiState by Downloader.uiState.collectAsStateWithLifecycle()
    val state by Downloader.state.collectAsStateWithLifecycle()
    val taskState by Downloader.taskState.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
            showNotificationDialog = false
            if (!isGranted) {
                Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    } else null

    val startDownload = {
        if (notificationPermission?.status?.isGranted == false) {
            showNotificationDialog = true
        }
        keyboardController?.hide()
        showDownloadDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onNavigateToSetting() }) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }
                },
                actions = {
                    ActionButton(Icons.Outlined.VideoLibrary) { }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.imePadding(),
                shape = CircleShape,
                onClick = {
                    adManager.showAdIfLoaded {
                        startDownload()
                        adManager.loadAd()
                    }
                }
            ) {
                Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.app_name),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
            DownloadURLInput(
                url = uiState.url,
                onUrlChange = { Downloader.updateUrl(it) }
            ) { startDownload() }

            AnimatedVisibility(visible = !taskState.progressText.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.downloading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = taskState.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    if (taskState.progress > 0) {
                        LinearProgressIndicator(
                            progress = { taskState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    Text(
                        text = taskState.progressText.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    DownloadDialog(
        showDialog = showDownloadDialog,
        onDownloadCancel = { showDownloadDialog = false },
        onDownloadConfirm = {
            showDownloadDialog = false
            Downloader.startDownload()
        }
    )

    if (showNotificationDialog) {
        NotificationPermissionDialog(
            onDismissRequest = { showNotificationDialog = false },
            onPermissionGranted = { notificationPermission?.launchPermissionRequest() }
        )
    }
}

@Composable
private fun DownloadURLInput(
    url: String,
    onUrlChange: (String) -> Unit,
    onGo: () -> Unit = {}
) {
    val trailingContent: @Composable () -> Unit = {
        IconButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = { onUrlChange("") }
        ) {
            Icon(imageVector = Icons.Outlined.Clear, contentDescription = null)
        }
    }

    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        placeholder = { Text(text = stringResource(R.string.url)) },
        shape = CircleShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            unfocusedBorderColor = Color.Transparent
        ),
        trailingIcon = if (url.isNotBlank()) trailingContent else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        textStyle = LocalTextStyle.current.copy(
            lineHeight = 30.sp,
            fontSize = 20.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun NotificationPermissionDialog(
    onDismissRequest: () -> Unit = {},
    onPermissionGranted: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null
            )
        },
        text = {
            Text(text = stringResource(id = R.string.enable_notifications_desc))
        },
        title = { Text(text = stringResource(id = R.string.enable_notifications)) },
        confirmButton = {
            Button(onClick = onPermissionGranted) {
                Text(text = stringResource(id = R.string.OK))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}