package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NeetSyllabus
import com.example.ui.state.FocusTimerState
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBg
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGoldBorder
import com.example.ui.theme.IgrisGoldSurface
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisGreenBorder
import com.example.ui.theme.IgrisGreenSurface
import com.example.ui.theme.IgrisHeroBorder
import com.example.ui.theme.IgrisLine
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPanel
import com.example.ui.theme.IgrisPanelAlt
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedBright
import com.example.ui.theme.IgrisRedDark
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisSurfaceDark
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FocusTimerModeToggle(
    flipClockMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = IgrisSurfaceDark,
        border = BorderStroke(1.dp, IgrisBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf(
                Triple(false, "Ring", Icons.Default.DonutLarge),
                Triple(true, "Flip", Icons.Default.Flip)
            )
            modes.forEach { (isFlip, label, icon) ->
                val selected = flipClockMode == isFlip
                Surface(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSelectMode(isFlip) }
                        .testTag(if (isFlip) "timer_mode_flip" else "timer_mode_ring"),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) IgrisSoftRed else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, IgrisRed) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "$label timer mode",
                            tint = if (selected) IgrisRedBright else IgrisMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = label,
                            fontFamily = DmMonoFontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (selected) IgrisText else IgrisMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinearFocusProgressBar(
    progress: Float,
    elapsedSeconds: Int,
    totalSeconds: Int,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "linear_focus_progress"
    )
    val accent = if (completed) IgrisGreen else IgrisRed

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESSION PROGRESS • ${(animatedProgress * 100).toInt()}%",
                fontFamily = DmMonoFontFamily,
                fontSize = 10.sp,
                color = if (completed) IgrisGreen else IgrisMuted,
                letterSpacing = 1.1.sp
            )
            Text(
                text = "${DateUtils.formatMMSS(elapsedSeconds)} / ${DateUtils.formatMMSS(totalSeconds)}",
                fontFamily = DmMonoFontFamily,
                fontSize = 11.sp,
                color = IgrisText
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(IgrisSurfaceDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (completed) listOf(IgrisGreenBorder, IgrisGreen)
                            else listOf(IgrisRedDark, IgrisRed, IgrisRedBright)
                        )
                    )
            )
        }
    }
}

@Composable
fun CircularFocusProgressDisplay(
    timerState: FocusTimerState,
    ringSize: Dp = 224.dp,
    modifier: Modifier = Modifier
) {
    val totalSec = maxOf(1, timerState.initialSeconds)
    val elapsedSec = (totalSec - timerState.remainingSeconds).coerceIn(0, totalSec)
    val rawProgress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "circular_focus_progress"
    )
    val isComplete = timerState.remainingSeconds == 0 && !timerState.running
    val (hh, mm, ss) = DateUtils.formatHHMMSS(timerState.remainingSeconds)
    val timeText = if (hh != "00") "$hh:$mm:$ss" else "$mm:$ss"

    val statusText = when {
        timerState.running -> "RUNNING"
        isComplete -> "COMPLETE"
        timerState.ranSeconds > 0 -> "PAUSED"
        else -> "READY"
    }
    val statusColor = when {
        timerState.running -> IgrisRedBright
        isComplete -> IgrisGreen
        timerState.ranSeconds > 0 -> IgrisGold
        else -> IgrisMuted
    }

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            val tickOuterRadius = size.minDimension / 2f - 4.dp.toPx()
            val arcRadius = tickOuterRadius - 14.dp.toPx()
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            // 60 precision dial tick marks around the perimeter
            for (i in 0 until 60) {
                val angleRad = (-90.0 + i * 6.0) * PI / 180.0
                val isMajor = i % 5 == 0
                val inner = tickOuterRadius - (if (isMajor) 8.dp.toPx() else 4.dp.toPx())
                val start = Offset(
                    x = center.x + (inner * cos(angleRad)).toFloat(),
                    y = center.y + (inner * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (tickOuterRadius * cos(angleRad)).toFloat(),
                    y = center.y + (tickOuterRadius * sin(angleRad)).toFloat()
                )
                val tickLit = (i / 60f) <= animatedProgress && animatedProgress > 0f
                drawLine(
                    color = when {
                        tickLit && isComplete -> IgrisGreen.copy(alpha = 0.85f)
                        tickLit -> IgrisRedBright.copy(alpha = 0.8f)
                        isMajor -> IgrisBorderSubtle
                        else -> IgrisLine
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Track ring
            drawArc(
                color = IgrisPanelAlt,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Subtle inner bezel
            drawCircle(
                color = IgrisSurfaceDark,
                radius = arcRadius - strokePx * 0.65f,
                center = center
            )

            // Active sweep arc
            if (animatedProgress > 0.002f) {
                val sweep = 360f * animatedProgress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = if (isComplete) {
                            listOf(IgrisGreenBorder, IgrisGreen, IgrisGreen)
                        } else {
                            listOf(IgrisRedDark, IgrisRed, IgrisRedBright, IgrisRed)
                        }
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Tip indicator dot
                val tipRad = (-90.0 + sweep) * PI / 180.0
                val tipOffset = Offset(
                    x = center.x + (arcRadius * cos(tipRad)).toFloat(),
                    y = center.y + (arcRadius * sin(tipRad)).toFloat()
                )
                drawCircle(
                    color = if (isComplete) IgrisGreen else Color.White,
                    radius = 4.dp.toPx(),
                    center = tipOffset
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    timerState.running -> IgrisSoftRed
                    isComplete -> IgrisGreenSurface
                    timerState.ranSeconds > 0 -> IgrisGoldSurface
                    else -> IgrisPanelAlt
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        timerState.running -> IgrisHeroBorder
                        isComplete -> IgrisGreenBorder
                        timerState.ranSeconds > 0 -> IgrisGoldBorder
                        else -> IgrisBorderSubtle
                    }
                )
            ) {
                Text(
                    text = statusText,
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = timeText,
                fontFamily = DmMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (ringSize >= 250.dp) 44.sp else 36.sp,
                color = IgrisText,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}% • ${DateUtils.formatMMSS(elapsedSec)} logged",
                fontFamily = DmMonoFontFamily,
                fontSize = 11.sp,
                color = IgrisMuted
            )
        }
    }
}

@Composable
fun FlipClockFocusDisplay(
    timerState: FocusTimerState,
    large: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (hh, mm, ss) = DateUtils.formatHHMMSS(timerState.remainingSeconds)
    val totalSec = maxOf(1, timerState.initialSeconds)
    val elapsedSec = (totalSec - timerState.remainingSeconds).coerceIn(0, totalSec)
    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    val isComplete = timerState.remainingSeconds == 0 && !timerState.running

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlipCountdownBox(hh, "HRS", modifier = Modifier.weight(1f), large = large)
            Text(
                text = ":",
                fontFamily = DmMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (large) 34.sp else 24.sp,
                color = if (timerState.running) IgrisRedBright else IgrisRed
            )
            FlipCountdownBox(mm, "MIN", modifier = Modifier.weight(1f), large = large)
            Text(
                text = ":",
                fontFamily = DmMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (large) 34.sp else 24.sp,
                color = if (timerState.running) IgrisRedBright else IgrisRed
            )
            FlipCountdownBox(ss, "SEC", modifier = Modifier.weight(1f), large = large)
        }
        Spacer(Modifier.height(16.dp))
        LinearFocusProgressBar(
            progress = progress,
            elapsedSeconds = elapsedSec,
            totalSeconds = totalSec,
            completed = isComplete
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusTimerPanel(
    timerState: FocusTimerState,
    onToggleRun: () -> Unit,
    onStopAndSave: () -> Unit,
    onReset: () -> Unit,
    onFullscreen: () -> Unit,
    onSetMinutes: (Int) -> Unit,
    onUpdateSessionMeta: (subject: String, topic: String) -> Unit,
    onToggleFlipMode: (Boolean) -> Unit,
    onUnlinkMission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var customMins by remember(timerState.initialSeconds) {
        mutableStateOf((timerState.initialSeconds / 60).coerceAtLeast(1).toString())
    }
    val plannedMins = maxOf(1, timerState.initialSeconds / 60)
    val hasTrackableTime = timerState.running || timerState.ranSeconds > 0
    val isComplete = timerState.remainingSeconds == 0 && !timerState.running

    val startPauseLabel = when {
        timerState.running -> "Pause"
        timerState.ranSeconds > 0 && !isComplete -> "Resume"
        else -> "Start Focus"
    }

    IgrisCard(
        modifier = modifier,
        borderColor = if (timerState.running) IgrisHeroBorder else IgrisLine
    ) {
        // Header + Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                EyebrowLabel(
                    text = if (timerState.missionLabel.isNotBlank()) {
                        "DAILY MISSION • ${timerState.subject.uppercase()}"
                    } else {
                        "STUDY SESSION • ${timerState.subject.uppercase()}"
                    },
                    color = if (timerState.running) IgrisRedBright else IgrisMuted
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = timerState.missionLabel.ifBlank {
                        timerState.topic.ifBlank { "$plannedMins min ${timerState.subject} Block" }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = IgrisText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            FocusTimerModeToggle(
                flipClockMode = timerState.flipClockMode,
                onSelectMode = onToggleFlipMode
            )
        }

        if (timerState.missionId != null || timerState.missionLabel.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IgrisSoftRed,
                border = BorderStroke(1.dp, IgrisHeroBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Linked mission",
                            tint = IgrisRedBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (timerState.missionId != null) {
                                "Linked mission: ${timerState.missionLabel} (${plannedMins}m)"
                            } else {
                                "Synced from Daily Mission draft: ${timerState.missionLabel} (${plannedMins}m)"
                            },
                            fontFamily = DmMonoFontFamily,
                            fontSize = 11.sp,
                            color = IgrisText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (timerState.missionId != null && !timerState.running) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unlink mission",
                            tint = IgrisMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onUnlinkMission() }
                                .testTag("unlink_mission_button")
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Main Visual Display (Ring Progress Mode vs Flip Clock Mode)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = timerState.flipClockMode,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "focus_timer_mode_transition"
            ) { isFlip ->
                if (isFlip) {
                    FlipClockFocusDisplay(timerState = timerState, large = false)
                } else {
                    val totalSec = maxOf(1, timerState.initialSeconds)
                    val elapsedSec = (totalSec - timerState.remainingSeconds).coerceIn(0, totalSec)
                    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularFocusProgressDisplay(timerState = timerState, ringSize = 220.dp)
                        Spacer(Modifier.height(14.dp))
                        LinearFocusProgressBar(
                            progress = progress,
                            elapsedSeconds = elapsedSec,
                            totalSeconds = totalSec,
                            completed = isComplete
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Primary Start / Stop Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleRun,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState.running) IgrisGoldSurface else IgrisRed,
                    contentColor = if (timerState.running) IgrisGold else Color.White
                ),
                border = if (timerState.running) BorderStroke(1.dp, IgrisGoldBorder) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("focus_start_pause_button")
            ) {
                Icon(
                    imageVector = if (timerState.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = startPauseLabel
                )
                Spacer(Modifier.width(6.dp))
                Text(startPauseLabel, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onStopAndSave,
                enabled = hasTrackableTime,
                border = BorderStroke(
                    1.dp,
                    if (hasTrackableTime) IgrisRed else IgrisBorderSubtle
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (hasTrackableTime) IgrisSoftRed else Color.Transparent,
                    contentColor = if (hasTrackableTime) Color.White else IgrisMuted
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("focus_stop_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop and save study session",
                    tint = if (hasTrackableTime) IgrisRedBright else IgrisMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Stop",
                    fontWeight = FontWeight.Bold,
                    color = if (hasTrackableTime) IgrisText else IgrisMuted
                )
            }

            OutlinedButton(
                onClick = onReset,
                border = BorderStroke(1.dp, IgrisBorderSubtle),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("focus_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset timer",
                    tint = IgrisText,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedButton(
                onClick = onFullscreen,
                border = BorderStroke(1.dp, IgrisBorderSubtle),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("focus_fullscreen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Open fullscreen focus mode",
                    tint = IgrisGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = IgrisLine)
        Spacer(Modifier.height(14.dp))

        // Study Session Tracking Setup
        EyebrowLabel("STUDY SUBJECT & TOPIC")
        Spacer(Modifier.height(8.dp))
        ChipSelectorRow(
            options = NeetSyllabus.subjectsList,
            selected = timerState.subject,
            onSelect = { onUpdateSessionMeta(it, timerState.topic) }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = timerState.topic,
            onValueChange = { onUpdateSessionMeta(timerState.subject, it) },
            label = { Text("Session topic or chapter (optional)") },
            placeholder = { Text("e.g., Rotational Motion PYQs", color = IgrisMuted) },
            colors = igrisTextFieldColors(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timer_topic_input")
        )

        Spacer(Modifier.height(14.dp))
        EyebrowLabel("DURATION PRESETS")
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 25, 50, 90, 180).forEach { mins ->
                val isSelected = timerState.initialSeconds == mins * 60 && timerState.missionId == null
                Surface(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            customMins = mins.toString()
                            onSetMinutes(mins)
                        }
                        .testTag("timer_preset_$mins"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) IgrisSoftRed else IgrisSurfaceDark,
                    border = BorderStroke(1.dp, if (isSelected) IgrisRed else IgrisBorderSubtle)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            fontFamily = DmMonoFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else IgrisText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customMins,
                onValueChange = { customMins = it.filter { c -> c.isDigit() } },
                label = { Text("Custom minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = igrisTextFieldColors(),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("timer_custom_minutes_input")
            )
            OutlinedButton(
                onClick = {
                    customMins.toIntOrNull()?.let { onSetMinutes(it) }
                },
                border = BorderStroke(1.dp, IgrisRed),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("timer_custom_set_button")
            ) {
                Text("Set", color = IgrisRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
