package com.moztro.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moztro.app.data.AppLanguage
import com.moztro.app.data.AppStrings
import com.moztro.app.data.OverdriveQuality
import com.moztro.app.data.OverdriveTouchMode
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextMuted
import com.moztro.app.ui.theme.MonoTextPrimary
import com.moztro.app.ui.theme.MonoWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    mouseSpeed: Int,
    scrollLines: Int,
    isFullscreen: Boolean,
    deleteFileOnClearHistory: Boolean = false,
    vodDefaultOn: Boolean = true,
    storageDirectoryPath: String = "",
    overdriveTouchMode: OverdriveTouchMode = OverdriveTouchMode.DIRECT_TOUCH,
    overdriveQuality: OverdriveQuality = OverdriveQuality.HIGH,
    overdriveAudioEnabled: Boolean = true,
    onAppLanguageChange: (AppLanguage) -> Unit = {},
    onMouseSpeedChange: (Int) -> Unit,
    onScrollLinesChange: (Int) -> Unit,
    onFullscreenToggle: (Boolean) -> Unit,
    onDeleteFileOnClearHistoryToggle: (Boolean) -> Unit = {},
    onVodDefaultOnToggle: (Boolean) -> Unit = {},
    onStorageDirectoryChange: (String) -> Unit = {},
    onOverdriveTouchModeChange: (OverdriveTouchMode) -> Unit = {},
    onOverdriveQualityChange: (OverdriveQuality) -> Unit = {},
    onOverdriveAudioEnabledToggle: (Boolean) -> Unit = {}
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val strings = AppStrings.forLanguage(appLanguage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section 1: LANGUAGE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionLanguage,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.settingsLanguageTitle,
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.settingsLanguageDesc,
                        color = MonoTextMuted,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = (appLanguage == lang)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MonoWhite else MonoDarkBg,
                                    shape = RectangleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MonoWhite else MonoBorder,
                                    shape = RectangleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onAppLanguageChange(lang)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) MonoDarkBg else MonoTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Section 2: POINTER & TOUCHPAD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionPointer,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 1: Pointer Speed (1 - 20)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.settingsPointerSpeed,
                            color = MonoTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.settingsPointerSpeedDesc,
                            color = MonoTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Value Badge
                    Box(
                        modifier = Modifier
                            .background(MonoDarkBg, shape = RectangleShape)
                            .border(1.dp, MonoBorder, shape = RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$mouseSpeed / 20",
                            color = MonoWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Slider(
                    value = mouseSpeed.toFloat(),
                    onValueChange = {
                        val newSpeed = it.toInt().coerceIn(1, 20)
                        if (newSpeed != mouseSpeed) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMouseSpeedChange(newSpeed)
                        }
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = MonoWhite,
                        activeTrackColor = MonoWhite,
                        inactiveTrackColor = MonoBorderLight,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 2: Scroll Lines (1 - 10)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.settingsScrollSpeed,
                            color = MonoTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.settingsScrollSpeedDesc,
                            color = MonoTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Value Badge
                    Box(
                        modifier = Modifier
                            .background(MonoDarkBg, shape = RectangleShape)
                            .border(1.dp, MonoBorder, shape = RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$scrollLines ${strings.settingsLinesSuffix}",
                            color = MonoWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Slider(
                    value = scrollLines.toFloat(),
                    onValueChange = {
                        val newLines = it.toInt().coerceIn(1, 10)
                        if (newLines != scrollLines) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onScrollLinesChange(newLines)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MonoWhite,
                        activeTrackColor = MonoWhite,
                        inactiveTrackColor = MonoBorderLight,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 3: DISPLAY & SYSTEM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionDisplay,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 3: Fullscreen Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onFullscreenToggle(!isFullscreen)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.settingsFullscreen,
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.settingsFullscreenDesc,
                        color = MonoTextMuted,
                        fontSize = 11.sp
                    )
                }

                MoztroSwitch(
                    checked = isFullscreen,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onFullscreenToggle(it)
                    }
                )
            }
        }

        // Section 4: FILE TRANSFER STORAGE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionStorage,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item: Delete original files when clearing history
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeleteFileOnClearHistoryToggle(!deleteFileOnClearHistory)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = strings.settingsDeleteHistory,
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.settingsDeleteHistoryDesc,
                        color = MonoTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                MoztroSwitch(
                    checked = deleteFileOnClearHistory,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeleteFileOnClearHistoryToggle(it)
                    }
                )
            }

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsDefaultStorage,
                            color = MonoTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.settingsDefaultStorageDesc,
                            color = MonoTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            val rawPath = uri.path ?: uri.toString()
                            val cleanPath = if (rawPath.contains(":")) {
                                val split = rawPath.split(":")
                                if (split.size > 1) "/storage/emulated/0/${split[1]}" else rawPath
                            } else {
                                rawPath
                            }
                            onStorageDirectoryChange(cleanPath)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(MonoDarkBg, shape = RectangleShape)
                            .border(1.dp, MonoBorder, shape = RectangleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                folderPickerLauncher.launch(null)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.settingsChangeButton,
                            color = MonoWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Path container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MonoDarkBg, shape = RectangleShape)
                        .border(1.dp, MonoBorder, shape = RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = storageDirectoryPath.ifBlank { "/storage/emulated/0/Moztro" },
                        color = MonoTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section 5: VIEW ON DEVICE (VOD)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionVod,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onVodDefaultOnToggle(!vodDefaultOn)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = strings.settingsVodDefault,
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.settingsVodDefaultDesc,
                        color = MonoTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                MoztroSwitch(
                    checked = vodDefaultOn,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onVodDefaultOnToggle(it)
                    }
                )
            }
        }

        // Section 6: OVERDRIVE (SCREEN MIRRORING & REMOTE CONTROL)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settingsSectionOverdrive,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 1: Touch Mode
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.settingsTouchMode,
                    color = MonoTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings.settingsTouchModeDesc,
                    color = MonoTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverdriveTouchMode.entries.forEach { mode ->
                        val isSelected = (overdriveTouchMode == mode)
                        val modeLabel = when (mode) {
                            OverdriveTouchMode.DIRECT_TOUCH -> strings.touchModeDirect
                            OverdriveTouchMode.TRACKPAD -> strings.touchModeTrackpad
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MonoWhite else MonoDarkBg,
                                    shape = RectangleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MonoWhite else MonoBorder,
                                    shape = RectangleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onOverdriveTouchModeChange(mode)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeLabel,
                                color = if (isSelected) MonoDarkBg else MonoTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 2: Streaming Quality
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.settingsStreamQuality,
                    color = MonoTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings.settingsStreamQualityDesc,
                    color = MonoTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverdriveQuality.entries.forEach { quality ->
                        val isSelected = (overdriveQuality == quality)
                        val qualityLabel = when (quality) {
                            OverdriveQuality.HIGH -> strings.qualityHigh
                            OverdriveQuality.FAST -> strings.qualityFast
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MonoWhite else MonoDarkBg,
                                    shape = RectangleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MonoWhite else MonoBorder,
                                    shape = RectangleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onOverdriveQualityChange(quality)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = qualityLabel,
                                color = if (isSelected) MonoDarkBg else MonoTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Item 3: Stream PC System Audio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOverdriveAudioEnabledToggle(!overdriveAudioEnabled)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = strings.settingsStreamAudio,
                        color = MonoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = strings.settingsStreamAudioDesc,
                        color = MonoTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                MoztroSwitch(
                    checked = overdriveAudioEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOverdriveAudioEnabledToggle(it)
                    }
                )
            }
        }
    }
}

/**
 * Minimalist Toggle Switch matching PC Electron app style
 */
@Composable
fun MoztroSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "SwitchThumbAnim"
    )

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 24.dp)
            .background(
                color = if (checked) MonoWhite else MonoSurface,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (checked) MonoWhite else MonoBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .background(
                    color = if (checked) MonoDarkBg else Color(0xFF888888),
                    shape = CircleShape
                )
        )
    }
}
