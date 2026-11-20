package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.model.MissionTask
import com.example.data.model.NeetSyllabus
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.SectionHeader
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.components.subjectTone
import com.example.ui.state.FocusTimerState
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisHeroBorder
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedBright
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisSurfaceDark
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils
import com.example.util.NotificationHelper

@Composable
fun MissionsScreen(
    bundle: IgrisDataBundle,
    timerState: FocusTimerState,
    viewModel: IgrisViewModel,
    onLaunchFocus: () -> Unit
) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.showToast(if (granted) "Reminder notifications enabled." else "Notifications denied — alerts will show inside app.")
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Open, 1 = Closed
    var textInput by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var missionType by remember { mutableStateOf("Practice") }
    var focusMinsInput by remember { mutableStateOf("45") }
    var reminderInput by remember { mutableStateOf("") } // YYYY-MM-DDTHH:mm

    val today = DateUtils.today()
    val filtered = remember(bundle.tasks, selectedTab) {
        if (selectedTab == 0) bundle.tasks.filter { !it.done }
        else bundle.tasks.filter { it.done }
    }

    val subjectGroups = remember(filtered) {
        listOf("Physics", "Chemistry", "Biology", "General").mapNotNull { sub ->
            val subTasks = filtered.filter { it.subject == sub }
            if (subTasks.isEmpty()) null else sub to subTasks
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "MISSION CONTROL",
                title = "Daily Missions",
                subtitle = "Timings sync live with Focus Room. Tap any open mission or its timer pill to launch.",
                trailing = {
                    OutlinedButton(
                        onClick = { viewModel.generateWeakMissions(bundle) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("generate_weak_missions_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Weak -> Missions", color = IgrisText, fontSize = 11.sp)
                    }
                }
            )
        }

        // Create Mission Card
        item {
            IgrisCard {
                EyebrowLabel("NEW MISSION")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { next ->
                        textInput = next
                        DateUtils.suggestMissionSubject(next)?.let { subject = it }
                        DateUtils.suggestMissionType(next)?.let { missionType = it }
                        val detectedMins = DateUtils.suggestMissionFocusMinutes(next)
                        if (detectedMins != null) {
                            focusMinsInput = detectedMins.toString()
                        }
                        val effectiveMins = detectedMins ?: focusMinsInput.toIntOrNull()
                        viewModel.syncMissionDraftToFocusTimer(effectiveMins, subject, next)
                    },
                    label = { Text("Mission (e.g. Kinematics 45m or NCERT Cell 1h)") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mission_text_input")
                )
                Spacer(Modifier.height(10.dp))
                EyebrowLabel("SUBJECT")
                Spacer(Modifier.height(6.dp))
                ChipSelectorRow(
                    options = listOf("Physics", "Chemistry", "Biology", "General"),
                    selected = subject,
                    onSelect = { nextSubject ->
                        subject = nextSubject
                        viewModel.syncMissionDraftToFocusTimer(
                            minutes = focusMinsInput.toIntOrNull(),
                            subject = nextSubject,
                            label = textInput
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                EyebrowLabel("BLOCK TYPE")
                Spacer(Modifier.height(6.dp))
                ChipSelectorRow(
                    options = NeetSyllabus.missionTypes,
                    selected = missionType,
                    onSelect = { missionType = it }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = focusMinsInput,
                        onValueChange = { raw ->
                            val clean = raw.filter { ch -> ch.isDigit() }
                            focusMinsInput = clean
                            viewModel.syncMissionDraftToFocusTimer(
                                minutes = clean.toIntOrNull(),
                                subject = subject,
                                label = textInput
                            )
                        },
                        label = { Text("Focus Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.42f)
                            .testTag("mission_focus_mins_input")
                    )
                    OutlinedTextField(
                        value = reminderInput,
                        onValueChange = { reminderInput = it },
                        label = { Text("Reminder (YYYY-MM-DDTHH:mm)") },
                        placeholder = { Text("${today}T18:00") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(0.58f)
                    )
                }

                // Live Focus Room Link Bar
                val draftMins = focusMinsInput.toIntOrNull()?.takeIf { it > 0 }
                val syncedMins = maxOf(1, timerState.initialSeconds / 60)
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (timerState.running) IgrisSoftRed else IgrisSurfaceDark,
                    border = BorderStroke(1.dp, if (timerState.running) IgrisRed else IgrisBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!timerState.running && draftMins != null) {
                                viewModel.syncMissionDraftToFocusTimer(draftMins, subject, textInput)
                            }
                            onLaunchFocus()
                        }
                        .testTag("draft_focus_sync_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus Room timer link",
                                tint = if (timerState.running) IgrisRedBright else IgrisGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    timerState.running -> "Focus running: ${DateUtils.formatMMSS(timerState.remainingSeconds)} left"
                                    draftMins != null -> "Synced to Focus Room: ${draftMins}m • $subject"
                                    else -> "Focus Room timer ready: ${syncedMins}m"
                                },
                                fontFamily = DmMonoFontFamily,
                                fontSize = 11.sp,
                                color = IgrisText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Open Focus →",
                            fontFamily = DmMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = IgrisRedBright
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                if (reminderInput.isNotBlank() &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.addMission(
                                    text = textInput,
                                    subject = subject,
                                    missionType = missionType,
                                    focusMins = focusMinsInput.toIntOrNull(),
                                    reminderAt = reminderInput.trim().takeIf { it.isNotBlank() }
                                )
                                textInput = ""
                                reminderInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_mission_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Mission")
                    }
                    if (!NotificationHelper.hasNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            border = BorderStroke(1.dp, IgrisBorderSubtle),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = "Allow notifications", tint = IgrisGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Alerts", color = IgrisText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Open / Closed Tabs
        item {
            val openCount = bundle.tasks.count { !it.done }
            val closedCount = bundle.tasks.count { it.done }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IgrisRed,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = IgrisRed
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Open ($openCount)") },
                    modifier = Modifier.testTag("missions_tab_open")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Closed ($closedCount)") },
                    modifier = Modifier.testTag("missions_tab_closed")
                )
            }
        }

        if (subjectGroups.isEmpty()) {
            item {
                IgrisCard {
                    Text(
                        text = if (selectedTab == 0) "No open missions right now. Add a mission above or queue weak chapters." else "No closed missions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                }
            }
        } else {
            items(subjectGroups, key = { it.first }) { (subName, subTasks) ->
                IgrisCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(subName, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                        IgrisTag("${subTasks.size} task(s)", tone = subjectTone(subName))
                    }
                    Spacer(Modifier.height(10.dp))
                    NeetSyllabus.missionTypes.forEach { blockType ->
                        val blockTasks = subTasks.filter {
                            (it.missionType.ifBlank { "Practice" }) == blockType
                        }
                        if (blockTasks.isNotEmpty()) {
                            EyebrowLabel(blockType, color = IgrisGold, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                            blockTasks.forEach { task ->
                                val isLinkedInFocus = timerState.missionId == task.id
                                MissionRowItem(
                                    task = task,
                                    today = today,
                                    isLinkedInFocus = isLinkedInFocus,
                                    isRunningInFocus = isLinkedInFocus && timerState.running,
                                    onToggle = { viewModel.toggleMission(task.id, !task.done) },
                                    onOpenInFocus = {
                                        viewModel.startMissionFocus(task, autoStart = false)
                                        onLaunchFocus()
                                    },
                                    onLaunchFocus = {
                                        viewModel.startMissionFocus(task, autoStart = true)
                                        onLaunchFocus()
                                    },
                                    onClearReminder = { viewModel.clearMissionReminder(task.id) },
                                    onDelete = { viewModel.deleteMission(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionRowItem(
    task: MissionTask,
    today: String,
    isLinkedInFocus: Boolean,
    isRunningInFocus: Boolean,
    onToggle: () -> Unit,
    onOpenInFocus: () -> Unit,
    onLaunchFocus: () -> Unit,
    onClearReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val displayMins = (task.focusMins ?: 25).coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isLinkedInFocus) IgrisSoftRed.copy(alpha = 0.45f) else Color.Transparent,
        border = if (isLinkedInFocus) BorderStroke(1.dp, IgrisHeroBorder) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mission_toggle_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.done) "Mark open" else "Mark done",
                    tint = if (task.done) IgrisGreen else IgrisMuted
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (!task.done) onOpenInFocus() else onToggle()
                    }
                    .padding(vertical = 4.dp)
                    .testTag("mission_row_body_${task.id}")
            ) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) IgrisMuted else IgrisText,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (isLinkedInFocus) {
                        IgrisTag(
                            text = if (isRunningInFocus) "ACTIVE IN FOCUS" else "LINKED TO TIMER",
                            tone = "red"
                        )
                    }
                    if (!task.done && task.date.isNotBlank() && task.date < today) {
                        IgrisTag("Carried • ${task.date}", tone = "warn")
                    }
                    if (!task.reminderAt.isNullOrBlank()) {
                        IgrisTag("⏰ ${task.reminderAt.replace('T', ' ')}", tone = "gold")
                    }
                    if (task.source == "weak-auto") {
                        IgrisTag("Weak Auto", tone = "red")
                    }
                }
            }
            if (!task.done) {
                OutlinedButton(
                    onClick = onLaunchFocus,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, if (isLinkedInFocus) IgrisRedBright else IgrisRed),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .testTag("mission_focus_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start ${displayMins}m focus timer",
                        tint = IgrisRedBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text("${displayMins}m", fontFamily = DmMonoFontFamily, fontSize = 11.sp, color = IgrisText)
                }
            }
            if (!task.reminderAt.isNullOrBlank()) {
                IconButton(onClick = onClearReminder) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = "Clear reminder", tint = IgrisMuted, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete mission", tint = IgrisMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
