package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.MissionTask
import com.example.ui.state.FocusTimerState
import com.example.ui.state.PreviewAttachment
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBg
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBlueBorder
import com.example.ui.theme.IgrisBlueSurface
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisEyebrow
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
import com.example.ui.theme.IgrisPurple
import com.example.ui.theme.IgrisPurpleBorder
import com.example.ui.theme.IgrisPurpleSurface
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedBright
import com.example.ui.theme.IgrisRedDark
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisSurfaceDark
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils
import com.example.util.LocalFileHelper
import java.io.File

@Composable
fun IgrisCard(
    modifier: Modifier = Modifier,
    borderColor: Color = IgrisLine,
    backgroundColor: Color = IgrisPanel,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun EyebrowLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = IgrisEyebrow
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            EyebrowLabel(eyebrow)
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = IgrisText
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IgrisMuted
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
    valueColor: Color = IgrisText,
    highlightBorder: Color = IgrisLine
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = IgrisPanel,
        border = BorderStroke(1.dp, highlightBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            EyebrowLabel(label)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = DmMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = IgrisMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IgrisTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: String = "neutral"
) {
    val (bg, fg, border) = when (tone.lowercase()) {
        "red", "weak", "hard", "physics", "danger" -> Triple(IgrisSoftRed, Color(0xFFFCA5A5), IgrisRedDark)
        "green", "strong", "easy", "biology", "done" -> Triple(IgrisGreenSurface, IgrisGreen, IgrisGreenBorder)
        "gold", "medium", "imp", "chemistry", "warn" -> Triple(IgrisGoldSurface, IgrisGold, IgrisGoldBorder)
        "blue", "info", "practice", "lecture" -> Triple(IgrisBlueSurface, IgrisBlue, IgrisBlueBorder)
        "purple", "revision" -> Triple(IgrisPurpleSurface, IgrisPurple, IgrisPurpleBorder)
        else -> Triple(IgrisPanelAlt, IgrisMuted, IgrisBorderSubtle)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            fontFamily = DmMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun subjectTone(subject: String): String = when (subject.lowercase()) {
    "physics" -> "blue"
    "chemistry" -> "gold"
    "biology" -> "green"
    else -> "neutral"
}

@Composable
fun igrisTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IgrisRed,
    unfocusedBorderColor = IgrisBorderSubtle,
    focusedLabelColor = IgrisRed,
    unfocusedLabelColor = IgrisMuted,
    cursorColor = IgrisRed,
    focusedTextColor = IgrisText,
    unfocusedTextColor = IgrisText,
    focusedContainerColor = IgrisSurfaceDark,
    unfocusedContainerColor = IgrisSurfaceDark
)

@Composable
fun HorizontalProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    color: Color = IgrisRed,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = IgrisText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = valueText,
                fontFamily = DmMonoFontFamily,
                fontSize = 12.sp,
                color = IgrisMuted
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(IgrisPanelAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun FlipCountdownBox(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val flipAnim = remember { Animatable(0f) }
    var previousValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != previousValue) {
            previousValue = value
            flipAnim.snapTo(-28f)
            flipAnim.animateTo(0f, animationSpec = tween(durationMillis = 240))
        }
    }

    Surface(
        modifier = modifier.graphicsLayer {
            rotationX = flipAnim.value
            cameraDistance = 12f * density
        },
        shape = RoundedCornerShape(if (large) 16.dp else 12.dp),
        color = IgrisSurfaceDark,
        border = BorderStroke(1.dp, IgrisHeroBorder)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(IgrisPanelAlt, IgrisSurfaceDark)
                        )
                    )
                    .padding(
                        horizontal = if (large) 16.dp else 10.dp,
                        vertical = if (large) 16.dp else 10.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontFamily = DmMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (large) 42.sp else 24.sp,
                    color = IgrisText,
                    maxLines = 1
                )
                // Split-flap mechanical center hinge divider
                HorizontalDivider(
                    color = IgrisBg.copy(alpha = 0.85f),
                    thickness = 1.5.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            HorizontalDivider(color = IgrisLine, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IgrisPanel)
                    .padding(vertical = if (large) 6.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unit.uppercase(),
                    fontFamily = DmMonoFontFamily,
                    fontSize = if (large) 10.sp else 9.sp,
                    color = IgrisMuted,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

data class BarDatum(val label: String, val value: Float, val valueLabel: String)

@Composable
fun BarChartCanvas(
    items: List<BarDatum>,
    barColor: Color = IgrisRed,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(1f, items.maxOfOrNull { it.value } ?: 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                val frac = (item.value / maxVal).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = item.valueLabel,
                        fontFamily = DmMonoFontFamily,
                        fontSize = 9.sp,
                        color = IgrisMuted,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(maxOf(6f, 92f * frac).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(barColor, barColor.copy(alpha = 0.55f))
                                )
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = IgrisLine)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Text(
                    text = item.label,
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.sp,
                    color = IgrisMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class LineDatum(val label: String, val value: Float)

@Composable
fun LineChartCanvas(
    points: List<LineDatum>,
    maxValue: Float = 720f,
    targetValue: Float? = null,
    lineColor: Color = IgrisRed,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Log at least 1 test to see the score trend.", color = IgrisMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            val safeMax = maxOf(1f, maxValue)

            // Reference horizontal grid lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = h - (h * frac)
                drawLine(
                    color = IgrisLine,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Target line
            if (targetValue != null) {
                val ty = h - ((targetValue.coerceIn(0f, safeMax) / safeMax) * h)
                drawLine(
                    color = IgrisGold.copy(alpha = 0.7f),
                    start = Offset(0f, ty),
                    end = Offset(w, ty),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            val coords = points.mapIndexed { idx, p ->
                val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
                val y = h - ((p.value.coerceIn(0f, safeMax) / safeMax) * h)
                Offset(x, y)
            }

            if (coords.size > 1) {
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = IgrisBg, radius = 6.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.take(6).forEach { p ->
                Text(
                    text = "${p.label} (${p.value.toInt()})",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 9.sp,
                    color = IgrisMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TopFocusBanner(
    timerState: FocusTimerState,
    onToggleRun: () -> Unit,
    onOpenFocus: () -> Unit,
    onFullscreen: () -> Unit,
    onHide: () -> Unit
) {
    if (!timerState.running || timerState.bannerHidden) return
    val (hh, mm, ss) = DateUtils.formatHHMMSS(timerState.remainingSeconds)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = IgrisSoftRed,
        border = BorderStroke(1.dp, IgrisRed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenFocus() }
            ) {
                Icon(Icons.Default.Timer, contentDescription = "Focus timer", tint = IgrisRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$hh:$mm:$ss",
                        fontFamily = DmMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = IgrisText
                    )
                    Text(
                        text = timerState.missionLabel.ifBlank {
                            timerState.topic.ifBlank { "${timerState.subject} • Deep Focus Active" }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IgrisMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleRun, modifier = Modifier.testTag("banner_pause_button")) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause focus timer", tint = IgrisText)
                }
                IconButton(onClick = onFullscreen, modifier = Modifier.testTag("banner_fullscreen_button")) {
                    Icon(Icons.Default.Timer, contentDescription = "Fullscreen focus", tint = IgrisGold)
                }
                IconButton(onClick = onHide, modifier = Modifier.testTag("banner_hide_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Hide banner", tint = IgrisMuted)
                }
            }
        }
    }
}

@Composable
fun ReminderAlertBanner(
    task: MissionTask?,
    onSnooze: () -> Unit,
    onMarkDone: () -> Unit,
    onDismiss: () -> Unit
) {
    if (task == null) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = IgrisGoldSurface,
        border = BorderStroke(1.dp, IgrisGoldBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = "Reminder alert", tint = IgrisGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DUE: ${task.subject}",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IgrisGold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss reminder", tint = IgrisMuted, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyLarge,
                color = IgrisText
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSnooze,
                    border = BorderStroke(1.dp, IgrisGoldBorder),
                    modifier = Modifier.testTag("reminder_snooze_button")
                ) {
                    Text("Snooze +10m", color = IgrisGold)
                }
                if (!task.id.startsWith("rev_")) {
                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                        modifier = Modifier.testTag("reminder_done_button")
                    ) {
                        Text("Mark Done")
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenFocusOverlay(
    timerState: FocusTimerState,
    onToggleRun: () -> Unit,
    onStopAndSave: () -> Unit,
    onReset: () -> Unit,
    onToggleFlipMode: (Boolean) -> Unit,
    onExit: () -> Unit
) {
    if (!timerState.fullscreen) return
    val context = LocalContext.current
    BackHandler { onExit() }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val totalSec = maxOf(1, timerState.initialSeconds)
    val elapsedSec = (totalSec - timerState.remainingSeconds).coerceIn(0, totalSec)
    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    val isComplete = timerState.remainingSeconds == 0 && !timerState.running
    val hasTrackableTime = timerState.running || timerState.ranSeconds > 0

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IgrisBg)
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                EyebrowLabel("DISTRACTION-FREE LOCK • SCREEN AWAKE", color = IgrisRed)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = timerState.missionLabel.ifBlank {
                        timerState.topic.ifBlank { "${timerState.subject.uppercase()} DEEP FOCUS" }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = IgrisText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                FocusTimerModeToggle(
                    flipClockMode = timerState.flipClockMode,
                    onSelectMode = onToggleFlipMode
                )
                Spacer(Modifier.height(26.dp))

                AnimatedContent(
                    targetState = timerState.flipClockMode,
                    transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
                    label = "fs_timer_mode"
                ) { isFlip ->
                    if (isFlip) {
                        FlipClockFocusDisplay(timerState = timerState, large = true)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularFocusProgressDisplay(timerState = timerState, ringSize = 264.dp)
                            Spacer(Modifier.height(18.dp))
                            LinearFocusProgressBar(
                                progress = progress,
                                elapsedSeconds = elapsedSec,
                                totalSeconds = totalSec,
                                completed = isComplete
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            .height(50.dp)
                            .testTag("fs_timer_toggle_button")
                    ) {
                        Icon(
                            if (timerState.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (timerState.running) "Pause" else if (timerState.ranSeconds > 0 && !isComplete) "Resume" else "Start"
                        )
                    }
                    OutlinedButton(
                        onClick = onStopAndSave,
                        enabled = hasTrackableTime,
                        border = BorderStroke(1.dp, if (hasTrackableTime) IgrisRed else IgrisBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("fs_timer_stop_save_button")
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop and save",
                            tint = if (hasTrackableTime) IgrisRedBright else IgrisMuted
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Stop", color = if (hasTrackableTime) IgrisText else IgrisMuted)
                    }
                    OutlinedButton(
                        onClick = onReset,
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("fs_timer_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = IgrisText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.testTag("fs_timer_exit_button")
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = null, tint = IgrisMuted)
                    Spacer(Modifier.width(6.dp))
                    Text("Exit Fullscreen", color = IgrisMuted)
                }
            }
        }
    }
}

@Composable
fun AttachmentPreviewDialog(
    attachment: PreviewAttachment?,
    onDismiss: () -> Unit
) {
    if (attachment == null) return
    val context = LocalContext.current
    val isPdf = attachment.fileType.contains("pdf", ignoreCase = true) ||
        attachment.localPath.endsWith(".pdf", ignoreCase = true)

    var pdfPages by remember(attachment.localPath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loadingPdf by remember(attachment.localPath) { mutableStateOf(isPdf) }

    BackHandler { onDismiss() }

    LaunchedEffect(attachment.localPath, isPdf) {
        if (isPdf) {
            loadingPdf = true
            pdfPages = LocalFileHelper.renderPdfPages(attachment.localPath)
            loadingPdf = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = IgrisPanel,
            border = BorderStroke(1.dp, IgrisLine)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachment.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = IgrisText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        LocalFileHelper.openExternally(context, attachment.localPath, attachment.fileType)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open externally", tint = IgrisBlue)
                    }
                    IconButton(onClick = {
                        LocalFileHelper.shareExternally(context, attachment.localPath, attachment.fileType, attachment.title)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share attachment", tint = IgrisText)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_attachment_preview")) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview", tint = IgrisMuted)
                    }
                }
                HorizontalDivider(color = IgrisLine)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IgrisBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPdf) {
                        when {
                            loadingPdf -> CircularProgressIndicator(color = IgrisRed)
                            pdfPages.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Could not render PDF inline.", color = IgrisMuted)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    LocalFileHelper.openExternally(context, attachment.localPath, attachment.fileType)
                                }) {
                                    Text("Open in PDF App")
                                }
                            }
                            else -> LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(pdfPages) { index, bmp ->
                                    Column {
                                        Text(
                                            "Page ${index + 1} / ${pdfPages.size}",
                                            fontFamily = DmMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = IgrisMuted,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "PDF page ${index + 1}",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val file = File(attachment.localPath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = attachment.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Attachment file is stored on another device.", color = IgrisMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupDialog(
    jsonText: String,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IgrisPanel,
        titleContentColor = IgrisText,
        textContentColor = IgrisMuted,
        title = { Text("Backup & Restore (JSON)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Copy or share your JSON snapshot, or paste an exported JSON backup below to restore tasks, sessions, and mistakes.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(jsonText)) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = IgrisText)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy JSON", color = IgrisText)
                    }
                    OutlinedButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "IGRIS Backup")
                                putExtra(Intent.EXTRA_TEXT, jsonText)
                            }
                            context.startActivity(Intent.createChooser(send, "Export IGRIS Backup"))
                        },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = IgrisText)
                        Spacer(Modifier.width(6.dp))
                        Text("Share", color = IgrisText)
                    }
                }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste backup JSON to import") },
                    minLines = 3,
                    maxLines = 5,
                    colors = igrisTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (importText.isNotBlank()) {
                        onImportJson(importText)
                        onDismiss()
                    }
                },
                enabled = importText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IgrisRed)
            ) {
                Text("Import JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = IgrisMuted)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSelectorRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Surface(
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .clickable { onSelect(opt) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) IgrisSoftRed else IgrisPanelAlt,
                border = BorderStroke(1.dp, if (isSelected) IgrisRed else IgrisBorderSubtle)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else IgrisMuted
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
