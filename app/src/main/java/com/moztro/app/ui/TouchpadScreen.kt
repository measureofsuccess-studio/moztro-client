package com.moztro.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moztro.app.ui.theme.MonoBorder
import com.moztro.app.ui.theme.MonoBorderLight
import com.moztro.app.ui.theme.MonoDarkBg
import com.moztro.app.ui.theme.MonoSurface
import com.moztro.app.ui.theme.MonoTextMuted
import com.moztro.app.ui.theme.MonoTextSecondary
import com.moztro.app.ui.theme.MonoWhite
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun TouchpadScreen(
    mouseSpeed: Int,
    scrollLines: Int,
    isDragging: Boolean,
    isKeyboardVisible: Boolean = false,
    onDraggingChange: (Boolean) -> Unit,
    onMouseMove: (dx: Float, dy: Float) -> Unit,
    onMouseClick: (button: String) -> Unit,
    onMouseDown: (button: String) -> Unit,
    onMouseUp: (button: String) -> Unit,
    onMouseScroll: (deltaY: Int) -> Unit,
    onKeyPress: (key: String, modifiers: List<String>) -> Unit = { _, _ -> },
    onModifierChange: (modifier: String, isDown: Boolean) -> Unit = { _, _ -> },
    onReleaseAllModifiers: () -> Unit = {}
) {
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Modifier toggles
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }
    var isShiftActive by remember { mutableStateOf(false) }
    var isWinActive by remember { mutableStateOf(false) }
    var isFnActive by remember { mutableStateOf(false) }

    // Live Typed Text & Hidden Native Input
    var typedText by remember { mutableStateOf("") }
    var hiddenInputText by remember { mutableStateOf(TextFieldValue("")) }
    val textScrollState = rememberScrollState()

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

    // Handle Keyboard visibility changes and focus request
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            delay(120)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
            onReleaseAllModifiers()
            isCtrlActive = false
            isAltActive = false
            isShiftActive = false
            isWinActive = false
            isFnActive = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            onReleaseAllModifiers()
        }
    }

    fun sendSpecialKey(code: String) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        when (code) {
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
                onKeyPress("WIN", emptyList())
                isWinActive = !isWinActive
                return
            }
            "FN" -> {
                isFnActive = !isFnActive
                return
            }
        }

        // Collect active modifiers
        val currentMods = mutableListOf<String>()
        if (isCtrlActive) currentMods.add("CTRL")
        if (isAltActive) currentMods.add("ALT")
        if (isShiftActive) currentMods.add("SHIFT")
        if (isWinActive) currentMods.add("WIN")

        // Handle Shortcut combos
        when (code) {
            "CTRL_C" -> {
                onKeyPress("c", listOf("CTRL"))
                typedText += " [Ctrl+C] "
                return
            }
            "CTRL_V" -> {
                onKeyPress("v", listOf("CTRL"))
                typedText += " [Ctrl+V] "
                return
            }
            "CTRL_Z" -> {
                onKeyPress("z", listOf("CTRL"))
                typedText += " [Ctrl+Z] "
                return
            }
            "CTRL_A" -> {
                onKeyPress("a", listOf("CTRL"))
                typedText += " [Ctrl+A] "
                return
            }
            "ALT_TAB" -> {
                onKeyPress("TAB", listOf("ALT"))
                return
            }
        }

        onKeyPress(code, currentMods)

        when (code) {
            "BKSP" -> {
                if (typedText.isNotEmpty()) typedText = typedText.dropLast(1)
            }
            "ENTER" -> typedText += " ↵ "
            "TAB" -> typedText += "    "
            "SPACE" -> typedText += " "
            "ESC" -> {
                if (isAltActive) isAltActive = false
            }
        }

        // Reset single-shot modifiers
        if (isCtrlActive) isCtrlActive = false
        if (isWinActive) isWinActive = false
        if (isShiftActive) isShiftActive = false
        if (isAltActive && (code == "ENTER" || code == "ESC")) isAltActive = false
    }

    // Double-tap and hold tracking state
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf<Offset?>(null) }

    val touchpadGestureModifier = Modifier.pointerInput(mouseSpeed, scrollLines) {
        val currentSensitivity = (mouseSpeed / 10f) * 1.5f
        val scrollMultiplier = (scrollLines / 3f) * 2.2f

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downTime = System.currentTimeMillis()
            val downPos = down.position
            var pointerCount = 1
            var totalMovedDistance = 0f
            var isTwoFingerGesture = false
            var lastTwoFingerY = 0f

            val timeSinceLastTap = downTime - lastTapTime
            val prevPos = lastTapPos
            val isDoubleTapHoldCandidate = prevPos != null &&
                    timeSinceLastTap in 40..320 &&
                    (downPos - prevPos).getDistance() < 120f

            var dragEngaged = false
            if (isDoubleTapHoldCandidate) {
                dragEngaged = true
                onDraggingChange(true)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onMouseDown("LEFT")
            }

            while (true) {
                val event = awaitPointerEvent()
                val activePointers = event.changes.filter { it.pressed }
                pointerCount = activePointers.size

                if (activePointers.isEmpty()) {
                    break
                }

                if (pointerCount >= 2) {
                    isTwoFingerGesture = true
                    val p1 = activePointers[0]
                    val p2 = activePointers[1]
                    val currentCenterY = (p1.position.y + p2.position.y) / 2f

                    if (lastTwoFingerY != 0f) {
                        val deltaY = currentCenterY - lastTwoFingerY
                        if (abs(deltaY) > 1.2f) {
                            val scrollAmount = (deltaY * scrollMultiplier).toInt()
                            if (scrollAmount != 0) {
                                onMouseScroll(scrollAmount)
                            }
                        }
                    }
                    lastTwoFingerY = currentCenterY
                    p1.consume()
                    p2.consume()
                } else if (pointerCount == 1 && !isTwoFingerGesture) {
                    val pointer = activePointers[0]
                    val change = pointer.positionChange()
                    val moveDist = change.getDistance()
                    totalMovedDistance += moveDist

                    if (moveDist > 0.3f) {
                        val speedMultiplier = when {
                            moveDist < 1.0f -> 0.85f
                            moveDist < 4.0f -> 1.05f
                            moveDist < 12.0f -> 1.35f
                            else -> 1.75f
                        }
                        val dx = change.x * currentSensitivity * speedMultiplier
                        val dy = change.y * currentSensitivity * speedMultiplier
                        onMouseMove(dx, dy)
                        pointer.consume()
                    }
                }
            }

            val upTime = System.currentTimeMillis()
            val duration = upTime - downTime

            if (dragEngaged) {
                onDraggingChange(false)
                onMouseUp("LEFT")
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                lastTapTime = 0L
                lastTapPos = null
            } else if (isTwoFingerGesture) {
                if (duration < 280 && totalMovedDistance < 45f) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMouseClick("RIGHT")
                }
            } else {
                if (duration < 220 && totalMovedDistance < 25f) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMouseClick("LEFT")
                    lastTapTime = upTime
                    lastTapPos = downPos
                } else {
                    lastTapTime = 0L
                    lastTapPos = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoDarkBg)
            .imePadding()
    ) {
        // Upper Section: Touchpad Canvas (Adjusts dynamically)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = if (isKeyboardVisible) 8.dp else 12.dp,
                    end = if (isKeyboardVisible) 8.dp else 12.dp,
                    top = if (isKeyboardVisible) 8.dp else 12.dp,
                    bottom = if (isKeyboardVisible) 4.dp else 12.dp
                )
                .background(MonoSurface, shape = RectangleShape)
                .border(1.dp, if (isDragging) MonoWhite else MonoBorder, shape = RectangleShape)
                .then(touchpadGestureModifier),
            contentAlignment = Alignment.Center
        ) {
            TouchSurfaceIndicator(isDragging = isDragging)
        }

        // Lower Section: When Keyboard is toggled ON
        if (isKeyboardVisible) {
            HorizontalDivider(thickness = 1.dp, color = MonoBorder)

            // Live Type Preview Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MonoDarkBg)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monospace Preview Box (Tapping it re-opens/focuses native keyboard)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MonoSurface, shape = RectangleShape)
                        .border(1.dp, MonoBorder, shape = RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            try {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            } catch (_: Exception) {}
                        }
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
                                text = "TAP TO TYPE",
                                color = Color(0xFF555555),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = " ▌",
                                color = MonoWhite.copy(alpha = cursorAlpha),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = typedText,
                                color = MonoWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "▌",
                                color = MonoWhite.copy(alpha = cursorAlpha),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Clear button [✕]
                    if (typedText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                                .background(Color(0xFF222222), shape = RectangleShape)
                                .border(1.dp, MonoBorderLight, shape = RectangleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    typedText = ""
                                    hiddenInputText = TextFieldValue("")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Active Modifier Badges
                val hasActiveBadge = isCtrlActive || isWinActive || isAltActive || isShiftActive || isFnActive
                if (hasActiveBadge) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCtrlActive) ModifierBadge("CTRL", isPortraitMode = true)
                        if (isWinActive) ModifierBadge("WIN", isPortraitMode = true)
                        if (isAltActive) ModifierBadge("ALT", isPortraitMode = true)
                        if (isShiftActive) ModifierBadge("SHIFT", isPortraitMode = true)
                        if (isFnActive) ModifierBadge("FN", isAccent = true, isPortraitMode = true)
                    }
                }
            }

            // Row 1: PC Modifiers & Special Key Extensions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MonoDarkBg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarKeyButton("ESC", isActive = false) { sendSpecialKey("ESC") }
                ToolbarKeyButton("TAB", isActive = false) { sendSpecialKey("TAB") }
                ToolbarKeyButton("WIN", isActive = isWinActive) { sendSpecialKey("WIN") }
                ToolbarKeyButton("CTRL", isActive = isCtrlActive) { sendSpecialKey("CTRL") }
                ToolbarKeyButton("ALT", isActive = isAltActive) { sendSpecialKey("ALT") }
                ToolbarKeyButton("SHIFT", isActive = isShiftActive) { sendSpecialKey("SHIFT") }
                ToolbarKeyButton("FN", isActive = isFnActive, isAccent = true) { sendSpecialKey("FN") }
                ToolbarKeyButton("DEL", isActive = false) { sendSpecialKey("DEL") }
                ToolbarKeyButton("BKSP", isActive = false) { sendSpecialKey("BKSP") }
                ToolbarKeyButton("ENTER", isActive = false) { sendSpecialKey("ENTER") }
            }

            // Row 2: Arrows, Navigation & Function Keys (Switches with FN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MonoDarkBg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isFnActive) {
                    // Navigation Arrows
                    ToolbarKeyButton("◄", minWidth = 36.dp) { sendSpecialKey("LEFT") }
                    ToolbarKeyButton("▲", minWidth = 36.dp) { sendSpecialKey("UP") }
                    ToolbarKeyButton("▼", minWidth = 36.dp) { sendSpecialKey("DOWN") }
                    ToolbarKeyButton("►", minWidth = 36.dp) { sendSpecialKey("RIGHT") }
                    // Document Navigation
                    ToolbarKeyButton("HOME") { sendSpecialKey("HOME") }
                    ToolbarKeyButton("END") { sendSpecialKey("END") }
                    ToolbarKeyButton("PGUP") { sendSpecialKey("PGUP") }
                    ToolbarKeyButton("PGDN") { sendSpecialKey("PGDN") }
                    // Quick Shortcuts
                    ToolbarKeyButton("Ctrl+C") { sendSpecialKey("CTRL_C") }
                    ToolbarKeyButton("Ctrl+V") { sendSpecialKey("CTRL_V") }
                    ToolbarKeyButton("Ctrl+Z") { sendSpecialKey("CTRL_Z") }
                    ToolbarKeyButton("Ctrl+A") { sendSpecialKey("CTRL_A") }
                    ToolbarKeyButton("Alt+Tab") { sendSpecialKey("ALT_TAB") }
                } else {
                    // FN Function Keys Layer (F1 - F12 + System)
                    listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12").forEach { fKey ->
                        ToolbarKeyButton(fKey, isAccent = true) { sendSpecialKey(fKey) }
                    }
                    ToolbarKeyButton("PRTSC") { sendSpecialKey("PRTSC") }
                    ToolbarKeyButton("SCRLK") { sendSpecialKey("SCRLK") }
                    ToolbarKeyButton("PAUSE") { sendSpecialKey("PAUSE") }
                    ToolbarKeyButton("INS") { sendSpecialKey("INS") }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Hidden Native Keyboard Capture Field
            BasicTextField(
                value = hiddenInputText,
                onValueChange = { newVal ->
                    val prevStr = hiddenInputText.text
                    val newStr = newVal.text

                    if (newStr.length > prevStr.length) {
                        val added = newStr.substring(prevStr.length)
                        val currentMods = mutableListOf<String>()
                        if (isCtrlActive) currentMods.add("CTRL")
                        if (isAltActive) currentMods.add("ALT")
                        if (isShiftActive) currentMods.add("SHIFT")
                        if (isWinActive) currentMods.add("WIN")

                        onKeyPress(added, currentMods)
                        typedText += added

                        if (isCtrlActive) isCtrlActive = false
                        if (isWinActive) isWinActive = false
                        if (isShiftActive) isShiftActive = false
                    } else if (newStr.length < prevStr.length) {
                        val deletedCount = prevStr.length - newStr.length
                        for (i in 0 until deletedCount) {
                            onKeyPress("BKSP", emptyList())
                            if (typedText.isNotEmpty()) {
                                typedText = typedText.dropLast(1)
                            }
                        }
                    }
                    hiddenInputText = newVal
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val currentMods = mutableListOf<String>()
                        if (isCtrlActive) currentMods.add("CTRL")
                        if (isAltActive) currentMods.add("ALT")
                        if (isShiftActive) currentMods.add("SHIFT")
                        if (isWinActive) currentMods.add("WIN")

                        onKeyPress("ENTER", currentMods)
                        typedText += " ↵ "
                        if (isCtrlActive) isCtrlActive = false
                        if (isWinActive) isWinActive = false
                        if (isShiftActive) isShiftActive = false
                        if (isAltActive) isAltActive = false
                    }
                ),
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
            )
        }
    }
}

@Composable
fun ToolbarKeyButton(
    label: String,
    isActive: Boolean = false,
    isAccent: Boolean = false,
    minWidth: androidx.compose.ui.unit.Dp = 42.dp,
    onClick: () -> Unit
) {
    val bg = when {
        isActive -> MonoWhite
        isAccent -> Color(0xFF242424)
        else -> MonoSurface
    }
    val textColor = when {
        isActive -> MonoDarkBg
        isAccent -> MonoWhite
        else -> MonoTextSecondary
    }
    val borderCol = when {
        isActive -> MonoWhite
        isAccent -> MonoBorderLight
        else -> MonoBorder
    }

    Box(
        modifier = Modifier
            .height(32.dp)
            .width(minWidth)
            .background(bg, shape = RectangleShape)
            .border(1.dp, borderCol, shape = RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 4) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
fun TouchSurfaceIndicator(isDragging: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isDragging) "DRAGGING..." else "TOUCH SURFACE",
            color = if (isDragging) MonoWhite else Color(0xFF3A3A3A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "•",
            color = if (isDragging) MonoWhite else Color(0xFF2A2A2A),
            fontSize = 20.sp
        )
    }
}
