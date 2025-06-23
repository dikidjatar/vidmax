package com.djatar.vidmax.download

import android.app.PendingIntent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.CheckResult
import com.djatar.vidmax.R
import com.djatar.vidmax.VidMaxApp.Companion.applicationScope
import com.djatar.vidmax.VidMaxApp.Companion.audioDownloadDir
import com.djatar.vidmax.VidMaxApp.Companion.context
import com.djatar.vidmax.VidMaxApp.Companion.startService
import com.djatar.vidmax.VidMaxApp.Companion.stopService
import com.djatar.vidmax.VidMaxApp.Companion.videoDownloadDir
import com.djatar.vidmax.settings.DOWNLOAD_AUDIO
import com.djatar.vidmax.settings.Settings
import com.djatar.vidmax.settings.VIDEO_QUALITY
import com.djatar.vidmax.utils.FileUtil
import com.djatar.vidmax.utils.NotificationUtil
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object Downloader {
    private const val TAG = "Downloader"

    const val BASENAME = "%(title).200B"
    const val EXTENSION = ".%(ext)s"

    private const val CLIP_TIMESTAMP = "%(section_start)d-%(section_end)d"
    private const val OUTPUT_TEMPLATE_CLIPS = "$BASENAME [$CLIP_TIMESTAMP]$EXTENSION"

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
    }

    sealed class State {
        data object Idle : State()
        data object Downloading : State()
    }

    private val _taskState = MutableStateFlow(DownloadTaskState())
    private val _uiState = MutableStateFlow(DownloadUiState())
    private val _state = MutableStateFlow<State>(State.Idle)

    val taskState = _taskState.asStateFlow()
    val uiState = _uiState.asStateFlow()
    val state = _state.asStateFlow()

    data class DownloadInfo(
        val aria2c: Boolean = false,
        val videoQuality: Int = Settings.getInt(VIDEO_QUALITY, 0),
        val extractAudio: Boolean = Settings.getBoolean(DOWNLOAD_AUDIO, false)
    )

    private val _quickDownloadCount = MutableStateFlow(0)

    init {
        applicationScope.launch {
            state.combine(_quickDownloadCount) { downloaderState, qdc ->
                when (downloaderState) {
                    is State.Idle -> false
                    else -> true
                } || qdc > 0
            }.collect {
                if (it) startService()
                else stopService()
            }
        }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun updateState(state: State) {
        _state.update { state }
    }

    private fun clearProgressState(isFinished: Boolean) {
        _taskState.update {
            it.copy(
                progress = if (isFinished) 100f else 0f,
                progressText = "",
            )
        }
    }

    @CheckResult
    fun getVideoInfo(
        url: String,
        downloadInfo: DownloadInfo
    ) : Result<VideoInfo> = YoutubeDLRequest(url).apply {
        addOption("-o", BASENAME)
        addOption("--restrict-filenames")
        if (downloadInfo.extractAudio) {
            addOption("-x")
        }
        addOption("--dump-json")
        addOption("-R", "1")
        addOption("--no-playlist")
        addOption("--socket-timeout", "30")
    }.runCatching {
        val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(this, null, null)
        jsonFormat.decodeFromString(response.out)
    }

    fun downloadFromShared(
        url: String,
        downloadInfo: DownloadInfo = DownloadInfo()
    ) {
        applicationScope.launch(Dispatchers.IO) {
            _quickDownloadCount.update { it + 1 }
            getVideoInfo(url, downloadInfo).onSuccess { videoInfo ->
                val taskId = videoInfo.id + downloadInfo.hashCode()
                val notificationId = taskId.hashCode()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.download_start_msg)
                        .format(videoInfo.title), Toast.LENGTH_SHORT).show()
                }
                download(
                    videoInfo = videoInfo,
                    downloadInfo = downloadInfo,
                    taskId = taskId
                ) { progress, _, line ->
                    NotificationUtil.notifyProgress(
                        title = videoInfo.title,
                        progress = progress.toInt(),
                        text = line,
                        taskId = taskId,
                        notificationId = notificationId
                    )
                }.onFailure {
                    manageDownloadError(it, notificationId)
                }.onSuccess {
                    FileUtil.createViewIntent(it.firstOrNull()).run {
                        NotificationUtil.finishNotification(
                            notificationId = notificationId,
                            title = videoInfo.title,
                            text = context.getString(R.string.download_completed),
                            intent = if (this != null) PendingIntent.getActivity(
                                context,
                                0,
                                this,
                                PendingIntent.FLAG_IMMUTABLE
                            ) else null
                        )
                    }
                }
            }.onFailure { manageDownloadError(it) }
            _quickDownloadCount.update { it - 1 }
        }
    }

    @CheckResult
    private suspend fun downloadVideo(
        videoInfo: VideoInfo,
        downloadInfo: DownloadInfo
    ) {
        _taskState.update {
            DownloadTaskState(
                url = videoInfo.webpageUrl.toString(),
                title = videoInfo.title,
                id = videoInfo.id + downloadInfo.hashCode(),
            )
        }
        val taskId = videoInfo.id + downloadInfo.hashCode()
        val notificationId = taskId.hashCode()
        NotificationUtil.notifyProgress(
            notificationId = notificationId, title = videoInfo.title
        )
        download(
            videoInfo = videoInfo,
            downloadInfo = downloadInfo,
            taskId = videoInfo.id + downloadInfo.hashCode()
        ) { progress, _, line ->
            _taskState.update {
                it.copy(progress = progress, progressText = line)
            }
            NotificationUtil.notifyProgress(
                title = videoInfo.title,
                progress = progress.toInt(),
                text = line,
                taskId = taskId,
                notificationId = notificationId
            )
        }.onFailure {
            manageDownloadError(it, notificationId)
        }.onSuccess {
            if (state.value is State.Idle) return
            _taskState.update { it.copy(progress = 100f, progressText = "") }
            clearProgressState(isFinished = true)
            updateState(State.Idle)

            FileUtil.createViewIntent(it.firstOrNull()).run {
                NotificationUtil.finishNotification(
                    notificationId = notificationId,
                    title = videoInfo.title,
                    text = context.getString(R.string.download_completed),
                    intent = if (this != null) PendingIntent.getActivity(
                        context,
                        0,
                        this,
                        PendingIntent.FLAG_IMMUTABLE
                    ) else null
                )
            }
        }
    }

    @CheckResult
    private fun download(
        videoInfo: VideoInfo? = null,
        taskId: String,
        downloadInfo: DownloadInfo,
        callback: ((Float, Long, String) -> Unit)?
    ) : Result<List<String>> {
        if (videoInfo == null) {
            return Result.failure(Throwable(context.getString(R.string.failed_download_msg)))
        }
        val url = videoInfo.originalUrl ?: videoInfo.webpageUrl ?: return Result.failure(
            Throwable(context.getString(R.string.failed_download_msg))
        )
        val request = YoutubeDLRequest(url)
        val pathBuilder = StringBuilder()
        val outputBuilder = StringBuilder()

        request.apply {
            addOption("--no-mtime")
            addOption("--restrict-filenames")
            addOption("--no-playlist")
            if (downloadInfo.aria2c) {
                addOption("--downloader", "libaria2c.so")
                addOption("--external-downloader-args", "aria2c:\"--summary-interval=1\"")
            }

            if (downloadInfo.extractAudio || (videoInfo.vcodec == "none")) {
                pathBuilder.append(audioDownloadDir)
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")
                addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
            } else {
                pathBuilder.append(videoDownloadDir)
                addOption("--add-metadata")
                addOption("--no-embed-info-json")
                val resolution = when (downloadInfo.videoQuality) {
                    1 -> "res:2160"
                    2 -> "res:1440"
                    3 -> "res:1080"
                    4 -> "res:720"
                    5 -> "res:480"
                    6 -> "res:360"
                    7 -> "+res"
                    else -> ""
                }
                if (resolution.isNotEmpty()) {
                    addOption("-S", resolution)
                }
            }
            addOption("-P", pathBuilder.toString())
            addOption("-P", "temp:" + FileUtil.getTempDir())
            val output = OUTPUT_TEMPLATE_CLIPS
            addOption("-o", outputBuilder.append(output).toString())
            for (s in request.buildCommand()) Log.d(TAG, s)
        }.runCatching {
            YoutubeDL.getInstance().execute(
                request = this, processId = taskId, callback = callback
            )
        }.onFailure { return Result.failure(it) }
        val filename = videoInfo.filename ?: videoInfo.title
        return FileUtil.findAndScanMedia(
            title = filename, downloadDir = pathBuilder.toString()
        ).run { Result.success(this) }
    }

    fun startDownload() {
        if (!canDownload()) return
        if (uiState.value.url.isBlank()) {
            Toast.makeText(context, context.getString(R.string.url_empty), Toast.LENGTH_SHORT).show()
            return
        }
        applicationScope.launch(Dispatchers.IO) {
            val downloadInfo = DownloadInfo()
            updateState(State.Downloading)
            getVideoInfo(uiState.value.url, downloadInfo).onSuccess { info ->
                downloadVideo(info, downloadInfo)
            }.onFailure { manageDownloadError(it) }
        }
    }

    fun manageDownloadError(
        throwable: Throwable,
        notificationId: Int? = null
    ) {
        if (throwable is YoutubeDL.CanceledException) return
        throwable.printStackTrace()
        Log.e(TAG, "Failed to downloading video", throwable)
        notificationId?.let {
            NotificationUtil.finishNotification(
                notificationId = notificationId,
                text = context.getString(R.string.download_error)
            )
        }
        updateState(State.Idle)
        clearProgressState(isFinished = false)
    }

    fun canDownload(): Boolean {
        return state.value is State.Idle
    }
}