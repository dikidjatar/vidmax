package com.djatar.vidmax.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.djatar.vidmax.R
import com.djatar.vidmax.settings.DOWNLOAD_AUDIO
import com.djatar.vidmax.settings.VIDEO_QUALITY
import com.djatar.vidmax.settings.rememberSetting
import com.djatar.vidmax.settings.toVideoQualityText

@Composable
fun DownloadDialog(
    showDialog: Boolean = false,
    onDownloadConfirm: () -> Unit = {},
    onDownloadCancel: () -> Unit = {}
) {
    var downloadAudio by rememberSetting(DOWNLOAD_AUDIO, false)
    var videoQuality by rememberSetting(VIDEO_QUALITY, 0)

    var showQualityDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.download)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DownloadType(downloadAudio) { isAudio -> downloadAudio = isAudio }
                    if (!downloadAudio) {
                        ElevatedAssistChip(
                            onClick = { showQualityDialog = true },
                            label = { Text(text = videoQuality.toVideoQualityText()) },
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDownloadCancel) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = onDownloadConfirm) {
                    Text(text = stringResource(R.string.download))
                }
            }
        )
    }

    if (showQualityDialog) {
        VideoQualityDialog(
            videoQuality = videoQuality,
            onDismissRequest = { showQualityDialog = false }
        ) { videoQuality = it }
    }
}

@Composable
private fun DownloadType(downloadAudio: Boolean, onCheckedChange: (Boolean) -> Unit) {
    MultiChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SegmentedButton(
            checked = !downloadAudio,
            onCheckedChange = { onCheckedChange(false) },
            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp),
            label = { Text(text = stringResource(R.string.video)) }
        )
        SegmentedButton(
            checked = downloadAudio,
            onCheckedChange = { onCheckedChange(true) },
            shape = RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp),
            label = { Text(text = stringResource(R.string.audio)) }
        )
    }
}

@Composable
fun VideoQualityDialog(
    videoQuality: Int,
    onDismissRequest: () -> Unit,
    onQualitySelected: (Int) -> Unit
) {
    var resolution by remember { mutableIntStateOf(videoQuality) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.video_quality))
        },
        text = {
            LazyColumn {
                items((0..7).toList()) { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { resolution = i },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (i == resolution),
                            onClick = { resolution = i }
                        )
                        Text(
                            text = i.toVideoQualityText(),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onQualitySelected(resolution)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}