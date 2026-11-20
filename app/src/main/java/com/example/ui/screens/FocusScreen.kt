package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NeetSyllabus
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.FocusTimerPanel
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
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils
import com.example.util.NotificationHelper

@Composable
fun FocusScreen(
    bundle: IgrisDataBundle,
    timerState: FocusTimerState,
    viewModel: IgrisViewModel
) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.showToast(if (granted) "Focus completion notifications enabled." else "Notifications denied.")
    }

    var manualSubject by remember { mutableStateOf("Physics") }
    var manualMins by remember { mutableStateOf("60") }
    var manualQs by remember { mutableStateOf("40") }
    var manualTopic by remember { mutableStateOf("") }

    val today = DateUtils.today()
    val todaySessions = remember(bundle.sessions, today) {
        bundle.sessions.filter { it.date == today }.reversed()
    }
    val openMissions = remember(bundle.tasks) {
        bundle.tasks.filter { !it.done }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "DEEP WORK ENGINE",
                title = "Focus Room",
                subtitle = "Track study sessions with visual ring progress or split-flap flip clock mode.",
                trailing = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
                        OutlinedButton(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            border = BorderStroke(1.dp, IgrisBorderSubtle)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Alerts", color = IgrisText, fontSize = 11.sp)
                        }
                    }
                }
            )
        }

        // Compose Focus Timer Component (Progress Ring + Flip Clock Mode + Session Tracker)
        item {
            FocusTimerPanel(
                timerState = timerState,
                onToggleRun = { viewModel.toggleTimerRunning(context) },
                onStopAndSave = { viewModel.stopAndSaveTimerSession(context) },
                onReset = { viewModel.resetCurrentTimer() },
                onFullscreen = { viewModel.setTimerFullscreen(true) },
                onSetMinutes = { viewModel.setTimerMinutes(it) },
                onUpdateSessionMeta = { sub, topic -> viewModel.setTimerSessionMeta(sub, topic) },
                onToggleFlipMode = { viewModel.setFlipClockMode(it) },
                onUnlinkMission = { viewModel.unlinkMissionFromTimer() }
            )
        }

        // Daily Missions linked directly to Focus Timer
        if (openMissions.isNotEmpty()) {
            item {
                IgrisCard {
                    EyebrowLabel("DAILY MISSIONS • TAP TO LOAD OR START")
                    Spacer(Modifier.height(2.dp))
                    Text("Link any open mission directly to the timer", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                    Spacer(Modifier.height(10.dp))
                    openMissions.forEach { task ->
                        val planned = task.focusMins ?: 25
                        val isLinked = timerState.missionId == task.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IgrisTag(task.subject, tone = subjectTone(task.subject))
                                    if (isLinked) {
                                        IgrisTag(
                                            text = if (timerState.running) "ACTIVE" else "LINKED",
                                            tone = "red"
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(task.text, style = MaterialTheme.typography.bodyLarge, color = IgrisText)
                                Text(
                                    text = if (task.focusMins != null && task.focusMins > 0) {
                                        "${task.focusMins} min planned • ${task.missionType}"
                                    } else {
                                        "Default 25 min • ${task.missionType}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IgrisMuted
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.startMissionFocus(task, autoStart = false) },
                                border = BorderStroke(1.dp, if (isLinked) IgrisRed else IgrisBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isLinked) "Loaded" else "Load",
                                    color = if (isLinked) IgrisRed else IgrisText,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.startMissionFocus(task, autoStart = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start mission timer", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${planned}m")
                            }
                        }
                    }
                }
            }
        }

        // Manual Session Logger
        item {
            IgrisCard {
                EyebrowLabel("MANUAL LOG")
                Spacer(Modifier.height(2.dp))
                Text("Log Offline Study Session", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                Spacer(Modifier.height(10.dp))
                ChipSelectorRow(
                    options = NeetSyllabus.subjectsList,
                    selected = manualSubject,
                    onSelect = { manualSubject = it }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualMins,
                        onValueChange = { manualMins = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = manualQs,
                        onValueChange = { manualQs = it.filter { c -> c.isDigit() } },
                        label = { Text("MCQs Solved") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualTopic,
                    onValueChange = { manualTopic = it },
                    label = { Text("Topic / Chapter") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.logManualSession(
                            subject = manualSubject,
                            minutes = manualMins.toIntOrNull() ?: 0,
                            topic = manualTopic,
                            questions = manualQs.toIntOrNull() ?: 0
                        )
                        manualTopic = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("log_manual_session_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Session")
                }
            }
        }

        // Today's Sessions
        item {
            EyebrowLabel("TODAY'S SESSIONS (${todaySessions.size})")
        }
        if (todaySessions.isEmpty()) {
            item {
                IgrisCard {
                    Text("No sessions logged today yet.", style = MaterialTheme.typography.bodyMedium, color = IgrisMuted)
                }
            }
        } else {
            items(todaySessions, key = { it.id }) { session ->
                IgrisCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IgrisTag(session.subject, tone = subjectTone(session.subject))
                                Text(
                                    "${session.minutes}m • ${session.questions} Qs",
                                    fontFamily = DmMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = IgrisGreen
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(session.topic, style = MaterialTheme.typography.bodyLarge, color = IgrisText)
                        }
                        IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete session", tint = IgrisMuted)
                        }
                    }
                }
            }
        }
    }
}
