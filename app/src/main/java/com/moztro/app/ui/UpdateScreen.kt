package com.moztro.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moztro.app.R
import com.moztro.app.data.AppLanguage
import com.moztro.app.data.AppStrings
import com.moztro.app.data.AppUpdateInfo
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextMuted
import com.moztro.app.ui.theme.MonoTextSecondary
import com.moztro.app.ui.theme.MonoWhite

private val AccentBlue = Color(0xFF0096E6)
private val DarkBoxBg = Color(0xFF141414)

@Composable
fun UpdateScreen(
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    currentVersion: String = "v1.0.1",
    updateInfo: AppUpdateInfo?,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadSpeedFormatted: String,
    isReadyToInstall: Boolean,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val strings = AppStrings.forLanguage(appLanguage)
    val targetVersion = updateInfo?.versionName ?: "v1.0.2"
    val buttonInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoDarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Content Section (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Update Overview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_update),
                        contentDescription = "Update Icon",
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.updateNewVersionAvailable,
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (updateInfo?.releaseTitle?.isNotBlank() == true) updateInfo.releaseTitle else "Moztro Client $targetVersion",
                            color = MonoWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MonoBorder)

                // Version Transition Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.updateCurrentVersion,
                            color = MonoTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(MonoDarkBg, shape = RectangleShape)
                                .border(1.dp, MonoBorder, shape = RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentVersion,
                                color = MonoTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "➔",
                        color = AccentBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.updateNewVersion,
                            color = MonoTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.15f), shape = RectangleShape)
                                .border(1.dp, AccentBlue, shape = RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = targetVersion,
                                color = MonoWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Changelog / What's New Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.updateWhatsNew,
                    color = MonoWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                HorizontalDivider(thickness = 1.dp, color = MonoBorder)

                val notes = if (updateInfo?.releaseNotes?.isNotBlank() == true) {
                    updateInfo.releaseNotes
                } else {
                    strings.updateNoReleaseNotes
                }

                Text(
                    text = notes,
                    color = MonoTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Controls: 20-Box Progress Bar & Blue Action Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Segmented 20-Box Progress Bar (Shown during downloading & when ready to install)
            AnimatedVisibility(
                visible = isDownloading || downloadProgress > 0f || isReadyToInstall,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header progress stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isReadyToInstall) "${strings.updateReadyToInstall.uppercase()} (100%)" else "${strings.updateDownloading.uppercase()} ${(downloadProgress * 100).toInt()}%",
                            color = if (isReadyToInstall) MonoWhite else AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (downloadSpeedFormatted.isNotBlank() && !isReadyToInstall) {
                            Text(
                                text = downloadSpeedFormatted,
                                color = MonoTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 20-Box Segmented Bar
                    Segmented20ProgressBar(
                        progress = if (isReadyToInstall) 1f else downloadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Main Pill Button (Download / Ready To Install)
            val buttonText = when {
                isReadyToInstall -> strings.updateReadyToInstall
                isDownloading -> "${strings.updateDownloading} ${(downloadProgress * 100).toInt()}%"
                else -> java.lang.String.format(strings.updateDownloadBtn, "($targetVersion)")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(50.dp), spotColor = AccentBlue)
                    .background(
                        color = if (isDownloading) AccentBlue.copy(alpha = 0.7f) else AccentBlue,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .clip(RoundedCornerShape(50.dp))
                    .clickable(
                        enabled = !isDownloading,
                        interactionSource = buttonInteractionSource,
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (isReadyToInstall) {
                            onInstallClick()
                        } else {
                            onDownloadClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x66000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 3f
                        )
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 20-Box Segmented Progress Bar
 */
@Composable
fun Segmented20ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val totalBoxes = 20
    val filledCount = (progress * totalBoxes).toInt().coerceIn(0, totalBoxes)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(DarkBoxBg, shape = RoundedCornerShape(6.dp))
            .border(1.5.dp, MonoBorderLight, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalBoxes) {
                val isFilled = i < filledCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(
                            color = if (isFilled) Color.White else Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .border(
                            width = 0.8.dp,
                            color = if (isFilled) Color.White else Color(0xFF333333),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
