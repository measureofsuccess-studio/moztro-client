package com.moztro.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.sqrt
import com.moztro.app.R
import com.moztro.app.data.ClipboardDirection
import com.moztro.app.data.ConnectionState
import com.moztro.app.data.DiscoveredServer
import com.moztro.app.data.FileTransferItem
import com.moztro.app.data.OverdriveQuality
import com.moztro.app.data.OverdriveTouchMode
import com.moztro.app.data.PcCursorPosition
import com.moztro.app.data.PcDisplaySource
import com.moztro.app.data.ScreenView
import com.moztro.app.data.TransferItemType
import com.moztro.app.data.TransferStatus
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextPrimary
import com.moztro.app.ui.theme.MonoTextSecondary
import com.moztro.app.ui.theme.MonoTextSubtle
import com.moztro.app.ui.theme.MonoWhite
import com.moztro.app.viewmodel.MainViewModel
import java.io.File

/**
 * Open completed file using the system default / appropriate viewer application
 */
fun openFileWithDefaultApp(context: Context, item: FileTransferItem) {
    if (item.status != TransferStatus.COMPLETE) return

    val filePath = item.localFilePath
    val file = if (!filePath.isNullOrBlank()) {
        File(filePath)
    } else {
        null
    }

    if (file == null || !file.exists()) return

    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val ext = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open ${file.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {}
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val mouseSpeed by viewModel.mouseSpeed.collectAsState()
    val scrollLines by viewModel.scrollLines.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isTouchpadKeyboardVisible by viewModel.isTouchpadKeyboardVisible.collectAsState()
    val isMouseDragging by viewModel.isMouseDragging.collectAsState()
    val storageDirectoryPath by viewModel.storageDirectoryPath.collectAsState()
    val transferLogs by viewModel.transferLogs.collectAsState()
    val deleteFileOnClearHistory by viewModel.deleteFileOnClearHistory.collectAsState()
    val isVodEnabled by viewModel.isVodEnabled.collectAsState()
    val vodFtpUrl by viewModel.vodFtpUrl.collectAsState()
    val vodDefaultOn by viewModel.vodDefaultOn.collectAsState()
    val connectedServer by viewModel.connectedServer.collectAsState()
    val overdriveFrame by viewModel.overdriveFrame.collectAsState()
    val availableMonitors by viewModel.availableMonitors.collectAsState()
    val selectedMonitorId by viewModel.selectedMonitorId.collectAsState()
    val overdriveTouchMode by viewModel.overdriveTouchMode.collectAsState()
    val overdriveQuality by viewModel.overdriveQuality.collectAsState()
    val overdriveAudioEnabled by viewModel.overdriveAudioEnabled.collectAsState()
    val isAudioMuted by viewModel.isAudioMuted.collectAsState()
    val isOverdriveActive by viewModel.isOverdriveActive.collectAsState()
    val overdriveStreamWidth by viewModel.overdriveStreamWidth.collectAsState()
    val overdriveStreamHeight by viewModel.overdriveStreamHeight.collectAsState()
    val pcCursorPosition by viewModel.pcCursorPosition.collectAsState()
    var isOverdriveKeyboardVisible by remember { mutableStateOf(false) }
    val view = LocalView.current

    // Android native multi-file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(context, if (connectionState == ConnectionState.CONNECTED) "Sending ${uris.size} file(s)..." else "Not connected to PC", Toast.LENGTH_SHORT).show()
            viewModel.sendFiles(
                uris = uris,
                context = context,
                onProgress = { current, total, fileName ->
                    Toast.makeText(context, "Sending ($current/$total): $fileName", Toast.LENGTH_SHORT).show()
                },
                onComplete = { successCount, failCount, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Handle physical/gesture back button
    BackHandler(enabled = currentScreen != ScreenView.MAIN) {
        if (isOverdriveKeyboardVisible) {
            isOverdriveKeyboardVisible = false
        } else {
            viewModel.navigateBack()
        }
    }

    // Rotation animation when scanning/discovering
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    Scaffold(
        containerColor = MonoDarkBg,
        topBar = {
            if (currentScreen != ScreenView.KEYBOARD) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isFullscreen) PaddingValues(0.dp) else WindowInsets.statusBars.asPaddingValues())
                ) {
                    // Top Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MonoDarkBg)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Section: Back Button (if inside sub-page) + App Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentScreen != ScreenView.MAIN) {
                                // Back Button Icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { viewModel.navigateBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_back_button),
                                        contentDescription = "Back",
                                        colorFilter = ColorFilter.tint(MonoWhite),
                                        modifier = Modifier.size(18.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            // App Logo
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Moztro Logo",
                                modifier = Modifier.height(24.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Right Section: Drag Indicator + Keyboard Toggle + Overdrive Controls + Radar Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentScreen == ScreenView.OVERDRIVE) {
                                // Monitor Switcher Button with two-computer-screens icon
                                val currentMonitor = availableMonitors.find { it.id == selectedMonitorId }
                                    ?: availableMonitors.firstOrNull()

                                // Monitor icon-only button (tap cycles through monitors)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MonoSurface, shape = RectangleShape)
                                        .border(1.dp, MonoBorder, shape = RectangleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            if (availableMonitors.size > 1) {
                                                val currentIndex = availableMonitors.indexOfFirst { it.id == selectedMonitorId }
                                                val nextIndex = (currentIndex + 1) % availableMonitors.size
                                                viewModel.selectOverdriveMonitor(availableMonitors[nextIndex].id)
                                            } else {
                                                viewModel.selectOverdriveMonitor(selectedMonitorId ?: "")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_screen_select),
                                        contentDescription = "Monitor Selected",
                                        colorFilter = ColorFilter.tint(MonoWhite),
                                        modifier = Modifier.size(18.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                // Audio Mute / Wave Sound Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isAudioMuted) MonoSurface else MonoWhite,
                                            shape = RectangleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isAudioMuted) MonoBorder else MonoWhite,
                                            shape = RectangleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            viewModel.toggleAudioMute()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(
                                            id = if (isAudioMuted) R.drawable.ic_sound_mute else R.drawable.ic_sound_wave
                                        ),
                                        contentDescription = if (isAudioMuted) "Audio Muted" else "Audio Active",
                                        colorFilter = ColorFilter.tint(
                                            if (isAudioMuted) MonoTextSecondary else MonoDarkBg
                                        ),
                                        modifier = Modifier.size(18.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                // Keyboard Toggle Button (for Overdrive Screen)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isOverdriveKeyboardVisible) MonoWhite else MonoSurface,
                                            shape = RectangleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isOverdriveKeyboardVisible) MonoWhite else MonoBorder,
                                            shape = RectangleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            isOverdriveKeyboardVisible = !isOverdriveKeyboardVisible
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_keyboard),
                                        contentDescription = "Toggle Keyboard",
                                        colorFilter = ColorFilter.tint(
                                            if (isOverdriveKeyboardVisible) MonoDarkBg else MonoWhite
                                        ),
                                        modifier = Modifier.size(20.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            if (currentScreen == ScreenView.TOUCHPAD) {
                                if (isMouseDragging) {
                                    Box(
                                        modifier = Modifier
                                            .background(MonoWhite, shape = RectangleShape)
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "DRAG",
                                            color = MonoDarkBg,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Keyboard Toggle Button (On/Off for Touchpad Screen)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isTouchpadKeyboardVisible) MonoWhite else MonoSurface,
                                            shape = RectangleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isTouchpadKeyboardVisible) MonoWhite else MonoBorder,
                                            shape = RectangleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            viewModel.toggleTouchpadKeyboard()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_keyboard),
                                        contentDescription = "Toggle Keyboard",
                                        colorFilter = ColorFilter.tint(
                                            if (isTouchpadKeyboardVisible) MonoDarkBg else MonoWhite
                                        ),
                                        modifier = Modifier.size(20.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            // Radar Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.onRadarClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_radar),
                                    contentDescription = "Radar Pairing",
                                    colorFilter = ColorFilter.tint(MonoWhite),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .alpha(
                                            when (connectionState) {
                                                ConnectionState.CONNECTED -> 1f
                                                ConnectionState.DISCOVERING, ConnectionState.CONNECTING -> 0.85f
                                                else -> 0.4f
                                            }
                                        )
                                        .rotate(
                                            if (connectionState == ConnectionState.DISCOVERING || connectionState == ConnectionState.CONNECTING) {
                                                rotationAngle
                                            } else {
                                                0f
                                            }
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // Thin 1px separator line
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MonoBorder
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MonoDarkBg)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "ScreenSwitch"
            ) { targetScreen ->
                when (targetScreen) {
                    ScreenView.MAIN -> {
                        MainMenuView(
                            onWakeOnLanClick = {
                                viewModel.executeWakeOnLan { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShutdownMenuClick = {
                                viewModel.navigateTo(ScreenView.POWER_MENU)
                            },
                            onInputMenuClick = {
                                viewModel.navigateTo(ScreenView.TOUCHPAD)
                            },
                            onKeyboardMenuClick = {
                                viewModel.navigateTo(ScreenView.KEYBOARD)
                            },
                            onSettingMenuClick = {
                                viewModel.navigateTo(ScreenView.SETTINGS)
                            },
                            onFileSendClick = {
                                viewModel.navigateTo(ScreenView.FILE_SEND)
                            },
                            onVodClick = {
                                viewModel.navigateTo(ScreenView.VOD)
                            },
                            onOverdriveClick = {
                                viewModel.navigateTo(ScreenView.OVERDRIVE)
                            },
                            onAboutClick = {
                                viewModel.navigateTo(ScreenView.ABOUT)
                            }
                        )
                    }
                    ScreenView.POWER_MENU -> {
                        PowerMenuView(
                            onShutdown = {
                                viewModel.executePowerAction("SHUTDOWN") { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRestart = {
                                viewModel.executePowerAction("RESTART") { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSleep = {
                                viewModel.executePowerAction("SLEEP") { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ScreenView.KEYBOARD -> {
                        Keyboard65Screen(
                            onBackClick = { viewModel.navigateBack() },
                            onKeyPress = { key, modifiers -> viewModel.sendKey(key, modifiers) },
                            onModifierChange = { mod, isDown -> viewModel.setModifier(mod, isDown) },
                            onReleaseAllModifiers = { viewModel.releaseAllModifiers() }
                        )
                    }
                    ScreenView.TOUCHPAD -> {
                        TouchpadScreen(
                            mouseSpeed = mouseSpeed,
                            scrollLines = scrollLines,
                            isDragging = isMouseDragging,
                            isKeyboardVisible = isTouchpadKeyboardVisible,
                            onDraggingChange = { viewModel.setMouseDragging(it) },
                            onMouseMove = { dx, dy -> viewModel.sendMouseMove(dx, dy) },
                            onMouseClick = { button -> viewModel.sendMouseClick(button) },
                            onMouseDown = { button -> viewModel.sendMouseDown(button) },
                            onMouseUp = { button -> viewModel.sendMouseUp(button) },
                            onMouseScroll = { delta -> viewModel.sendMouseScroll(delta) },
                            onKeyPress = { key, modifiers -> viewModel.sendKey(key, modifiers) },
                            onModifierChange = { mod, isDown -> viewModel.setModifier(mod, isDown) },
                            onReleaseAllModifiers = { viewModel.releaseAllModifiers() }
                        )
                    }
                    ScreenView.SETTINGS -> {
                        SettingsScreen(
                            mouseSpeed = mouseSpeed,
                            scrollLines = scrollLines,
                            isFullscreen = isFullscreen,
                            deleteFileOnClearHistory = deleteFileOnClearHistory,
                            vodDefaultOn = vodDefaultOn,
                            storageDirectoryPath = storageDirectoryPath,
                            overdriveTouchMode = overdriveTouchMode,
                            overdriveQuality = overdriveQuality,
                            overdriveAudioEnabled = overdriveAudioEnabled,
                            onMouseSpeedChange = { viewModel.setMouseSpeed(it) },
                            onScrollLinesChange = { viewModel.setScrollLines(it) },
                            onFullscreenToggle = { viewModel.setFullscreen(it) },
                            onDeleteFileOnClearHistoryToggle = { viewModel.setDeleteFileOnClearHistory(it) },
                            onVodDefaultOnToggle = { viewModel.setVodDefaultOn(it) },
                            onStorageDirectoryChange = { viewModel.updateStorageDirectory(it) },
                            onOverdriveTouchModeChange = { viewModel.setOverdriveTouchMode(it) },
                            onOverdriveQualityChange = { viewModel.setOverdriveQuality(it) },
                            onOverdriveAudioEnabledToggle = { viewModel.setOverdriveAudioEnabled(it) }
                        )
                    }
                    ScreenView.VOD -> {
                        VodScreen(
                            connectionState = connectionState,
                            connectedServer = connectedServer,
                            isVodEnabled = isVodEnabled,
                            vodFtpUrl = vodFtpUrl,
                            onToggleVod = { viewModel.toggleVod() }
                        )
                    }
                    ScreenView.OVERDRIVE -> {
                        OverdriveScreen(
                            connectionState = connectionState,
                            connectedServer = connectedServer,
                            overdriveFrame = overdriveFrame,
                            availableMonitors = availableMonitors,
                            selectedMonitorId = selectedMonitorId,
                            overdriveTouchMode = overdriveTouchMode,
                            isAudioMuted = isAudioMuted,
                            isKeyboardOpen = isOverdriveKeyboardVisible,
                            streamWidth = overdriveStreamWidth,
                            streamHeight = overdriveStreamHeight,
                            pcCursorPosition = pcCursorPosition,
                            onStartSession = { viewModel.startOverdriveSession() },
                            onStopSession = { viewModel.stopOverdriveSession() },
                            onSelectMonitor = { viewModel.selectOverdriveMonitor(it) },
                            onToggleAudioMute = { viewModel.toggleAudioMute() },
                            onCloseKeyboard = { isOverdriveKeyboardVisible = false },
                            onConnect = { viewModel.startDiscoveryAndConnect() },
                            onTouchEvent = { action, x, y, dx, dy, button, deltaY ->
                                viewModel.sendOverdriveTouch(action, x, y, dx, dy, button, deltaY)
                            },
                            onSendKey = { key, mods -> viewModel.sendKey(key, mods) },
                            onSendText = { text ->
                                for (char in text) {
                                    viewModel.sendKey(char.toString())
                                }
                            }
                        )
                    }
                    ScreenView.FILE_SEND -> {
                        FileSendView(
                            transferLogs = transferLogs,
                            onCancelTransfer = { transferId ->
                                viewModel.cancelTransfer(transferId)
                            },
                            onClearLogs = { idsToClear ->
                                viewModel.clearTransferLogs(idsToClear)
                            },
                            onSelectFileClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            onSendClipboardClick = {
                                viewModel.sendClipboard(context) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPasteClipboardClick = {
                                viewModel.requestPasteClipboard { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopyTextToClipboard = { text ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Moztro Clipboard", text)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    ScreenView.ABOUT -> {
                        AboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuView(
    onWakeOnLanClick: () -> Unit,
    onShutdownMenuClick: () -> Unit,
    onInputMenuClick: () -> Unit,
    onKeyboardMenuClick: () -> Unit,
    onSettingMenuClick: () -> Unit,
    onFileSendClick: () -> Unit,
    onVodClick: () -> Unit,
    onOverdriveClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Row 1: 2 Sharp Square Menu Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu 1: Wake On Lan (Kotak tanpa rounded / lancip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onWakeOnLanClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_smart),
                        contentDescription = "Wake On Lan",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Wake On Lan",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Menu 2: Shutting Down (Kotak tanpa rounded / lancip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onShutdownMenuClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_power_button),
                        contentDescription = "Shutting Down",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Shutting Down",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Menu Input (below Wake On Lan) & Menu Keyboard (below Shutting Down)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu 3: Input / Mouse (Kotak tanpa rounded / lancip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onInputMenuClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mouse),
                        contentDescription = "Input",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Input",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Menu 4: Keyboard (Kotak tanpa rounded / lancip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onKeyboardMenuClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_keyboard),
                        contentDescription = "Keyboard",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Keyboard",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Menu Setting (below Input) & Menu File Send (below Keyboard)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu 5: Setting (Kotak tanpa rounded / lancip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onSettingMenuClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_setting),
                        contentDescription = "Setting",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Setting",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Menu 6: File Send (Kotak tanpa rounded / lancip, below Keyboard)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onFileSendClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_file_send),
                        contentDescription = "File Send",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "File Send",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 4: Menu VOD (View On Device) below Setting & File Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu 7: VOD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onVodClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_vod),
                        contentDescription = "VOD",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VOD",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Menu 8: Overdrive
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onOverdriveClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_overdrive),
                        contentDescription = "Overdrive",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Overdrive",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 5: Menu About (under VOD)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu 9: About
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .clickable { onAboutClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_about),
                        contentDescription = "About",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "About",
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Empty spacer to balance 2-column grid
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PowerMenuView(
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onSleep: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3 Small Bordered Buttons Centered in 1 Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Shutting Down
            PowerActionButton(
                text = "Shutting Down",
                onClick = onShutdown
            )

            // Button 2: Restart
            PowerActionButton(
                text = "Restart",
                onClick = onRestart
            )

            // Button 3: Sleep
            PowerActionButton(
                text = "Sleep",
                onClick = onSleep
            )
        }
    }
}

@Composable
fun PowerActionButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(MonoSurface, shape = RectangleShape)
            .border(1.dp, MonoBorderLight, shape = RectangleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MonoTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun FileSendView(
    transferLogs: List<FileTransferItem>,
    onCancelTransfer: (String) -> Unit,
    onClearLogs: (Set<String>) -> Unit,
    onSelectFileClick: () -> Unit,
    onSendClipboardClick: () -> Unit,
    onPasteClipboardClick: () -> Unit,
    onCopyTextToClipboard: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedItemIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedItemIds.isNotEmpty()

    // Auto-clean selectedItemIds if logs change or are removed
    LaunchedEffect(transferLogs) {
        val validIds = transferLogs.map { it.id }.toSet()
        selectedItemIds = selectedItemIds.filter { validIds.contains(it) }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1:1 Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorderLight, shape = RectangleShape)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header: Select All + Selected Count + Cancel selection (only when selection mode is active)
                if (isSelectionMode) {
                    val allSelected = transferLogs.isNotEmpty() && selectedItemIds.size == transferLogs.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonoDarkBg)
                            .border(1.dp, MonoBorderLight.copy(alpha = 0.6f), RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Select All Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    selectedItemIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        transferLogs.map { it.id }.toSet()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .border(1.dp, if (allSelected) MonoWhite else MonoBorderLight, RectangleShape)
                                    .background(if (allSelected) MonoWhite else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (allSelected) {
                                    Text(
                                        text = "✓",
                                        color = MonoDarkBg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select All",
                                color = MonoTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Selected Count & Cancel Exit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${selectedItemIds.size} selected",
                                color = MonoTextSecondary,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(1.dp, MonoBorderLight, RectangleShape)
                                    .clickable { selectedItemIds = emptySet() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = MonoTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Scrollable Transfer Logs
                if (transferLogs.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transferLogs, key = { it.id }) { item ->
                            val isSelected = selectedItemIds.contains(item.id)
                            if (item.itemType == TransferItemType.CLIPBOARD) {
                                ClipboardTransferRow(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onRowClick = {
                                        selectedItemIds = if (isSelected) {
                                            selectedItemIds - item.id
                                        } else {
                                            selectedItemIds + item.id
                                        }
                                    },
                                    onLongClick = {
                                        selectedItemIds = selectedItemIds + item.id
                                    },
                                    onCopy = { text ->
                                        onCopyTextToClipboard(text)
                                    }
                                )
                            } else {
                                FileTransferRow(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onRowClick = {
                                        selectedItemIds = if (isSelected) {
                                            selectedItemIds - item.id
                                        } else {
                                            selectedItemIds + item.id
                                        }
                                    },
                                    onLongClick = {
                                        selectedItemIds = selectedItemIds + item.id
                                    },
                                    onOpenFile = { file ->
                                        openFileWithDefaultApp(context, file)
                                    },
                                    onCancel = { onCancelTransfer(item.id) }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Bottom Clear Button: Only visible when selection mode is active
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonoDarkBg)
                            .border(1.dp, MonoBorderLight.copy(alpha = 0.6f), RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MonoSurface, RectangleShape)
                                .border(1.dp, MonoBorderLight, RectangleShape)
                                .clickable {
                                    onClearLogs(selectedItemIds)
                                    selectedItemIds = emptySet()
                                }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Clear (${selectedItemIds.size})",
                                color = MonoWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Row: Send Clipboard | Select File | Paste Clipboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileActionButton(
                text = "Send\nClipboard",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onSendClipboardClick
            )

            FileActionButton(
                text = "Select\nFile",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onSelectFileClick
            )

            FileActionButton(
                text = "Paste\nClipboard",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onPasteClipboardClick
            )
        }
    }
}

@Composable
fun FileActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(MonoSurface, shape = RectangleShape)
            .border(1.dp, MonoBorderLight, shape = RectangleShape)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MonoTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardTransferRow(
    item: FileTransferItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onRowClick: () -> Unit,
    onLongClick: () -> Unit,
    onCopy: (String) -> Unit
) {
    val view = LocalView.current
    val text = item.textContent ?: item.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MonoSurface else MonoDarkBg, shape = RectangleShape)
            .border(
                width = 1.dp,
                color = if (isSelected) MonoWhite else MonoBorderLight.copy(alpha = 0.5f),
                shape = RectangleShape
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onRowClick()
                    } else {
                        onCopy(text)
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox indicator (only shown when selection mode is active)
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(1.dp, if (isSelected) MonoWhite else MonoBorderLight, RectangleShape)
                    .background(if (isSelected) MonoWhite else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = MonoDarkBg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Text Content (Max 3 Lines with Ellipsis)
            Text(
                text = text,
                color = MonoTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Status label
            val isToPc = item.clipboardDirection == ClipboardDirection.TO_PC
            val (statusColor, statusText) = when (item.status) {
                TransferStatus.COMPLETE -> Pair(
                    MonoTextSecondary,
                    if (isToPc) "Sent to PC" else "Copied to Phone"
                )
                TransferStatus.FAILED -> Pair(Color(0xFFFF5252), "Failed")
                TransferStatus.CANCELLED -> Pair(MonoTextSecondary, "Cancelled")
                else -> Pair(MonoTextSecondary, "Pending")
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Direction Arrow Indicator Box on the Right
        val isToPc = item.clipboardDirection == ClipboardDirection.TO_PC
        val arrowSymbol = if (isToPc) "↑" else "↓"
        val arrowLabel = if (isToPc) "HP→PC" else "PC→HP"

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorderLight, shape = RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arrowSymbol,
                    color = MonoWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = arrowLabel,
                color = MonoTextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTransferRow(
    item: FileTransferItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onRowClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenFile: (FileTransferItem) -> Unit,
    onCancel: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MonoSurface else MonoDarkBg, shape = RectangleShape)
            .border(
                width = 1.dp,
                color = if (isSelected) MonoWhite else MonoBorderLight.copy(alpha = 0.5f),
                shape = RectangleShape
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onRowClick()
                    } else {
                        onOpenFile(item)
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox indicator (only shown when selection mode is active)
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(1.dp, if (isSelected) MonoWhite else MonoBorderLight, RectangleShape)
                    .background(if (isSelected) MonoWhite else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = MonoDarkBg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: File Name + Status or Cancel 'X' Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    color = MonoTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                if (item.status == TransferStatus.UPLOADING) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.dp, MonoBorderLight, shape = RectangleShape)
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = MonoTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val (statusColor, statusText) = when (item.status) {
                        TransferStatus.COMPLETE -> Pair(MonoTextPrimary, "Complete")
                        TransferStatus.CANCELLED -> Pair(MonoTextSecondary, "Cancelled")
                        TransferStatus.FAILED -> Pair(Color(0xFFFF5252), "Failed")
                        else -> Pair(MonoTextPrimary, "")
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Row 2: Progress Line
            val progressVal = if (item.status == TransferStatus.COMPLETE) 1f else item.progress
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MonoTextPrimary,
                trackColor = MonoBorderLight.copy(alpha = 0.4f),
                strokeCap = StrokeCap.Square
            )

            // Row 3: Percentage & Upload Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pct = (progressVal * 100).toInt()
                Text(
                    text = "$pct%",
                    color = MonoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )

                Text(
                    text = item.speedFormatted,
                    color = MonoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun VodScreen(
    connectionState: ConnectionState,
    connectedServer: DiscoveredServer?,
    isVodEnabled: Boolean,
    vodFtpUrl: String?,
    onToggleVod: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val serverName = connectedServer?.hostname ?: "PC"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Connection Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS",
                        color = MonoTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isConnected) "Connected to $serverName" else "Not Connected",
                            color = if (isConnected) MonoTextPrimary else Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MonoBorderLight.copy(alpha = 0.4f))

                // Row 2: Description Text (as specified by user)
                Text(
                    text = "Access and manage local device storage directly via PC.",
                    color = MonoTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Row 3: FTP URL details if active
                if (isVodEnabled && vodFtpUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonoDarkBg, shape = RectangleShape)
                            .border(1.dp, MonoBorderLight, shape = RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "FTP ADDRESS",
                                color = MonoTextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = vodFtpUrl,
                                color = MonoWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Toggle Button: "Turn Off" (when active) / "Turn On" (when inactive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(
                    if (isVodEnabled) MonoWhite else MonoSurface,
                    shape = RectangleShape
                )
                .border(
                    1.dp,
                    if (isVodEnabled) MonoWhite else MonoBorder,
                    shape = RectangleShape
                )
                .clickable { onToggleVod() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isVodEnabled) "Turn Off" else "Turn On",
                color = if (isVodEnabled) MonoDarkBg else MonoTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun mapToPcCoordinates(
    touchX: Float,
    touchY: Float,
    canvasW: Int,
    canvasH: Int,
    streamW: Int,
    streamH: Int
): Pair<Float, Float> {
    if (canvasW <= 0 || canvasH <= 0 || streamW <= 0 || streamH <= 0) {
        return Pair(touchX, touchY)
    }
    val scale = minOf(canvasW.toFloat() / streamW.toFloat(), canvasH.toFloat() / streamH.toFloat())
    val renderedW = streamW * scale
    val renderedH = streamH * scale
    val offsetX = (canvasW - renderedW) / 2f
    val offsetY = (canvasH - renderedH) / 2f

    val clampedX = (touchX - offsetX).coerceIn(0f, renderedW)
    val clampedY = (touchY - offsetY).coerceIn(0f, renderedH)

    val pcX = (clampedX / scale).coerceIn(0f, streamW.toFloat())
    val pcY = (clampedY / scale).coerceIn(0f, streamH.toFloat())
    return Pair(pcX, pcY)
}

fun mapPcToCanvasCoordinates(
    pcX: Float,
    pcY: Float,
    pcScreenW: Float,
    pcScreenH: Float,
    canvasW: Int,
    canvasH: Int,
    streamW: Int,
    streamH: Int
): Pair<Float, Float> {
    if (canvasW <= 0 || canvasH <= 0 || pcScreenW <= 0f || pcScreenH <= 0f) {
        return Pair(0f, 0f)
    }
    val effectiveStreamW = if (streamW > 0) streamW.toFloat() else pcScreenW
    val effectiveStreamH = if (streamH > 0) streamH.toFloat() else pcScreenH
    val scale = minOf(canvasW.toFloat() / effectiveStreamW, canvasH.toFloat() / effectiveStreamH)
    val renderedW = effectiveStreamW * scale
    val renderedH = effectiveStreamH * scale
    val offsetX = (canvasW - renderedW) / 2f
    val offsetY = (canvasH - renderedH) / 2f

    val normX = (pcX / pcScreenW).coerceIn(0f, 1f)
    val normY = (pcY / pcScreenH).coerceIn(0f, 1f)

    val canvasX = offsetX + normX * renderedW
    val canvasY = offsetY + normY * renderedH
    return Pair(canvasX, canvasY)
}

@Composable
fun OverdriveScreen(
    connectionState: ConnectionState,
    connectedServer: DiscoveredServer?,
    overdriveFrame: Bitmap?,
    availableMonitors: List<PcDisplaySource>,
    selectedMonitorId: String?,
    overdriveTouchMode: OverdriveTouchMode,
    isAudioMuted: Boolean,
    isKeyboardOpen: Boolean,
    streamWidth: Int,
    streamHeight: Int,
    pcCursorPosition: PcCursorPosition? = null,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onSelectMonitor: (String) -> Unit,
    onToggleAudioMute: () -> Unit,
    onCloseKeyboard: () -> Unit,
    onConnect: () -> Unit,
    onTouchEvent: (action: String, x: Float?, y: Float?, dx: Float?, dy: Float?, button: String?, deltaY: Int?) -> Unit,
    onSendKey: (String, List<String>) -> Unit,
    onSendText: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    var textInput by remember { mutableStateOf("") }

    val isConnected = connectionState == ConnectionState.CONNECTED

    // Cursor overlay state (trackpad mode)
    var cursorVisible by remember { mutableStateOf(false) }
    var cursorSmoothedX by remember { mutableFloatStateOf(0f) }
    var cursorSmoothedY by remember { mutableFloatStateOf(0f) }

    // Start streaming session on enter, stop on exit & manage full sensor auto-rotation
    DisposableEffect(isConnected) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        if (isConnected) {
            onStartSession()
        }
        onDispose {
            onStopSession()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isConnected && overdriveFrame != null) {
            var canvasWidth by remember { mutableStateOf(0) }
            var canvasHeight by remember { mutableStateOf(0) }

            // Sync cursor overlay position with actual PC cursor coordinates
            LaunchedEffect(pcCursorPosition, canvasWidth, canvasHeight, streamWidth, streamHeight) {
                val pos = pcCursorPosition ?: return@LaunchedEffect
                val (targetX, targetY) = mapPcToCanvasCoordinates(
                    pcX = pos.x,
                    pcY = pos.y,
                    pcScreenW = pos.screenW,
                    pcScreenH = pos.screenH,
                    canvasW = canvasWidth,
                    canvasH = canvasHeight,
                    streamW = streamWidth,
                    streamH = streamHeight
                )
                cursorSmoothedX = targetX
                cursorSmoothedY = targetY
                cursorVisible = true
            }

            // Dead zone threshold in pixels
            val deadZonePx = with(density) { 4.dp.toPx() }
            // Smoothing factor: 0 = lag, 1 = no smoothing
            val smoothAlpha = 0.75f
            // Speed multiplier for trackpad
            val trackpadSpeed = 1.5f

            Image(
                bitmap = overdriveFrame.asImageBitmap(),
                contentDescription = "Overdrive PC Mirror",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasWidth = it.width
                        canvasHeight = it.height
                        // If no PC position yet, default to center
                        if (cursorSmoothedX == 0f && cursorSmoothedY == 0f) {
                            cursorSmoothedX = it.width / 2f
                            cursorSmoothedY = it.height / 2f
                        }
                    }
                    // Direct touch mode gestures
                    .pointerInput(overdriveTouchMode, streamWidth, streamHeight) {
                        if (overdriveTouchMode == OverdriveTouchMode.DIRECT_TOUCH) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val (pcX, pcY) = mapToPcCoordinates(
                                        offset.x, offset.y,
                                        canvasWidth, canvasHeight,
                                        streamWidth, streamHeight
                                    )
                                    onTouchEvent("DOWN", pcX, pcY, null, null, "LEFT", null)
                                    tryAwaitRelease()
                                    onTouchEvent("UP", pcX, pcY, null, null, "LEFT", null)
                                },
                                onLongPress = { offset ->
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    val (pcX, pcY) = mapToPcCoordinates(
                                        offset.x, offset.y,
                                        canvasWidth, canvasHeight,
                                        streamWidth, streamHeight
                                    )
                                    onTouchEvent("CLICK", pcX, pcY, null, null, "RIGHT", null)
                                }
                            )
                        }
                    }
                    .pointerInput(overdriveTouchMode, streamWidth, streamHeight) {
                        if (overdriveTouchMode == OverdriveTouchMode.DIRECT_TOUCH) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val (pcX, pcY) = mapToPcCoordinates(
                                        offset.x, offset.y,
                                        canvasWidth, canvasHeight,
                                        streamWidth, streamHeight
                                    )
                                    onTouchEvent("DOWN", pcX, pcY, null, null, "LEFT", null)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val (pcX, pcY) = mapToPcCoordinates(
                                        change.position.x, change.position.y,
                                        canvasWidth, canvasHeight,
                                        streamWidth, streamHeight
                                    )
                                    onTouchEvent("MOVE", pcX, pcY, null, null, null, null)
                                },
                                onDragEnd = {
                                    onTouchEvent("UP", null, null, null, null, "LEFT", null)
                                },
                                onDragCancel = {
                                    onTouchEvent("UP", null, null, null, null, "LEFT", null)
                                }
                            )
                        }
                    }
                    // Trackpad mode: full multi-touch gesture handling
                    .pointerInput(overdriveTouchMode) {
                        if (overdriveTouchMode == OverdriveTouchMode.TRACKPAD) {
                            awaitEachGesture {
                                // Wait for first finger down
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                firstDown.consume()

                                var isDragging = false
                                var accumulatedX = 0f
                                var accumulatedY = 0f
                                var isScrolling = false

                                cursorVisible = true

                                // Track active pointers for multi-touch detection
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    val fingerCount = activePointers.size

                                    // Check if any pointer lifted (gesture ended)
                                    val anyUp = event.changes.any { it.changedToUp() }

                                    if (fingerCount == 0 || anyUp && !isDragging) {
                                        // All lifted — was it a tap?
                                        val totalMoved = sqrt(accumulatedX * accumulatedX + accumulatedY * accumulatedY)
                                        if (!isDragging && totalMoved < deadZonePx) {
                                            if (fingerCount == 2 || event.changes.count { it.changedToUp() } >= 2) {
                                                // Two-finger tap = right click
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                onTouchEvent("CLICK", null, null, null, null, "RIGHT", null)
                                            } else {
                                                // Single tap = left click
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                onTouchEvent("CLICK", null, null, null, null, "LEFT", null)
                                            }
                                        } else if (isDragging) {
                                            onTouchEvent("UP", null, null, null, null, "LEFT", null)
                                        }
                                        isScrolling = false
                                        isDragging = false
                                        accumulatedX = 0f
                                        accumulatedY = 0f
                                        break
                                    }

                                    if (fingerCount >= 2) {
                                        // Two-finger drag = scroll
                                        val primaryChange = activePointers.first()
                                        val dy = primaryChange.position.y - primaryChange.previousPosition.y
                                        val absMovedY = abs(dy)
                                        if (absMovedY > deadZonePx / 2f) {
                                            isScrolling = true
                                            // Negative dy = scroll up, positive = scroll down
                                            val scrollDelta = -(dy * 3).toInt().coerceIn(-500, 500)
                                            onTouchEvent("SCROLL", null, null, null, null, null, scrollDelta)
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (fingerCount == 1) {
                                        // Single finger drag = mouse move
                                        val change = activePointers.first()
                                        val rawDx = change.position.x - change.previousPosition.x
                                        val rawDy = change.position.y - change.previousPosition.y
                                        accumulatedX += rawDx
                                        accumulatedY += rawDy

                                        val totalMoved = sqrt(accumulatedX * accumulatedX + accumulatedY * accumulatedY)
                                        if (totalMoved > deadZonePx || isDragging) {
                                            if (!isDragging) isDragging = true

                                            // Apply dead zone filter per-delta
                                            if (abs(rawDx) > deadZonePx / 4f || abs(rawDy) > deadZonePx / 4f) {
                                                val scaledDx = rawDx * trackpadSpeed
                                                val scaledDy = rawDy * trackpadSpeed

                                                // Update cursor position with smoothing
                                                val targetX = (cursorSmoothedX + scaledDx).coerceIn(0f, canvasWidth.toFloat())
                                                val targetY = (cursorSmoothedY + scaledDy).coerceIn(0f, canvasHeight.toFloat())
                                                cursorSmoothedX = cursorSmoothedX + (targetX - cursorSmoothedX) * smoothAlpha
                                                cursorSmoothedY = cursorSmoothedY + (targetY - cursorSmoothedY) * smoothAlpha

                                                onTouchEvent("MOVE", null, null, scaledDx, scaledDy, null, null)
                                            }
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
            )

            // Cursor dot overlay (only in trackpad mode) — uses offset{IntOffset} for pixel-accurate absolute positioning
            if (overdriveTouchMode == OverdriveTouchMode.TRACKPAD && cursorVisible) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val dotSizePx = with(density) { 10.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .offset {
                                IntOffset(
                                    x = (cursorSmoothedX - dotSizePx / 2f).toInt().coerceAtLeast(0),
                                    y = (cursorSmoothedY - dotSizePx / 2f).toInt().coerceAtLeast(0)
                                )
                            }
                            .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                            .background(Color.White, shape = CircleShape)
                            .border(1.5.dp, Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    )
                }
            }
        } else {
            // Placeholder when connecting / loading stream
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_overdrive),
                    contentDescription = "Overdrive",
                    colorFilter = ColorFilter.tint(MonoWhite),
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isConnected) "Starting Overdrive Stream..." else "Not Connected to PC",
                    color = if (isConnected) MonoWhite else Color(0xFFFF5252),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isConnected) "Mirroring PC screen & system audio in real-time" else "Connect to PC from the Home screen to start screen mirroring.",
                    color = MonoTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (!isConnected) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .background(MonoWhite, shape = RectangleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onConnect()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CONNECT TO PC",
                            color = MonoDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Soft Keyboard Input Bar Overlay (if toggled)
        if (isKeyboardOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MonoSurface)
                    .border(1.dp, MonoBorder, RectangleShape)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = textInput,
                        onValueChange = { newValue ->
                            if (newValue.length > textInput.length) {
                                val added = newValue.substring(textInput.length)
                                onSendText(added)
                            } else if (newValue.length < textInput.length) {
                                onSendKey("BACKSPACE", emptyList())
                            }
                            textInput = newValue
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MonoWhite,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .background(MonoDarkBg, RectangleShape)
                            .border(1.dp, MonoBorder, RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .background(MonoWhite, RectangleShape)
                            .clickable {
                                onSendKey("ENTER", emptyList())
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter",
                            color = MonoDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(MonoDarkBg, RectangleShape)
                            .border(1.dp, MonoBorder, RectangleShape)
                            .clickable {
                                onCloseKeyboard()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = MonoWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
