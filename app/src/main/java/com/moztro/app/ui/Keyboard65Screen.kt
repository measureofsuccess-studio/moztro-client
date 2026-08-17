package com.moztro.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moztro.app.R
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextMuted
import com.moztro.app.ui.theme.MonoTextSecondary
import com.moztro.app.ui.theme.MonoWhite
import kotlinx.coroutines.withTimeoutOrNull

data class KeyDef(
    val label: String,
    val shiftLabel: String? = null,
    val fnLabel: String? = null,
    val code: String = label,
    val fnCode: String? = null,
    val weight: Float = 1f,
    val isModifier: Boolean = false
)

@Composable
fun Keyboard65Screen(
    onBackClick: () -> Unit,
    onKeyPress: (key: String, modifiers: List<String>) -> Unit,
    onModifierChange: (modifier: String, isDown: Boolean) -> Unit = { _, _ -> },
    onReleaseAllModifiers: () -> Unit = {}
) {
    val context = LocalContext.current

    // Lock orientation to LANDSCAPE when entering this screen, revert on exit & release all held modifiers
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onReleaseAllModifiers()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoDarkBg)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Keyboard65Content(
            isPortraitMode = false,
            showBackButton = true,
            onBackClick = onBackClick,
            onKeyPress = onKeyPress,
            onModifierChange = onModifierChange,
            onReleaseAllModifiers = onReleaseAllModifiers,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun Keyboard65Content(
    isPortraitMode: Boolean = false,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onKeyPress: (key: String, modifiers: List<String>) -> Unit,
    onModifierChange: (modifier: String, isDown: Boolean) -> Unit = { _, _ -> },
    onReleaseAllModifiers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }
    var isShiftActive by remember { mutableStateOf(false) }
    var isWinActive by remember { mutableStateOf(false) }
    var isFnActive by remember { mutableStateOf(false) }
    var isCapsLockActive by remember { mutableStateOf(false) }

    // Live Typed Text Preview Buffer
    var typedText by remember { mutableStateOf("") }
    val textScrollState = rememberScrollState()

    LaunchedEffect(typedText) {
        textScrollState.animateScrollTo(textScrollState.maxValue)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    fun handleKey(key: KeyDef) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        // 1. Toggle Modifiers Handling
        when (key.code) {
            "CTRL" -> {
                isCtrlActive = !isCtrlActive
                return
            }
            "ALT" -> {
                isAltActive = !isAltActive
                return
            }
            "SHIFT" -> {
                isShiftActive = !isShiftActive
                return
            }
            "WIN" -> {
                // Tapping WIN immediately opens/closes Start Menu
                onKeyPress("WIN", emptyList())
                isWinActive = !isWinActive
                return
            }
            "FN" -> {
                isFnActive = !isFnActive
                return
            }
            "CAPS" -> {
                isCapsLockActive = !isCapsLockActive
                onKeyPress("CAPSLOCK", emptyList())
                return
            }
        }

        // 2. Collect Active Modifier Keys for Combinations (CTRL, ALT, SHIFT, WIN)
        val currentModifiers = mutableListOf<String>()
        if (isCtrlActive) currentModifiers.add("CTRL")
        if (isAltActive) currentModifiers.add("ALT")
        if (isShiftActive) currentModifiers.add("SHIFT")
        if (isWinActive) currentModifiers.add("WIN")

        // 3. Determine target key code
        val targetKey: String = when {
            isFnActive && key.fnCode != null -> key.fnCode
            key.code == "SPACE" -> "SPACE"
            key.code == "ESCAPE" || key.code == "ESC" -> "ESC"
            key.isModifier -> key.code
            else -> {
                if (isShiftActive || isCapsLockActive) {
                    key.shiftLabel ?: key.label.uppercase()
                } else {
                    key.label.lowercase()
                }
            }
        }

        // 4. Send key to PC (with all active modifiers)
        onKeyPress(targetKey, currentModifiers)

        // 5. Update live typed text preview buffer
        when (targetKey) {
            "SPACE" -> {
                typedText += " "
            }
            "BKSP", "DEL" -> {
                if (typedText.isNotEmpty()) {
                    typedText = typedText.dropLast(1)
                }
            }
            "ENTER" -> {
                typedText += " ↵ "
            }
            "TAB" -> {
                typedText += "    "
            }
            else -> {
                // Append printable character
                if (targetKey.length == 1) {
                    typedText += targetKey
                }
            }
        }

        // Reset single-shot modifiers (CTRL, WIN, SHIFT), but KEEP ALT active for multi-step navigation
        if (isCtrlActive) isCtrlActive = false
        if (isWinActive) isWinActive = false
        if (isShiftActive) isShiftActive = false

        // Release ALT toggle when ENTER or ESC is pressed
        if (isAltActive && (targetKey == "ENTER" || targetKey == "ESC")) {
            isAltActive = false
        }
    }

    // 65% Keyboard Layout (5 Rows, Exact 15.6 Total Weight per Row)
    val row1 = listOf(
        KeyDef("ESC", fnLabel = "`", code = "ESC", fnCode = "`", weight = 1f, isModifier = true),
        KeyDef("1", "!", "F1", "1", "F1", 1f),
        KeyDef("2", "@", "F2", "2", "F2", 1f),
        KeyDef("3", "#", "F3", "3", "F3", 1f),
        KeyDef("4", "$", "F4", "4", "F4", 1f),
        KeyDef("5", "%", "F5", "5", "F5", 1f),
        KeyDef("6", "^", "F6", "6", "F6", 1f),
        KeyDef("7", "&", "F7", "7", "F7", 1f),
        KeyDef("8", "*", "F8", "8", "F8", 1f),
        KeyDef("9", "(", "F9", "9", "F9", 1f),
        KeyDef("0", ")", "F10", "0", "F10", 1f),
        KeyDef("-", "_", "F11", "-", "F11", 1f),
        KeyDef("=", "+", "F12", "=", "F12", 1f),
        KeyDef("BKSP", fnLabel = "DEL", code = "BKSP", fnCode = "DEL", weight = 1.6f, isModifier = true),
        KeyDef("DEL", fnLabel = "INS", code = "DEL", fnCode = "INS", weight = 1f, isModifier = true)
    )

    val row2 = listOf(
        KeyDef("TAB", code = "TAB", weight = 1.4f, isModifier = true),
        KeyDef("Q", weight = 1f),
        KeyDef("W", weight = 1f),
        KeyDef("E", weight = 1f),
        KeyDef("R", weight = 1f),
        KeyDef("T", weight = 1f),
        KeyDef("Y", weight = 1f),
        KeyDef("U", weight = 1f),
        KeyDef("I", weight = 1f),
        KeyDef("O", weight = 1f),
        KeyDef("P", fnLabel = "PRTSC", fnCode = "PRTSC", weight = 1f),
        KeyDef("[", "{", "SCRLK", "[", "SCRLK", 1f),
        KeyDef("]", "}", "PAUSE", "]", "PAUSE", 1f),
        KeyDef("\\", "|", fnLabel = "APP", code = "\\", fnCode = "APP", weight = 1.2f),
        KeyDef("PGUP", fnLabel = "HOME", code = "PGUP", fnCode = "HOME", weight = 1f, isModifier = true)
    )

    val row3 = listOf(
        KeyDef("CAPS", code = "CAPS", weight = 1.8f, isModifier = true),
        KeyDef("A", fnLabel = "VOL-", fnCode = "VOL-", weight = 1f),
        KeyDef("S", fnLabel = "VOL+", fnCode = "VOL+", weight = 1f),
        KeyDef("D", fnLabel = "MUTE", fnCode = "MUTE", weight = 1f),
        KeyDef("F", fnLabel = "PREV", fnCode = "PREV", weight = 1f),
        KeyDef("G", fnLabel = "PLAY", fnCode = "PLAY", weight = 1f),
        KeyDef("H", fnLabel = "NEXT", fnCode = "NEXT", weight = 1f),
        KeyDef("J", weight = 1f),
        KeyDef("K", weight = 1f),
        KeyDef("L", weight = 1f),
        KeyDef(";", ":", weight = 1f),
        KeyDef("'", "\"", weight = 1f),
        KeyDef("ENTER", code = "ENTER", weight = 2f, isModifier = true),
        KeyDef("PGDN", fnLabel = "END", code = "PGDN", fnCode = "END", weight = 1f, isModifier = true)
    )

    val row4 = listOf(
        KeyDef("SHIFT", code = "SHIFT", weight = 2.2f, isModifier = true),
        KeyDef("Z", weight = 1f),
        KeyDef("X", weight = 1f),
        KeyDef("C", weight = 1f),
        KeyDef("V", weight = 1f),
        KeyDef("B", weight = 1f),
        KeyDef("N", weight = 1f),
        KeyDef("M", weight = 1f),
        KeyDef(",", "<", weight = 1f),
        KeyDef(".", ">", weight = 1f),
        KeyDef("/", "?", weight = 1f),
        KeyDef("SHIFT", code = "SHIFT", weight = 1.4f, isModifier = true),
        KeyDef("▲", fnLabel = "PGUP", code = "UP", fnCode = "PGUP", weight = 1f, isModifier = true),
        KeyDef("END", code = "END", weight = 1f, isModifier = true)
    )

    val row5 = listOf(
        KeyDef("CTRL", code = "CTRL", weight = 1.3f, isModifier = true),
        KeyDef("WIN", code = "WIN", weight = 1.3f, isModifier = true),
        KeyDef("ALT", code = "ALT", weight = 1.3f, isModifier = true),
        KeyDef("SPACE", code = "SPACE", weight = 5.7f, isModifier = false),
        KeyDef("ALT", code = "ALT", weight = 1.2f, isModifier = true),
        KeyDef("FN", code = "FN", weight = 1.2f, isModifier = true),
        KeyDef("CTRL", code = "CTRL", weight = 1.3f, isModifier = true),
        KeyDef("◄", fnLabel = "HOME", code = "LEFT", fnCode = "HOME", weight = 1.1f, isModifier = true),
        KeyDef("▼", fnLabel = "PGDN", code = "DOWN", fnCode = "PGDN", weight = 1.1f, isModifier = true),
        KeyDef("►", fnLabel = "END", code = "RIGHT", fnCode = "END", weight = 1.1f, isModifier = true)
    )

    Column(
        modifier = modifier
            .background(MonoDarkBg)
    ) {
        // Live Typed Text Monitor Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isPortraitMode) 34.dp else 44.dp)
                .background(MonoDarkBg)
                .padding(horizontal = 4.dp, vertical = if (isPortraitMode) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                // Back Button Box
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MonoSurface, shape = RectangleShape)
                        .border(1.dp, MonoBorder, shape = RectangleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_back_button),
                        contentDescription = "Back",
                        colorFilter = ColorFilter.tint(MonoWhite),
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Live Typed Text Monitor Box (High-contrast, Monospace)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MonoSurface, shape = RectangleShape)
                    .border(1.dp, MonoBorder, shape = RectangleShape)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = if (typedText.isNotEmpty()) 24.dp else 0.dp)
                        .horizontalScroll(textScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (typedText.isEmpty()) {
                        Text(
                            text = "TYPE PREVIEW",
                            color = Color(0xFF555555),
                            fontSize = if (isPortraitMode) 11.sp else 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " ▌",
                            color = MonoWhite.copy(alpha = cursorAlpha),
                            fontSize = if (isPortraitMode) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = typedText,
                            color = MonoWhite,
                            fontSize = if (isPortraitMode) 13.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "▌",
                            color = MonoWhite.copy(alpha = cursorAlpha),
                            fontSize = if (isPortraitMode) 12.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Quick Clear Button [✕] when text is typed
                if (typedText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(if (isPortraitMode) 26.dp else 32.dp)
                            .background(Color(0xFF222222), shape = RectangleShape)
                            .border(1.dp, MonoBorderLight, shape = RectangleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                typedText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color(0xFFAAAAAA),
                            fontSize = if (isPortraitMode) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status Indicator Badges (Active Modifiers & FN)
            val hasActiveBadge = isCtrlActive || isWinActive || isAltActive || isShiftActive || isCapsLockActive || isFnActive
            if (hasActiveBadge) {
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCtrlActive) ModifierBadge("CTRL", isPortraitMode = isPortraitMode)
                    if (isWinActive) ModifierBadge("WIN", isPortraitMode = isPortraitMode)
                    if (isAltActive) ModifierBadge("ALT", isPortraitMode = isPortraitMode)
                    if (isShiftActive) ModifierBadge("SHIFT", isPortraitMode = isPortraitMode)
                    if (isCapsLockActive) ModifierBadge("CAPS", isPortraitMode = isPortraitMode)
                    if (isFnActive) ModifierBadge("FN", isAccent = true, isPortraitMode = isPortraitMode)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MonoBorder)

        // 65% Keyboard Grid (5 Rows with dynamic active highlight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isPortraitMode) Modifier.fillMaxHeight() else Modifier.height(190.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(if (isPortraitMode) 2.dp else 3.dp)
        ) {
            KeyboardRow(
                keys = row1,
                isShift = isShiftActive,
                isCaps = isCapsLockActive,
                isFn = isFnActive,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                isWinActive = isWinActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier.weight(1f),
                onKeyClick = { handleKey(it) }
            )
            KeyboardRow(
                keys = row2,
                isShift = isShiftActive,
                isCaps = isCapsLockActive,
                isFn = isFnActive,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                isWinActive = isWinActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier.weight(1f),
                onKeyClick = { handleKey(it) }
            )
            KeyboardRow(
                keys = row3,
                isShift = isShiftActive,
                isCaps = isCapsLockActive,
                isFn = isFnActive,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                isWinActive = isWinActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier.weight(1f),
                onKeyClick = { handleKey(it) }
            )
            KeyboardRow(
                keys = row4,
                isShift = isShiftActive,
                isCaps = isCapsLockActive,
                isFn = isFnActive,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                isWinActive = isWinActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier.weight(1f),
                onKeyClick = { handleKey(it) }
            )
            KeyboardRow(
                keys = row5,
                isShift = isShiftActive,
                isCaps = isCapsLockActive,
                isFn = isFnActive,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                isWinActive = isWinActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier.weight(1f),
                onKeyClick = { handleKey(it) }
            )
        }
    }
}

@Composable
fun ModifierBadge(text: String, isAccent: Boolean = false, isPortraitMode: Boolean = false) {
    Box(
        modifier = Modifier
            .background(if (isAccent) Color(0xFF2E2E2E) else MonoSurface, shape = RectangleShape)
            .border(1.dp, if (isAccent) MonoWhite else MonoBorderLight, shape = RectangleShape)
            .padding(horizontal = if (isPortraitMode) 4.dp else 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MonoWhite,
            fontSize = if (isPortraitMode) 8.sp else 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun KeyboardRow(
    keys: List<KeyDef>,
    isShift: Boolean,
    isCaps: Boolean,
    isFn: Boolean,
    isCtrlActive: Boolean,
    isAltActive: Boolean,
    isWinActive: Boolean,
    isPortraitMode: Boolean = false,
    modifier: Modifier = Modifier,
    onKeyClick: (KeyDef) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isPortraitMode) 1.5.dp else 3.dp)
    ) {
        keys.forEach { key ->
            val isKeyActive = when (key.code) {
                "CTRL" -> isCtrlActive
                "ALT" -> isAltActive
                "SHIFT" -> isShift
                "WIN" -> isWinActive
                "FN" -> isFn
                "CAPS" -> isCaps
                else -> false
            }

            KeyCap(
                key = key,
                isShift = isShift,
                isCaps = isCaps,
                isFn = isFn,
                isActive = isKeyActive,
                isPortraitMode = isPortraitMode,
                modifier = Modifier
                    .weight(key.weight)
                    .fillMaxHeight(),
                onClick = { onKeyClick(key) }
            )
        }
    }
}

@Composable
fun KeyCap(
    key: KeyDef,
    isShift: Boolean,
    isCaps: Boolean,
    isFn: Boolean,
    isActive: Boolean,
    isPortraitMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressedState by remember { mutableStateOf(false) }
    val isNoSpamKey = key.code in listOf("CTRL", "ALT", "SHIFT", "WIN", "FN", "CAPS", "TAB", "ESC", "ENTER")

    val surfaceColor = when {
        isPressedState -> Color(0xFF333333)
        isActive -> Color(0xFF282828)
        key.isModifier -> Color(0xFF0F0F0F)
        else -> MonoSurface
    }

    val borderColor = when {
        isActive -> MonoWhite
        isPressedState -> MonoWhite
        key.isModifier -> MonoBorder
        else -> MonoBorderLight
    }

    val displayLabel = when {
        isFn && key.fnLabel != null -> key.fnLabel
        isShift || isCaps -> key.label.uppercase()
        key.isModifier -> key.label
        else -> key.label.uppercase()
    }

    val currentOnClick by rememberUpdatedState(onClick)

    // Gesture handling: Immediate response on touch down for ALL keys (0ms latency, no wait for release)
    val gestureModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            isPressedState = true
            currentOnClick() // Responds immediately the moment finger touches the key!

            if (!isNoSpamKey) {
                // Initial hold delay before auto-repeat begins for typing/navigation keys (380ms)
                val initialHoldPassed = withTimeoutOrNull(380L) {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) {
                            return@withTimeoutOrNull false
                        }
                    }
                } == null

                if (initialHoldPassed) {
                    // Continuous repeat loop while finger remains pressed (every 55ms)
                    while (true) {
                        val released = withTimeoutOrNull(55L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) {
                                    return@withTimeoutOrNull true
                                }
                            }
                        } == true

                        if (released) break
                        currentOnClick()
                    }
                }
            } else {
                // For non-spam keys (TAB, ESC, ENTER, modifiers), wait until finger is lifted without repeating
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.all { !it.pressed }) {
                        break
                    }
                }
            }
            isPressedState = false
        }
    }

    Box(
        modifier = modifier
            .then(gestureModifier)
            .background(surfaceColor, shape = RectangleShape)
            .border(1.dp, borderColor, shape = RectangleShape)
            .padding(if (isPortraitMode) 0.5.dp else 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sub-label (shift symbol or FN function hint when not active)
            if (key.shiftLabel != null && !isFn) {
                Text(
                    text = key.shiftLabel,
                    color = if (isShift) MonoWhite else MonoTextMuted,
                    fontSize = if (isPortraitMode) 6.5.sp else 8.sp,
                    fontWeight = if (isShift) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            // Main Label
            Text(
                text = displayLabel,
                color = when {
                    isActive -> MonoWhite
                    isFn && key.fnLabel != null -> MonoWhite
                    key.isModifier -> MonoTextSecondary
                    else -> MonoWhite
                },
                fontSize = when {
                    isPortraitMode -> when {
                        displayLabel.length > 4 -> 6.5.sp
                        displayLabel.length > 2 -> 7.5.sp
                        key.isModifier -> 8.sp
                        else -> 9.5.sp
                    }
                    else -> when {
                        displayLabel.length > 4 -> 8.sp
                        displayLabel.length > 2 -> 9.sp
                        key.isModifier -> 10.sp
                        else -> 12.sp
                    }
                },
                fontWeight = if (key.isModifier || isActive || (isFn && key.fnLabel != null)) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}
