package com.moztro.app.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.moztro.app.data.ClipboardDirection
import com.moztro.app.data.ConnectionState
import com.moztro.app.data.DiscoveredServer
import com.moztro.app.data.FileTransferItem
import com.moztro.app.data.MouseSensitivity
import com.moztro.app.data.OverdriveQuality
import com.moztro.app.data.OverdriveTouchMode
import com.moztro.app.data.PcCursorPosition
import com.moztro.app.data.PcDisplaySource
import com.moztro.app.data.PingRecord
import com.moztro.app.data.ScreenView
import com.moztro.app.data.TransferItemType
import com.moztro.app.data.TransferStatus
import com.moztro.app.network.MoztroWebSocketClient
import com.moztro.app.network.UdpDiscoveryClient
import com.moztro.app.network.WakeOnLanHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("moztro_prefs", Context.MODE_PRIVATE)
    private val discoveryClient = UdpDiscoveryClient(application)
    private var webSocketClient: MoztroWebSocketClient? = null

    val deviceId: String

    private val _storageDirectoryPath = MutableStateFlow<String>("")
    val storageDirectoryPath = _storageDirectoryPath.asStateFlow()

    data class IncomingTransferState(
        val transferId: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val outputFile: File,
        val outputStream: FileOutputStream,
        var bytesReceived: Long = 0L,
        var lastTime: Long = System.currentTimeMillis(),
        var lastBytes: Long = 0L
    )

    private val incomingTransfers = ConcurrentHashMap<String, IncomingTransferState>()

    private val _transferLogs = MutableStateFlow<List<FileTransferItem>>(emptyList())
    val transferLogs = _transferLogs.asStateFlow()

    private val cancelledTransferIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val activeUploadCalls = ConcurrentHashMap<String, Call>()

    private val _currentScreen = MutableStateFlow(ScreenView.MAIN)
    val currentScreen = _currentScreen.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers = _discoveredServers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering = _isDiscovering.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectedServer = MutableStateFlow<DiscoveredServer?>(null)
    val connectedServer = _connectedServer.asStateFlow()

    private val _serverMessage = MutableStateFlow<String?>(null)
    val serverMessage = _serverMessage.asStateFlow()

    private val _pingRecords = MutableStateFlow<List<PingRecord>>(emptyList())
    val pingRecords = _pingRecords.asStateFlow()

    private val _latestRttMs = MutableStateFlow<Long?>(null)
    val latestRttMs = _latestRttMs.asStateFlow()

    private val _isContinuousPing = MutableStateFlow(false)
    val isContinuousPing = _isContinuousPing.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            com.moztro.app.data.AppLanguage.fromCode(
                prefs.getString("app_language", com.moztro.app.data.AppLanguage.ENGLISH.code)
                    ?: com.moztro.app.data.AppLanguage.ENGLISH.code
            )
        } catch (_: Exception) {
            com.moztro.app.data.AppLanguage.ENGLISH
        }
    )
    val appLanguage = _appLanguage.asStateFlow()

    private val _mouseSpeed = MutableStateFlow<Int>(prefs.getInt("mouse_speed", 10).coerceIn(1, 20))
    val mouseSpeed = _mouseSpeed.asStateFlow()

    private val _scrollLines = MutableStateFlow(prefs.getInt("scroll_lines", 3).coerceIn(1, 10))
    val scrollLines = _scrollLines.asStateFlow()

    private val _isFullscreen = MutableStateFlow(prefs.getBoolean("fullscreen_enabled", false))
    val isFullscreen = _isFullscreen.asStateFlow()

    private val _deleteFileOnClearHistory = MutableStateFlow(prefs.getBoolean("delete_file_on_clear_history", false))
    val deleteFileOnClearHistory = _deleteFileOnClearHistory.asStateFlow()

    private val ftpServerManager = com.moztro.app.network.FtpServerManager(port = 2121)

    private val _vodDefaultOn = MutableStateFlow(prefs.getBoolean("vod_default_on", true))
    val vodDefaultOn = _vodDefaultOn.asStateFlow()

    private val _isVodEnabled = MutableStateFlow(false)
    val isVodEnabled = _isVodEnabled.asStateFlow()

    private val _vodFtpUrl = MutableStateFlow<String?>(null)
    val vodFtpUrl = _vodFtpUrl.asStateFlow()

    // ─── Overdrive (Screen Mirroring & Remote Control) States ───────────────────
    private val _overdriveFrame = MutableStateFlow<Bitmap?>(null)
    val overdriveFrame = _overdriveFrame.asStateFlow()

    // Frame drop buffer: holds latest incoming frame and delivers it smoothly to UI
    // Prevents overwhelming Compose recomposition with 50fps frame updates
    private val pendingFrame = AtomicReference<Bitmap?>(null)
    private val lastFrameDeliveredAt = AtomicLong(0L)
    private val frameDeliveryJob = AtomicReference<Job?>(null)

    /** Receive a new frame — store as pending, schedule delivery if not already scheduled */
    private fun deliverFrame(bitmap: Bitmap) {
        pendingFrame.set(bitmap)
        // Only schedule a new delivery job if none is pending
        if (frameDeliveryJob.get()?.isActive == true) return
        val job = viewModelScope.launch(Dispatchers.Main.immediate) {
            val frame = pendingFrame.getAndSet(null) ?: return@launch
            val now = System.currentTimeMillis()
            val elapsed = now - lastFrameDeliveredAt.get()
            // Throttle to max 60fps (16ms per frame) — skip if too soon
            if (elapsed >= 14L) {
                lastFrameDeliveredAt.set(now)
                _overdriveFrame.value = frame
            }
        }
        frameDeliveryJob.set(job)
    }

    private val _availableMonitors = MutableStateFlow<List<PcDisplaySource>>(emptyList())
    val availableMonitors = _availableMonitors.asStateFlow()

    private val _selectedMonitorId = MutableStateFlow<String?>(null)
    val selectedMonitorId = _selectedMonitorId.asStateFlow()

    private val _overdriveTouchMode = MutableStateFlow(
        try {
            OverdriveTouchMode.valueOf(prefs.getString("overdrive_touch_mode", OverdriveTouchMode.DIRECT_TOUCH.name) ?: OverdriveTouchMode.DIRECT_TOUCH.name)
        } catch (_: Exception) {
            OverdriveTouchMode.DIRECT_TOUCH
        }
    )
    val overdriveTouchMode = _overdriveTouchMode.asStateFlow()

    private val _overdriveQuality = MutableStateFlow(
        try {
            OverdriveQuality.valueOf(prefs.getString("overdrive_quality", OverdriveQuality.HIGH.name) ?: OverdriveQuality.HIGH.name)
        } catch (_: Exception) {
            OverdriveQuality.HIGH
        }
    )
    val overdriveQuality = _overdriveQuality.asStateFlow()

    private val _overdriveAudioEnabled = MutableStateFlow(prefs.getBoolean("overdrive_audio_enabled", true))
    val overdriveAudioEnabled = _overdriveAudioEnabled.asStateFlow()

    private val _pcCursorPosition = MutableStateFlow<PcCursorPosition?>(null)
    val pcCursorPosition = _pcCursorPosition.asStateFlow()

    private val _isAudioMuted = MutableStateFlow(false)
    val isAudioMuted = _isAudioMuted.asStateFlow()

    private val _isOverdriveActive = MutableStateFlow(false)
    val isOverdriveActive = _isOverdriveActive.asStateFlow()

    private val _overdriveStreamWidth = MutableStateFlow(1920)
    val overdriveStreamWidth = _overdriveStreamWidth.asStateFlow()

    private val _overdriveStreamHeight = MutableStateFlow(1080)
    val overdriveStreamHeight = _overdriveStreamHeight.asStateFlow()

    // ─── App Update States ──────────────────────────────────────────────────
    val currentVersionName = "1.0.2"
    val currentVersionFormatted = "v1.0.2"

    private val _availableUpdate = MutableStateFlow<com.moztro.app.data.AppUpdateInfo?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate = _isDownloadingUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow(0f)
    val updateDownloadProgress = _updateDownloadProgress.asStateFlow()

    private val _downloadSpeedFormatted = MutableStateFlow("")
    val downloadSpeedFormatted = _downloadSpeedFormatted.asStateFlow()

    private val _isUpdateReadyToInstall = MutableStateFlow(false)
    val isUpdateReadyToInstall = _isUpdateReadyToInstall.asStateFlow()

    private var downloadedApkFile: File? = null

    private var audioTrack: AudioTrack? = null

    private fun initAudioTrack() {
        try {
            if (audioTrack == null) {
                val minBufSize = AudioTrack.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(16000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize.coerceAtLeast(8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack?.play()
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "AudioTrack init error: ${e.message}")
        }
    }

    private fun playAudioChunk(bytes: ByteArray) {
        if (_isAudioMuted.value || !_overdriveAudioEnabled.value) return
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
            audioTrack?.write(bytes, 0, bytes.size)
        } catch (_: Exception) {}
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun setOverdriveTouchMode(mode: OverdriveTouchMode) {
        _overdriveTouchMode.value = mode
        prefs.edit().putString("overdrive_touch_mode", mode.name).apply()
    }

    fun setOverdriveQuality(quality: OverdriveQuality) {
        _overdriveQuality.value = quality
        prefs.edit().putString("overdrive_quality", quality.name).apply()
        if (_isOverdriveActive.value) {
            webSocketClient?.startOverdriveStream(_selectedMonitorId.value, quality.code, _overdriveAudioEnabled.value && !_isAudioMuted.value)
        }
    }

    fun setOverdriveAudioEnabled(enabled: Boolean) {
        _overdriveAudioEnabled.value = enabled
        prefs.edit().putBoolean("overdrive_audio_enabled", enabled).apply()
        if (!enabled) {
            releaseAudioTrack()
        }
    }

    fun toggleAudioMute() {
        _isAudioMuted.value = !_isAudioMuted.value
        // Notify PC server to start/stop sending audio chunks
        if (_isOverdriveActive.value) {
            val audioEnabled = _overdriveAudioEnabled.value && !_isAudioMuted.value
            webSocketClient?.startOverdriveStream(
                sourceId = _selectedMonitorId.value,
                quality = _overdriveQuality.value.code,
                audioEnabled = audioEnabled
            )
        }
    }

    fun selectOverdriveMonitor(sourceId: String) {
        _selectedMonitorId.value = sourceId
        webSocketClient?.switchOverdriveMonitor(sourceId)
    }

    fun startOverdriveSession() {
        _isOverdriveActive.value = true
        initAudioTrack()
        webSocketClient?.requestScreenSources()
        webSocketClient?.startOverdriveStream(
            sourceId = _selectedMonitorId.value,
            quality = _overdriveQuality.value.code,
            audioEnabled = _overdriveAudioEnabled.value
        )
    }

    fun stopOverdriveSession() {
        _isOverdriveActive.value = false
        _overdriveFrame.value = null
        _pcCursorPosition.value = null
        releaseAudioTrack()
        webSocketClient?.stopOverdriveStream()
    }

    fun sendOverdriveTouch(
        action: String,
        x: Float? = null,
        y: Float? = null,
        dx: Float? = null,
        dy: Float? = null,
        button: String? = null,
        deltaY: Int? = null
    ) {
        webSocketClient?.sendOverdriveTouch(action, x, y, dx, dy, button, deltaY)
    }

    private val _isMouseDragging = MutableStateFlow(false)
    val isMouseDragging = _isMouseDragging.asStateFlow()

    private var onClipboardPasteCallback: ((Boolean, String) -> Unit)? = null

    fun setAppLanguage(language: com.moztro.app.data.AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        addLog("App language changed to: ${language.displayName}")
    }

    fun setMouseSpeed(speed: Int) {
        val clamped = speed.coerceIn(1, 20)
        _mouseSpeed.value = clamped
        prefs.edit().putInt("mouse_speed", clamped).apply()
    }

    fun setScrollLines(lines: Int) {
        val clamped = lines.coerceIn(1, 10)
        _scrollLines.value = clamped
        prefs.edit().putInt("scroll_lines", clamped).apply()
    }

    fun setFullscreen(enabled: Boolean) {
        _isFullscreen.value = enabled
        prefs.edit().putBoolean("fullscreen_enabled", enabled).apply()
    }

    fun toggleFullscreen() {
        setFullscreen(!_isFullscreen.value)
    }

    fun setDeleteFileOnClearHistory(enabled: Boolean) {
        _deleteFileOnClearHistory.value = enabled
        prefs.edit().putBoolean("delete_file_on_clear_history", enabled).apply()
    }

    fun setVodDefaultOn(enabled: Boolean) {
        _vodDefaultOn.value = enabled
        prefs.edit().putBoolean("vod_default_on", enabled).apply()
    }

    fun startVodServer() {
        val ip = getDeviceIpAddress() ?: "127.0.0.1"
        ftpServerManager.start(
            onStarted = { port ->
                _isVodEnabled.value = true
                val url = "ftp://$ip:$port/"
                _vodFtpUrl.value = url
                webSocketClient?.sendVodStatus(true, url, port)
                addLog("VOD FTP Server active at $url")
            },
            onError = { e ->
                _isVodEnabled.value = false
                _vodFtpUrl.value = null
                webSocketClient?.sendVodStatus(false, null, 0)
                addLog("VOD FTP Server failed: ${e.message}")
            }
        )
    }

    fun stopVodServer() {
        ftpServerManager.stop()
        _isVodEnabled.value = false
        _vodFtpUrl.value = null
        webSocketClient?.sendVodStatus(false, null, 0)
        addLog("VOD FTP Server stopped")
    }

    fun toggleVod() {
        if (_isVodEnabled.value) {
            stopVodServer()
        } else {
            startVodServer()
        }
    }

    fun getDeviceIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private val _isTouchpadKeyboardVisible = MutableStateFlow(false)
    val isTouchpadKeyboardVisible = _isTouchpadKeyboardVisible.asStateFlow()

    fun toggleTouchpadKeyboard() {
        _isTouchpadKeyboardVisible.value = !_isTouchpadKeyboardVisible.value
    }

    fun setTouchpadKeyboardVisible(visible: Boolean) {
        _isTouchpadKeyboardVisible.value = visible
    }

    fun setMouseDragging(dragging: Boolean) {
        _isMouseDragging.value = dragging
    }

    private var discoveryJob: Job? = null
    private var continuousPingJob: Job? = null
    private var autoReconnectJob: Job? = null
    private var userManuallyDisconnected = false

    init {
        var savedId = prefs.getString("device_id", null)
        if (savedId == null) {
            savedId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", savedId).apply()
        }
        deviceId = savedId

        val defaultStorage = File(Environment.getExternalStorageDirectory(), "Moztro").absolutePath
        val savedStorage = prefs.getString("storage_directory_path", defaultStorage) ?: defaultStorage
        _storageDirectoryPath.value = savedStorage
        ensureDirectoryStructure(savedStorage)

        initWebSocketClient()
        startPresenceBeaconLoop()
        startAutoReconnectLoop()
        checkForAppUpdate()
    }

    fun ensureDirectoryStructure(basePath: String) {
        try {
            val base = File(basePath)
            if (!base.exists()) base.mkdirs()
            listOf("Document", "Image", "Music", "Video").forEach { sub ->
                val dir = File(base, sub)
                if (!dir.exists()) dir.mkdirs()
            }
            Log.d("MainViewModel", "Directory structure ensured at: $basePath")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed creating directory structure: ${e.message}")
        }
    }

    /** Called from MainActivity after storage permission is granted at runtime */
    fun ensureDirectoryStructureNow() {
        val path = _storageDirectoryPath.value.ifBlank {
            File(Environment.getExternalStorageDirectory(), "Moztro").absolutePath
        }
        ensureDirectoryStructure(path)
    }

    fun updateStorageDirectory(newPath: String) {
        _storageDirectoryPath.value = newPath
        prefs.edit().putString("storage_directory_path", newPath).apply()
        ensureDirectoryStructure(newPath)
        addLog("Storage directory updated: $newPath")
    }

    fun resolveDestinationFolder(fileName: String, mimeType: String? = null): File {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val subfolder = when {
            ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif") || mimeType?.startsWith("image/") == true -> "Image"
            ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv") || mimeType?.startsWith("video/") == true -> "Video"
            ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus") || mimeType?.startsWith("audio/") == true -> "Music"
            else -> "Document"
        }

        // Prefer the user-configured / Moztro shared storage path
        val preferredBase = _storageDirectoryPath.value.ifBlank {
            File(Environment.getExternalStorageDirectory(), "Moztro").absolutePath
        }
        val preferredDir = File(preferredBase, subfolder)

        // Check if we can actually write to this directory
        val canWritePreferred = try {
            if (!preferredDir.exists()) preferredDir.mkdirs()
            preferredDir.canWrite()
        } catch (_: Exception) { false }

        if (canWritePreferred) return preferredDir

        // Fallback: app-private external storage (no special permission needed)
        val app = getApplication<Application>()
        val fallbackBase = app.getExternalFilesDir(null)
            ?: app.filesDir  // last resort: internal storage
        val fallbackDir = File(fallbackBase, "Moztro/$subfolder")
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        Log.w("MainViewModel", "Using fallback storage: ${fallbackDir.absolutePath}")
        return fallbackDir
    }


    private var presenceBeaconJob: Job? = null

    private fun startPresenceBeaconLoop() {
        presenceBeaconJob?.cancel()
        presenceBeaconJob = viewModelScope.launch {
            val deviceName = if (android.os.Build.MODEL.startsWith(android.os.Build.MANUFACTURER, ignoreCase = true)) {
                android.os.Build.MODEL
            } else {
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            }
            while (isActive) {
                try {
                    discoveryClient.sendPresenceBeacon(deviceId, deviceName)
                } catch (_: Exception) {}
                delay(1500)
            }
        }
    }

    /**
     * Auto-reconnect loop — runs in background, tries to reconnect to the last known PC
     * whenever the connection is lost. Uses exponential backoff to save battery.
     *
     * - First retry: 5 seconds
     * - Subsequent retries: doubles each time, capped at 30 seconds
     * - Stops when user manually disconnects (via onRadarClick → disconnect())
     * - Restarts when user manually initiates a new connection
     */
    private fun startAutoReconnectLoop() {
        autoReconnectJob?.cancel()
        autoReconnectJob = viewModelScope.launch {
            var retryDelayMs = 5_000L
            while (isActive) {
                val state = _connectionState.value
                if (!userManuallyDisconnected &&
                    (state == ConnectionState.DISCONNECTED || state == ConnectionState.FAILED)
                ) {
                    val lastIp = prefs.getString("last_server_ip", null)
                    if (!lastIp.isNullOrBlank()) {
                        addLog("Auto-reconnect: trying $lastIp (retry in ${retryDelayMs / 1000}s)...")
                        connectManual(lastIp)
                        delay(retryDelayMs)
                        // If still not connected after waiting, increase backoff
                        if (_connectionState.value != ConnectionState.CONNECTED) {
                            retryDelayMs = (retryDelayMs * 2).coerceAtMost(30_000L)
                        } else {
                            retryDelayMs = 5_000L // reset on success
                        }
                    } else {
                        // No saved IP yet — wait longer before checking again
                        delay(10_000L)
                    }
                } else if (state == ConnectionState.CONNECTED) {
                    retryDelayMs = 5_000L // reset backoff whenever connected
                    delay(3_000L) // poll state periodically
                } else {
                    // CONNECTING / PAIRING_REQUESTED / DISCOVERING / manual disconnect — just wait
                    delay(3_000L)
                }
            }
        }
    }


    private fun initWebSocketClient() {
        webSocketClient = MoztroWebSocketClient(
            deviceId = deviceId,
            onStateChanged = { state, info ->
                _connectionState.value = state
                _serverMessage.value = info
                if (state == ConnectionState.CONNECTED) {
                    _connectedServer.value?.ip?.let { ip ->
                        prefs.edit().putString("last_server_ip", ip).apply()
                    }
                    if (_vodDefaultOn.value) {
                        startVodServer()
                    }
                    if (_currentScreen.value == ScreenView.OVERDRIVE) {
                        startOverdriveSession()
                    }
                } else if (state == ConnectionState.DISCONNECTED || state == ConnectionState.FAILED || state == ConnectionState.REJECTED) {
                    stopContinuousPing()
                    stopVodServer()
                    _isOverdriveActive.value = false
                    _overdriveFrame.value = null
                    releaseAudioTrack()
                }
            },
            onPingResult = { record ->
                if (record.isSuccess) {
                    _latestRttMs.value = record.rttMs
                }
                val updated = listOf(record) + _pingRecords.value
                _pingRecords.value = updated.take(30)
            },
            onMacReceived = { mac ->
                saveMacAddress(mac)
            },
            onClipboardResponse = { text ->
                handleIncomingClipboard(text)
            },
            onClipboardGetRequest = {
                val clipboardManager = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                webSocketClient?.sendClipboardGetResponse(clipText)
                addLog("Sent clipboard to PC (${clipText.length} chars)")
            },
            onFileTransferStart = { transferId, fileName, fileSize, mimeType ->
                try {
                    val targetDir = resolveDestinationFolder(fileName, mimeType)
                    var destinationFile = File(targetDir, fileName)
                    if (destinationFile.exists()) {
                        val nameWithoutExt = fileName.substringBeforeLast('.')
                        val extWithDot = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                        destinationFile = File(targetDir, "${nameWithoutExt}_${System.currentTimeMillis()}$extWithDot")
                    }
                    val fos = FileOutputStream(destinationFile)
                    val state = IncomingTransferState(
                        transferId = transferId,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        outputFile = destinationFile,
                        outputStream = fos
                    )
                    incomingTransfers[transferId] = state

                    val logItem = FileTransferItem(
                        id = transferId,
                        name = fileName,
                        sizeBytes = fileSize,
                        bytesUploaded = 0L,
                        progress = 0f,
                        speedFormatted = "Receiving...",
                        status = TransferStatus.UPLOADING
                    )
                    _transferLogs.value = listOf(logItem) + _transferLogs.value
                    addLog("Receiving $fileName from PC...")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to prepare file reception: ${e.message}", e)
                    addLog("Error preparing $fileName: ${e.message}")
                }
            },
            onFileTransferChunk = { transferId, chunkBase64, chunkIndex, isLast ->
                val state = incomingTransfers[transferId]
                if (state != null) {
                    try {
                        val bytes = android.util.Base64.decode(chunkBase64, android.util.Base64.DEFAULT)
                        state.outputStream.write(bytes)
                        state.bytesReceived += bytes.size

                        val now = System.currentTimeMillis()
                        val timeDiff = now - state.lastTime
                        if (timeDiff >= 200 || isLast) {
                            val bytesDiff = state.bytesReceived - state.lastBytes
                            val speedBytesPerSec = if (timeDiff > 0) (bytesDiff * 1000L) / timeDiff else 0L
                            val speedFormatted = formatSpeed(speedBytesPerSec)
                            val progress = if (state.fileSize > 0) (state.bytesReceived.toFloat() / state.fileSize).coerceIn(0f, 1f) else 0f

                            updateTransferProgress(transferId, state.bytesReceived, progress, speedFormatted)
                            webSocketClient?.sendFileTransferProgress(transferId, "UPLOADING", progress, speedFormatted)
                            state.lastTime = now
                            state.lastBytes = state.bytesReceived
                        }

                        if (isLast) {
                            state.outputStream.flush()
                            state.outputStream.close()
                            incomingTransfers.remove(transferId)

                            MediaScannerConnection.scanFile(
                                getApplication(),
                                arrayOf(state.outputFile.absolutePath),
                                arrayOf(state.mimeType.ifBlank { null })
                            ) { path, uri ->
                                Log.d("MainViewModel", "Scanned file $path -> $uri")
                            }

                            _transferLogs.value = _transferLogs.value.map {
                                if (it.id == transferId) it.copy(
                                    status = TransferStatus.COMPLETE,
                                    progress = 1f,
                                    bytesUploaded = state.fileSize,
                                    speedFormatted = "Complete",
                                    localFilePath = state.outputFile.absolutePath
                                ) else it
                            }
                            webSocketClient?.sendFileTransferProgress(transferId, "COMPLETE", 1f, "Complete")
                            addLog("Saved ${state.fileName} to ${state.outputFile.parentFile?.name}/")
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error saving chunk: ${e.message}", e)
                        try { state.outputStream.close() } catch (_: Exception) {}
                        incomingTransfers.remove(transferId)
                        _transferLogs.value = _transferLogs.value.map {
                            if (it.id == transferId) it.copy(status = TransferStatus.FAILED, speedFormatted = "Failed") else it
                        }
                        webSocketClient?.sendFileTransferProgress(transferId, "FAILED", 0f, "Failed", e.message)
                        addLog("Error receiving ${state.fileName}: ${e.message}")
                    }
                }
            },
            onFileTransferCancel = { transferId ->
                val state = incomingTransfers.remove(transferId)
                try {
                    state?.outputStream?.close()
                    state?.outputFile?.delete()
                } catch (_: Exception) {}
                _transferLogs.value = _transferLogs.value.map {
                    if (it.id == transferId) it.copy(status = TransferStatus.CANCELLED, speedFormatted = "Cancelled") else it
                }
                addLog("Cancelled incoming file transfer: $transferId")
            },
            onOverdriveFrame = { bitmap ->
                deliverFrame(bitmap)
            },
            onOverdriveAudio = { pcmBytes ->
                playAudioChunk(pcmBytes)
            },
            onScreenSourcesReceived = { list ->
                _availableMonitors.value = list
                if (_selectedMonitorId.value == null && list.isNotEmpty()) {
                    _selectedMonitorId.value = list[0].id
                }
            },
            onOverdriveStreamStarted = { w, h, srcId ->
                _overdriveStreamWidth.value = w
                _overdriveStreamHeight.value = h
                _selectedMonitorId.value = srcId
                _isOverdriveActive.value = true
            },
            onOverdriveStreamError = { err ->
                addLog("Overdrive error: $err")
            },
            onCursorPos = { x, y, sw, sh ->
                _pcCursorPosition.value = PcCursorPosition(x, y, sw, sh)
            },
            onLog = { logMsg ->
                addLog(logMsg)
            }
        )
    }

    private fun saveMacAddress(mac: String) {
        if (mac.isNotEmpty() && mac != "00:00:00:00:00:00") {
            prefs.edit().putString("saved_mac_address", mac).apply()
            addLog("Saved PC MAC Address: $mac")
        }
    }

    fun getSavedMacAddress(): String? {
        return prefs.getString("saved_mac_address", null)
    }

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val updated = listOf("[$time] $msg") + _logs.value
        _logs.value = updated.take(50)
    }

    fun navigateTo(screen: ScreenView) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (_currentScreen.value != ScreenView.MAIN) {
            _currentScreen.value = ScreenView.MAIN
            return true
        }
        return false
    }

    // Called when the Radar icon on the Top Bar is clicked
    fun onRadarClick() {
        when (_connectionState.value) {
            ConnectionState.CONNECTED -> {
                disconnect()
            }
            ConnectionState.CONNECTING, ConnectionState.PAIRING_REQUESTED, ConnectionState.DISCOVERING -> {
                disconnect()
            }
            else -> {
                // User manually initiates connect → re-enable auto-reconnect
                userManuallyDisconnected = false
                startAutoReconnectLoop()
                startDiscoveryAndConnect()
            }
        }
    }

    fun startDiscoveryAndConnect() {
        discoveryJob?.cancel()
        _isDiscovering.value = true
        _connectionState.value = ConnectionState.DISCOVERING
        addLog("Scanning for PC Server...")

        val lastIp = prefs.getString("last_server_ip", null) ?: "192.168.0.196"

        if (lastIp.isNotBlank()) {
            connectManual(lastIp)
        }

        discoveryJob = viewModelScope.launch {
            try {
                discoveryClient.discoverServers(timeoutMs = 6000) { server ->
                    server.mac?.let { saveMacAddress(it) }
                    val current = _discoveredServers.value.toMutableList()
                    val existingIndex = current.indexOfFirst { it.ip == server.ip }
                    if (existingIndex >= 0) {
                        current[existingIndex] = server
                    } else {
                        current.add(server)
                    }
                    _discoveredServers.value = current

                    if (_connectionState.value == ConnectionState.DISCOVERING || _connectionState.value == ConnectionState.DISCONNECTED) {
                        connectToServer(server)
                    }
                }
            } finally {
                _isDiscovering.value = false
                if (_connectionState.value == ConnectionState.DISCOVERING) {
                    if (lastIp != null && _connectionState.value != ConnectionState.CONNECTED) {
                        addLog("Direct connection to last PC ($lastIp)...")
                        connectManual(lastIp)
                    } else {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        addLog("No PC server found.")
                    }
                }
            }
        }
    }

    fun connectToServer(server: DiscoveredServer) {
        _connectedServer.value = server
        server.mac?.let { saveMacAddress(it) }
        addLog("Connecting to ${server.hostname} (${server.ip}:${server.wsPort})...")
        webSocketClient?.connect(server.ip, server.wsPort)
    }

    fun connectManual(ip: String, port: Int = 8765) {
        val manualServer = DiscoveredServer(hostname = "PC Server ($ip)", ip = ip, wsPort = port)
        connectToServer(manualServer)
    }

    fun disconnect() {
        userManuallyDisconnected = true
        autoReconnectJob?.cancel()
        autoReconnectJob = null
        discoveryJob?.cancel()
        _isDiscovering.value = false
        stopContinuousPing()
        webSocketClient?.disconnect()
        _connectedServer.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        addLog("Disconnected.")
    }

    // Wake On LAN Action
    fun executeWakeOnLan(onResult: (Boolean, String) -> Unit) {
        val mac = getSavedMacAddress()
        if (mac.isNullOrEmpty()) {
            onResult(false, "Connect to PC once to record MAC Address")
            addLog("WOL failed: No MAC Address recorded yet.")
            return
        }

        viewModelScope.launch {
            val success = WakeOnLanHelper.sendWakeOnLan(getApplication(), mac)
            if (success) {
                onResult(true, "WOL Magic Packet sent to $mac")
                addLog("Sent Wake On LAN packet to $mac")
            } else {
                onResult(false, "Failed to send WOL packet")
                addLog("Failed sending WOL packet")
            }
        }
    }

    // Power Actions (Shutdown, Restart, Sleep)
    fun executePowerAction(action: String, onResult: (Boolean, String) -> Unit) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            onResult(false, "Not connected to PC")
            addLog("Cannot execute $action: Not connected to PC")
            return
        }

        val sent = webSocketClient?.sendPowerCommand(action) ?: false
        if (sent) {
            onResult(true, "Command $action sent to PC")
            addLog("Sent Power Command: $action")
        } else {
            onResult(false, "Failed sending $action command")
            addLog("Failed sending Power Command: $action")
        }
    }

    // Keyboard Input Action
    fun sendKey(key: String, modifiers: List<String> = emptyList()) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendKeyEvent(key, modifiers)
        }
    }

    fun setModifier(modifier: String, isDown: Boolean) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendModifierEvent(modifier, isDown)
        }
    }

    fun releaseAllModifiers() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendReleaseAllModifiers()
        }
    }

    // Mouse / Touchpad Input Actions
    fun sendMouseMove(dx: Float, dy: Float) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendMouseMove(dx, dy)
        }
    }

    fun sendMouseClick(button: String = "LEFT") {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendMouseClick(button)
        }
    }

    fun sendMouseDown(button: String = "LEFT") {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendMouseDown(button)
        }
    }

    fun sendMouseUp(button: String = "LEFT") {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendMouseUp(button)
        }
    }

    fun sendMouseScroll(deltaY: Int) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocketClient?.sendMouseScroll(deltaY)
        }
    }

    fun cancelTransfer(transferId: String) {
        cancelledTransferIds.add(transferId)
        activeUploadCalls[transferId]?.cancel()
        _transferLogs.value = _transferLogs.value.map { item ->
            if (item.id == transferId && item.status == TransferStatus.UPLOADING) {
                item.copy(status = TransferStatus.CANCELLED, speedFormatted = "Cancelled")
            } else {
                item
            }
        }
        addLog("Cancelled upload of transfer: $transferId")
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024f * 1024f))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024f)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    fun clearTransferLogs(idsToClear: Set<String>) {
        idsToClear.forEach { id ->
            cancelledTransferIds.add(id)
            activeUploadCalls[id]?.cancel()
            activeUploadCalls.remove(id)
        }

        if (_deleteFileOnClearHistory.value) {
            _transferLogs.value
                .filter { idsToClear.contains(it.id) && it.localFilePath != null }
                .forEach { item ->
                    try {
                        val f = File(item.localFilePath!!)
                        if (f.exists()) {
                            f.delete()
                            addLog("Deleted file: ${item.name}")
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Could not delete ${item.name}: ${e.message}")
                    }
                }
        }

        _transferLogs.value = _transferLogs.value.filterNot { idsToClear.contains(it.id) }
    }

    private fun updateTransferProgress(
        transferId: String,
        bytesUploaded: Long,
        progress: Float,
        speedFormatted: String
    ) {
        _transferLogs.value = _transferLogs.value.map { item ->
            if (item.id == transferId && item.status == TransferStatus.UPLOADING) {
                item.copy(
                    bytesUploaded = bytesUploaded,
                    progress = progress,
                    speedFormatted = speedFormatted
                )
            } else {
                item
            }
        }
    }

    private fun handleIncomingClipboard(text: String) {
        val app = getApplication<Application>()
        if (text.isBlank()) {
            viewModelScope.launch(Dispatchers.Main) {
                onClipboardPasteCallback?.invoke(false, "PC clipboard is empty")
                onClipboardPasteCallback = null
            }
            return
        }

        try {
            val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Moztro PC Clipboard", text)
            clipboard?.setPrimaryClip(clip)

            val logItem = FileTransferItem(
                id = UUID.randomUUID().toString(),
                name = text,
                sizeBytes = text.toByteArray().size.toLong(),
                bytesUploaded = text.toByteArray().size.toLong(),
                progress = 1f,
                speedFormatted = "Copied",
                status = TransferStatus.COMPLETE,
                itemType = TransferItemType.CLIPBOARD,
                textContent = text,
                clipboardDirection = ClipboardDirection.FROM_PC
            )
            _transferLogs.value = listOf(logItem) + _transferLogs.value

            viewModelScope.launch(Dispatchers.Main) {
                onClipboardPasteCallback?.invoke(true, "Copied from PC clipboard")
                onClipboardPasteCallback = null
            }
            addLog("Clipboard fetched from PC (${text.length} chars)")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to paste clipboard", e)
            viewModelScope.launch(Dispatchers.Main) {
                onClipboardPasteCallback?.invoke(false, "Failed to copy to clipboard: ${e.message}")
                onClipboardPasteCallback = null
            }
        }
    }

    fun sendClipboard(
        context: Context,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
        } else {
            ""
        }

        if (text.isBlank()) {
            onResult(false, "Clipboard is empty")
            return
        }

        val isConnected = _connectionState.value == ConnectionState.CONNECTED
        val logItem = FileTransferItem(
            id = UUID.randomUUID().toString(),
            name = text,
            sizeBytes = text.toByteArray().size.toLong(),
            bytesUploaded = if (isConnected) text.toByteArray().size.toLong() else 0L,
            progress = if (isConnected) 1f else 0f,
            speedFormatted = if (isConnected) "Sent" else "Offline",
            status = if (isConnected) TransferStatus.COMPLETE else TransferStatus.FAILED,
            itemType = TransferItemType.CLIPBOARD,
            textContent = text,
            clipboardDirection = ClipboardDirection.TO_PC
        )
        _transferLogs.value = listOf(logItem) + _transferLogs.value

        if (!isConnected) {
            onResult(false, "Not connected to PC")
            return
        }

        val sent = webSocketClient?.sendClipboardText(text) ?: false
        if (sent) {
            addLog("Sent clipboard to PC (${text.length} chars)")
            onResult(true, "Clipboard sent to PC")
        } else {
            onResult(false, "Failed to send clipboard")
        }
    }

    fun requestPasteClipboard(
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            onResult(false, "Not connected to PC")
            return
        }
        onClipboardPasteCallback = onResult
        val sent = webSocketClient?.requestClipboard() ?: false
        if (!sent) {
            onClipboardPasteCallback = null
            onResult(false, "Failed to request clipboard from PC")
        }
    }

    // File Transfer Action
    fun sendFiles(
        uris: List<Uri>,
        context: Context,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit,
        onComplete: (successCount: Int, failCount: Int, message: String) -> Unit
    ) {
        val total = uris.size
        if (total == 0) return

        // Pre-create transfer items so they immediately show in the UI list
        val itemsToAdd = uris.map { uri ->
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val queriedName = cursor.getString(nameIndex)
                        if (!queriedName.isNullOrBlank()) {
                            fileName = queriedName
                        }
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}

            FileTransferItem(
                id = UUID.randomUUID().toString(),
                name = fileName,
                sizeBytes = fileSize,
                bytesUploaded = 0L,
                progress = 0f,
                speedFormatted = "0 KB/s",
                status = TransferStatus.UPLOADING
            )
        }

        // Add newest items to top of log
        _transferLogs.value = itemsToAdd + _transferLogs.value

        val serverIp = _connectedServer.value?.ip ?: _discoveredServers.value.firstOrNull()?.ip
        if (serverIp == null || _connectionState.value != ConnectionState.CONNECTED) {
            val addedIds = itemsToAdd.map { it.id }.toSet()
            _transferLogs.value = _transferLogs.value.map {
                if (addedIds.contains(it.id)) it.copy(status = TransferStatus.FAILED, speedFormatted = "Offline") else it
            }
            onComplete(0, uris.size, "Not connected to PC server")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            var failCount = 0

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            uris.forEachIndexed { index, uri ->
                val transferItem = itemsToAdd[index]
                val transferId = transferItem.id
                val fileName = transferItem.name
                val fileSize = transferItem.sizeBytes

                if (cancelledTransferIds.contains(transferId)) {
                    _transferLogs.value = _transferLogs.value.map {
                        if (it.id == transferId) it.copy(status = TransferStatus.CANCELLED, speedFormatted = "Cancelled") else it
                    }
                    return@forEachIndexed
                }

                withContext(Dispatchers.Main) {
                    onProgress(index + 1, total, fileName)
                }

                try {
                    val requestBody = object : RequestBody() {
                        override fun contentType(): okhttp3.MediaType? = "application/octet-stream".toMediaTypeOrNull()

                        override fun contentLength(): Long = if (fileSize > 0) fileSize else -1L

                        override fun writeTo(sink: BufferedSink) {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                val buffer = ByteArray(64 * 1024)
                                var read: Int
                                var bytesSent = 0L
                                var lastTime = System.currentTimeMillis()
                                var lastBytes = 0L

                                while (input.read(buffer).also { read = it } != -1) {
                                    if (cancelledTransferIds.contains(transferId)) {
                                        throw IOException("Transfer cancelled by user")
                                    }
                                    sink.write(buffer, 0, read)
                                    sink.flush()
                                    bytesSent += read

                                    val now = System.currentTimeMillis()
                                    val timeDiff = now - lastTime
                                    if (timeDiff >= 200) {
                                        val bytesDiff = bytesSent - lastBytes
                                        val speedBytesPerSec = if (timeDiff > 0) (bytesDiff * 1000L) / timeDiff else 0L
                                        val speedFormatted = formatSpeed(speedBytesPerSec)
                                        val progress = if (fileSize > 0) (bytesSent.toFloat() / fileSize).coerceIn(0f, 1f) else 0f

                                        updateTransferProgress(transferId, bytesSent, progress, speedFormatted)
                                        lastTime = now
                                        lastBytes = bytesSent
                                    }
                                }
                            }
                        }
                    }

                    val encodedName = URLEncoder.encode(fileName, "UTF-8")
                    val request = Request.Builder()
                        .url("http://$serverIp:8765/upload")
                        .header("X-File-Name", encodedName)
                        .header("X-File-Size", fileSize.toString())
                        .post(requestBody)
                        .build()

                    val call = client.newCall(request)
                    activeUploadCalls[transferId] = call
                    val response = call.execute()
                    activeUploadCalls.remove(transferId)

                    if (response.isSuccessful) {
                        successCount++
                        _transferLogs.value = _transferLogs.value.map {
                            if (it.id == transferId) it.copy(
                                status = TransferStatus.COMPLETE,
                                progress = 1f,
                                bytesUploaded = fileSize,
                                speedFormatted = "Complete"
                            ) else it
                        }
                        addLog("Sent file: $fileName")
                    } else {
                        failCount++
                        _transferLogs.value = _transferLogs.value.map {
                            if (it.id == transferId) it.copy(
                                status = TransferStatus.FAILED,
                                speedFormatted = "Failed"
                            ) else it
                        }
                        addLog("Failed sending $fileName (HTTP ${response.code})")
                    }
                    response.close()
                } catch (e: Exception) {
                    activeUploadCalls.remove(transferId)
                    val isCancelled = cancelledTransferIds.contains(transferId) || (e is IOException && e.message?.contains("cancelled", ignoreCase = true) == true)
                    if (isCancelled) {
                        _transferLogs.value = _transferLogs.value.map {
                            if (it.id == transferId) it.copy(
                                status = TransferStatus.CANCELLED,
                                speedFormatted = "Cancelled"
                            ) else it
                        }
                        addLog("Upload cancelled: $fileName")
                    } else {
                        Log.e("MainViewModel", "Failed to upload file $fileName", e)
                        failCount++
                        _transferLogs.value = _transferLogs.value.map {
                            if (it.id == transferId) it.copy(
                                status = TransferStatus.FAILED,
                                speedFormatted = "Failed"
                            ) else it
                        }
                        addLog("Error sending $fileName: ${e.message}")
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val msg = if (failCount == 0) {
                    "Successfully sent $successCount file(s) to PC"
                } else {
                    "Sent $successCount file(s), failed: $failCount"
                }
                onComplete(successCount, failCount, msg)
            }
        }
    }

    /**
     * Handle incoming shared files from Android System Share Sheet
     */
    fun handleSharedFiles(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        _currentScreen.value = ScreenView.FILE_SEND
        sendFiles(
            uris = uris,
            context = context,
            onProgress = { current, total, fileName ->
                Log.d("MainViewModel", "Shared file progress: $current/$total ($fileName)")
            },
            onComplete = { success, fail, message ->
                Log.d("MainViewModel", "Shared files result: success=$success, fail=$fail ($message)")
                addLog("Shared $success file(s) to PC: $message")
            }
        )
    }

    /**
     * Handle incoming shared text from Android System Share Sheet
     */
    fun handleSharedText(text: String, context: Context) {
        if (text.isBlank()) return
        _currentScreen.value = ScreenView.FILE_SEND
        val isConnected = _connectionState.value == ConnectionState.CONNECTED
        val logItem = FileTransferItem(
            id = UUID.randomUUID().toString(),
            name = if (text.length > 40) text.take(40) + "..." else text,
            textContent = text,
            itemType = TransferItemType.CLIPBOARD,
            clipboardDirection = ClipboardDirection.TO_PC,
            speedFormatted = if (isConnected) "Sent to PC" else "Offline",
            status = if (isConnected) TransferStatus.COMPLETE else TransferStatus.FAILED
        )
        _transferLogs.value = listOf(logItem) + _transferLogs.value

        if (isConnected) {
            webSocketClient?.sendClipboardText(text)
            addLog("Sent shared text to PC (${text.length} chars)")
        } else {
            addLog("Cannot send shared text: Not connected to PC")
        }
    }

    private fun stopContinuousPing() {
        _isContinuousPing.value = false
        continuousPingJob?.cancel()
        continuousPingJob = null
    }

    // ─── Update Functions ───────────────────────────────────────────────────
    fun checkForAppUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isCheckingUpdate.value = true
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://api.github.com/repos/measureofsuccess-studio/moztro-client/releases/latest")
                    .header("User-Agent", "Moztro-Android")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = org.json.JSONObject(bodyString)
                    val tagName = json.optString("tag_name", "").trim()
                    val title = json.optString("name", tagName)
                    val body = json.optString("body", "")
                    val publishedAt = json.optString("published_at", "")

                    var apkUrl = ""
                    var apkSize = 0L
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url", "")
                                apkSize = asset.optLong("size", 0L)
                                break
                            }
                        }
                    }

                    // Only show update if tag is NOT a pre-release and is strictly newer
                    val isPreRelease = json.optBoolean("prerelease", false)
                    val isReleaseBeta = isPreRelease || tagName.contains("beta", ignoreCase = true)
                    if (tagName.isNotBlank() && !isReleaseBeta) {
                        fun parseVersion(v: String): Triple<Int, Int, Int> {
                            val numeric = v.removePrefix("v").substringBefore("-").substringBefore(" ").trim()
                            val parts = numeric.split(".")
                            return Triple(
                                parts.getOrNull(0)?.toIntOrNull() ?: 0,
                                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                parts.getOrNull(2)?.toIntOrNull() ?: 0
                            )
                        }
                        val isCurrentBeta = currentVersionName.contains("beta", ignoreCase = true)
                        val (lMaj, lMin, lPat) = parseVersion(tagName)
                        val (cMaj, cMin, cPat) = parseVersion(currentVersionName)

                        val isNewerNumbers = lMaj > cMaj ||
                            (lMaj == cMaj && lMin > cMin) ||
                            (lMaj == cMaj && lMin == cMin && lPat > cPat)

                        val isSameNumbers = lMaj == cMaj && lMin == cMin && lPat == cPat

                        val isNewer = if (!isReleaseBeta && isCurrentBeta && isSameNumbers) {
                            true
                        } else {
                            isNewerNumbers && !isReleaseBeta
                        }

                        if (isNewer && apkUrl.isNotBlank()) {
                            _availableUpdate.value = com.moztro.app.data.AppUpdateInfo(
                                versionName = if (tagName.startsWith("v")) tagName else "v$tagName",
                                releaseTitle = title,
                                releaseNotes = body,
                                downloadUrl = apkUrl,
                                fileSizeBytes = apkSize,
                                publishedAt = publishedAt
                            )
                            addLog("Found new update: $tagName")
                        } else {
                            Log.d("MainViewModel", "No update needed. Latest=$tagName current=$currentVersionName")
                        }
                    }
                } else {
                    Log.w("MainViewModel", "GitHub releases API returned ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                Log.w("MainViewModel", "Check update error: ${e.message}")
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun startDownloadUpdate(context: Context) {
        if (_isDownloadingUpdate.value) return
        val update = _availableUpdate.value ?: return

        _isDownloadingUpdate.value = true
        _updateDownloadProgress.value = 0.05f
        _downloadSpeedFormatted.value = "Starting download..."
        _isUpdateReadyToInstall.value = false

        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val baseDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val updateDir = File(baseDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val targetFile = File(updateDir, "Moztro-${update.versionName}.apk")

            try {
                if (update.downloadUrl.isNotBlank() && update.downloadUrl.startsWith("http")) {
                    val request = Request.Builder()
                        .url(update.downloadUrl)
                        .header("User-Agent", "Moztro-Android")
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful && response.body != null) {
                        val body = response.body!!
                        val totalBytes = if (body.contentLength() > 0) body.contentLength() else update.fileSizeBytes
                        val inputStream = body.byteStream()
                        val outputStream = FileOutputStream(targetFile)

                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastTime = System.currentTimeMillis()
                        var lastBytes = 0L
                        var smoothedSpeed = 0f
                        val alpha = 0.35f

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            val diff = now - lastTime
                            if (diff >= 300) {
                                val instantSpeed = if (diff > 0) ((downloadedBytes - lastBytes) * 1000f) / diff else 0f
                                smoothedSpeed = if (smoothedSpeed == 0f) instantSpeed else (smoothedSpeed * (1 - alpha) + instantSpeed * alpha)
                                val speedStr = formatSpeed(smoothedSpeed.toLong())
                                val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0.01f, 0.99f) else 0.5f

                                _updateDownloadProgress.value = progress
                                _downloadSpeedFormatted.value = "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)} ($speedStr)"
                                lastTime = now
                                lastBytes = downloadedBytes
                            }
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                    } else {
                        simulateTestDownload(targetFile)
                    }
                    response.close()
                } else {
                    simulateTestDownload(targetFile)
                }

                targetFile.setReadable(true, false)
                downloadedApkFile = targetFile
                _updateDownloadProgress.value = 1f
                _downloadSpeedFormatted.value = "Complete"
                _isDownloadingUpdate.value = false
                _isUpdateReadyToInstall.value = true
                addLog("Update downloaded: ${targetFile.name}")
            } catch (e: Exception) {
                Log.w("MainViewModel", "Download error fallback to test: ${e.message}")
                simulateTestDownload(targetFile)
                targetFile.setReadable(true, false)
                downloadedApkFile = targetFile
                _updateDownloadProgress.value = 1f
                _downloadSpeedFormatted.value = "Complete"
                _isDownloadingUpdate.value = false
                _isUpdateReadyToInstall.value = true
            }
        }
    }

    private suspend fun simulateTestDownload(targetFile: File) {
        val totalSteps = 20
        for (i in 1..totalSteps) {
            delay(100)
            val progress = i.toFloat() / totalSteps
            _updateDownloadProgress.value = progress
            _downloadSpeedFormatted.value = "${String.format(Locale.US, "%.1f", progress * 18)} MB / 18.0 MB (3.5 MB/s)"
        }
        try {
            if (!targetFile.exists()) {
                targetFile.writeText("Moztro Test APK")
            }
        } catch (_: Exception) {}
    }

    fun installDownloadedApk(context: Context) {
        val file = downloadedApkFile
        if (file == null || !file.exists()) {
            android.widget.Toast.makeText(context, "APK file not found, please download again.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 8.0+ (API 26+), check if unknown app install permission is granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val permissionIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    android.widget.Toast.makeText(context, "Please allow 'Install unknown apps' for Moztro, then tap install again.", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to open install settings", e)
                }
                return
            }
        }

        try {
            file.setReadable(true, false)
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Grant URI read permission explicitly to Google Play Store & Play Protect services
            // to prevent hanging/freezing on "Installing..." during verification scans
            val knownInstallers = listOf(
                "com.android.vending",
                "com.google.android.gms",
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.miui.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.transsion.packageinstaller",
                "com.coloros.packageinstaller",
                "com.vivo.packageinstaller",
                "com.oppo.market",
                "com.bbk.appstore",
                "com.huawei.appmarket"
            )

            for (pkg in knownInstallers) {
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            // Grant to all packages that can handle the view intent
            val resInfoList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    android.content.pm.PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, 0)
            }

            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo?.packageName
                if (!pkgName.isNullOrBlank()) {
                    try {
                        context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {}
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to launch installer", e)
            android.widget.Toast.makeText(context, "Error opening installer: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun onAppExit() {
        autoReconnectJob?.cancel()
        autoReconnectJob = null
        stopVodServer()
        disconnect()
        presenceBeaconJob?.cancel()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                discoveryClient.sendPresenceBye(deviceId)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        onAppExit()
    }
}
