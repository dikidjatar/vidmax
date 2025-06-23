package com.djatar.vidmax.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.djatar.vidmax.R
import com.djatar.vidmax.settings.Settings
import com.djatar.vidmax.settings.YT_DLP_VERSION
import com.djatar.vidmax.ui.theme.VidMaxTheme
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsDialog(
    showDialog: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUpdatingYtdlp by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        var ytdlpVersion by remember {
            mutableStateOf(
                YoutubeDL.getInstance().version(context.applicationContext)
                    ?: context.getString(R.string.ytdlp_update)
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.settings)) },
            icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AuthorCard()
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingItem(
                        label = stringResource(R.string.update_ytdlp),
                        description = ytdlpVersion,
                        leadingContent = {
                            if (isUpdatingYtdlp) UpdateProgressIndicator() else
                                Icon(
                                    imageVector = Icons.Outlined.Update,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                        },
                        onClick = {
                            scope.launch {
                                runCatching {
                                    isUpdatingYtdlp = true
                                    Settings.updateYtdlp()
                                    ytdlpVersion = Settings.getString(YT_DLP_VERSION, ytdlpVersion)
                                }.onFailure { th ->
                                    th.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed update Ytdlp", Toast.LENGTH_SHORT).show()
                                    }
                                }.onSuccess {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Success update Ytdlp", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isUpdatingYtdlp = false
                            }
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SettingItem(
    description: String,
    label: String,
    onClick: () -> Unit = {},
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent:  @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {onClick()}
            .padding(horizontal = if (leadingContent != null) 8.dp else 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            Spacer(modifier = Modifier.padding(start = 8.dp))
            leadingContent()
            Spacer(modifier = Modifier.padding(end = 16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailingContent != null) {
            VerticalDivider(modifier = Modifier.height(30.dp))
            trailingContent()
        }
    }
}

@Composable
fun AuthorCard() {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = { uriHandler.openUri("https://github.com/dikidjatar") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.copyright_notice),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Dikidjatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Image(
                painter = painterResource(R.drawable.github_mark),
                contentDescription = "GitHub Icon",
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun UpdateProgressIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp)
    )
}

@Preview
@Composable
private fun AuthorCardPreview() {
    VidMaxTheme {
        AuthorCard()
    }
}