package com.moztro.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moztro.app.data.AppLanguage
import com.moztro.app.data.AppStrings
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextMuted
import com.moztro.app.ui.theme.MonoTextPrimary
import com.moztro.app.ui.theme.MonoTextSecondary
import com.moztro.app.ui.theme.MonoTextSubtle
import com.moztro.app.ui.theme.MonoWhite

@Composable
fun AboutScreen(
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    versionName: String = "v1.0.1"
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val strings = AppStrings.forLanguage(appLanguage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MOZTRO",
                    color = MonoWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Box(
                    modifier = Modifier
                        .background(MonoDarkBg, shape = RectangleShape)
                        .border(1.dp, MonoBorder, shape = RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = versionName,
                        color = MonoTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = strings.aboutSubtitle,
                color = MonoTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Section: About the App
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.aboutTitle,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            HorizontalDivider(thickness = 1.dp, color = MonoBorder)
            Text(
                text = strings.aboutDescription,
                color = MonoTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        // Section: Main Features
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.aboutMainFeatures,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            AboutFeatureItem(
                title = strings.featureOverdriveTitle,
                desc = strings.featureOverdriveDesc
            )
            AboutFeatureItem(
                title = strings.featureFastFileTitle,
                desc = strings.featureFastFileDesc
            )
            AboutFeatureItem(
                title = strings.featureVodTitle,
                desc = strings.featureVodDesc
            )
            AboutFeatureItem(
                title = strings.featureRemoteTitle,
                desc = strings.featureRemoteDesc
            )
        }

        // Section: Developer & Creator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.aboutDeveloperTitle,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.aboutCreatedBy,
                    color = MonoTextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Akbar Dwi Mulya",
                    color = MonoWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.aboutBrandProject,
                    color = MonoTextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Measure of Success",
                    color = MonoWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Section: Connect & Contact
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, MonoBorder, shape = RectangleShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.aboutConnectTitle,
                color = MonoWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Instagram card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoDarkBg, shape = RectangleShape)
                    .border(1.dp, MonoBorderLight, shape = RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/measureofsuccess.official"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open Instagram", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.aboutInstagramCard,
                            color = MonoTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "@measureofsuccess.official",
                            color = MonoWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "↗",
                        color = MonoWhite,
                        fontSize = 16.sp
                    )
                }
            }

            // Email card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoDarkBg, shape = RectangleShape)
                    .border(1.dp, MonoBorderLight, shape = RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val email = "measureofsuccess.official@gmail.com"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Moztro Contact Email", email)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, strings.aboutEmailCopied, Toast.LENGTH_SHORT).show()

                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.aboutEmailCard,
                            color = MonoTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "measureofsuccess.official@gmail.com",
                            color = MonoWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "✉",
                        color = MonoWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Section: Copyright & Legal
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = strings.aboutCopyright,
                color = MonoTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = strings.aboutFooter,
                color = MonoTextSubtle,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AboutFeatureItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonoDarkBg, shape = RectangleShape)
            .border(1.dp, MonoBorder, shape = RectangleShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = MonoWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = desc,
            color = MonoTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
