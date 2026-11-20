package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NeetSyllabus
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.BarDatum
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.FlipCountdownBox
import com.example.ui.components.HorizontalProgressRow
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.StatCard
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.components.subjectTone
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisDestination
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisHeroBorder
import com.example.ui.theme.IgrisLine
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPanel
import com.example.ui.theme.IgrisPanelAlt
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedDark
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel,
    onNavigate: (IgrisDestination) -> Unit
) {
    val profile = bundle.profile
    val today = DateUtils.today()
    val todayTasks = remember(bundle.tasks, today) { bundle.tasks.filter { it.date == today } }
    val doneToday = todayTasks.count { it.done }
    val totalToday = todayTasks.size
    val todayFocusMin = remember(bundle.sessions, today) {
        bundle.sessions.filter { it.date == today }.sumOf { it.minutes }
    }
    val todayQs = remember(bundle.sessions, today) {
        bundle.sessions.filter { it.date == today }.sumOf { it.questions }
    }
    val openMistakes = remember(bundle.mistakes) { bundle.mistakes.count { !it.closed } }
    val streak = remember(bundle.sessions, bundle.tasks) { viewModel.streakCount(bundle) }

    val dueRevs = remember(bundle.revisions) { viewModel.dueRevisions(bundle.revisions) }
    val weakBacklog = remember(bundle.chapters) { viewModel.backlogDueList(bundle.chapters) }

    var tickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tickMs = System.currentTimeMillis()
        }
    }
    val examDate = profile?.examDate ?: "2027-05-02"
    val countdown = remember(examDate, tickMs) { DateUtils.countdownParts(examDate) }

    var showRankDialog by remember { mutableStateOf(false) }
    var showExamDateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = IgrisPanel,
                border = BorderStroke(1.dp, IgrisHeroBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF281017), IgrisPanel, Color(0xFF15121B))
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            EyebrowLabel("NEET COMMAND CENTER • ${DateUtils.dateText(today)}", color = IgrisRed)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "System Active, ${profile?.name ?: "IGRIS"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = IgrisText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Execute daily missions, crush weak chapters, and lock spaced revisions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IgrisMuted
                            )
                        }
                        IconButton(
                            onClick = { showRankDialog = true },
                            modifier = Modifier.testTag("edit_rank_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit rank targets", tint = IgrisGold)
                        }
                    }

                    // Rank trajectory badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRankDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = IgrisSoftRed.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, IgrisHeroBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                EyebrowLabel("CURRENT RANK")
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = profile?.currentRank ?: "4 Lakh",
                                    fontFamily = DmMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = IgrisText
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRankDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF14251F),
                            border = BorderStroke(1.dp, Color(0xFF256F51))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                EyebrowLabel("TARGET RANK", color = IgrisGreen)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = profile?.upcomingRank ?: "10k",
                                    fontFamily = DmMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = IgrisGreen
                                )
                            }
                        }
                    }

                    // Live Countdown Strip
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EyebrowLabel("NEET 2027 COUNTDOWN • $examDate")
                            TextButton(
                                onClick = { showExamDateDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("edit_exam_date_button")
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = IgrisMuted, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Set Date", fontSize = 11.sp, color = IgrisMuted)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlipCountdownBox(countdown.days.toString(), "DAYS", modifier = Modifier.weight(1f))
                            FlipCountdownBox("%02d".format(countdown.hours), "HRS", modifier = Modifier.weight(1f))
                            FlipCountdownBox("%02d".format(countdown.minutes), "MINS", modifier = Modifier.weight(1f))
                            FlipCountdownBox("%02d".format(countdown.seconds), "SECS", modifier = Modifier.weight(1f))
                        }
                        val paceNote = if (countdown.days > 0) {
                            val totalCh = bundle.chapters.size
                            val doneCh = bundle.chapters.count { it.done }
                            val remCh = (totalCh - doneCh).coerceAtLeast(0)
                            if (totalCh > 0) "$remCh/$totalCh chapters left • ~${String.format("%.1f", remCh / (countdown.days / 7.0).coerceAtLeast(1.0))} chapters/week pace"
                            else "Load the 74-chapter NEET syllabus in Subject HQ to track pace."
                        } else "Exam day arrived — stay calm & execute!"
                        Text(paceNote, style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                    }
                }
            }
        }

        // 4 KPI Cards (2x2 grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "STREAK",
                        value = "D-$streak",
                        note = "6h+ focus or 60%+ daily missions",
                        valueColor = IgrisGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "TODAY MISSIONS",
                        value = "$doneToday/$totalToday",
                        note = if (totalToday == 0) "No tasks queued yet" else "${(doneToday * 100 / totalToday)}% complete",
                        valueColor = IgrisGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "FOCUS TODAY",
                        value = "${todayFocusMin}m",
                        note = "$todayQs MCQs logged today",
                        valueColor = IgrisBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "OPEN MISTAKES",
                        value = "$openMistakes",
                        note = "Requires 2-clear Fix Loop",
                        valueColor = if (openMistakes > 0) IgrisRed else IgrisGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions
        item {
            IgrisCard {
                EyebrowLabel("QUICK COMMANDS")
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigate(IgrisDestination.Missions) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("qa_missions")
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null, tint = IgrisRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Mission", color = IgrisText)
                    }
                    OutlinedButton(
                        onClick = { onNavigate(IgrisDestination.Focus) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("qa_focus")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Focus Room", color = IgrisText)
                    }
                    OutlinedButton(
                        onClick = { onNavigate(IgrisDestination.Practice) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("qa_quiz")
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Practice Quiz", color = IgrisText)
                    }
                    OutlinedButton(
                        onClick = { onNavigate(IgrisDestination.Mistakes) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("qa_mistakes")
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = IgrisRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Mistake", color = IgrisText)
                    }
                    OutlinedButton(
                        onClick = { onNavigate(IgrisDestination.AiTutor) },
                        border = BorderStroke(1.dp, IgrisRedDark),
                        modifier = Modifier.testTag("qa_ai_tutor")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IgrisRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Tutor", color = IgrisText)
                    }
                }
            }
        }

        // Today's Missions Preview
        item {
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        EyebrowLabel("EXECUTION QUEUE")
                        Text("Today's Missions", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                    }
                    TextButton(onClick = { onNavigate(IgrisDestination.Missions) }) {
                        Text("View All", color = IgrisRed)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (todayTasks.isEmpty()) {
                    Text(
                        "No missions added for today yet. Tap 'Add Mission' or generate from weak chapters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                } else {
                    todayTasks.take(5).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.toggleMission(task.id, !task.done) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (task.done) "Completed" else "Pending",
                                tint = if (task.done) IgrisGreen else IgrisMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (task.done) IgrisMuted else IgrisText,
                                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            IgrisTag(task.subject, tone = subjectTone(task.subject))
                        }
                    }
                }
            }
        }

        // Focus 7-Day Bar Chart
        item {
            val days = remember(today) { DateUtils.dayKeys(7) }
            val bars = remember(bundle.sessions, days) {
                days.map { d ->
                    val mins = bundle.sessions.filter { it.date == d }.sumOf { it.minutes }
                    BarDatum(
                        label = DateUtils.dayLabel(d),
                        value = mins.toFloat(),
                        valueLabel = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                    )
                }
            }
            IgrisCard {
                EyebrowLabel("CONSISTENCY TELEMETRY")
                Spacer(Modifier.height(2.dp))
                Text("7-Day Focus Hours", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                Spacer(Modifier.height(12.dp))
                BarChartCanvas(items = bars, barColor = IgrisRed)
            }
        }

        // Subject Progress
        item {
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        EyebrowLabel("SYLLABUS MASTERY")
                        Text("Subject Progress", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                    }
                    if (bundle.chapters.isEmpty()) {
                        TextButton(onClick = { viewModel.loadNeetSyllabus() }) {
                            Text("Load 74 Ch", color = IgrisRed)
                        }
                    } else {
                        TextButton(onClick = { onNavigate(IgrisDestination.Subjects) }) {
                            Text("Subject HQ", color = IgrisRed)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeetSyllabus.subjectsList.forEach { sub ->
                        val subCh = bundle.chapters.filter { it.subject == sub }
                        val done = subCh.count { it.done }
                        val total = subCh.size
                        val pct = if (total > 0) done.toFloat() / total else 0f
                        val barColor = when (sub) {
                            "Physics" -> IgrisBlue
                            "Chemistry" -> IgrisGold
                            else -> IgrisGreen
                        }
                        HorizontalProgressRow(
                            label = sub,
                            valueText = "$done/$total ch",
                            progress = pct,
                            color = barColor
                        )
                    }
                }
            }
        }

        // Revision Due & Weak Backlog
        item {
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EyebrowLabel("RETENTION & BACKLOG")
                        Text(
                            "Due Revisions (${dueRevs.size}) & Weak (${weakBacklog.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = IgrisText
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateWeakMissions(bundle) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("dash_generate_weak_missions")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = IgrisRed)
                        Spacer(Modifier.width(4.dp))
                        Text("Queue Weak", color = IgrisText, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (dueRevs.isEmpty() && weakBacklog.isEmpty()) {
                    Text(
                        "No pending revisions or weak chapters flagged right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                } else {
                    dueRevs.take(3).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IgrisTag("REV DUE", tone = "purple")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${r.chapter} (${r.subject})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IgrisText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.markRevisionDone(r) }) {
                                Text("Done", color = IgrisGreen, fontSize = 11.sp)
                            }
                        }
                    }
                    weakBacklog.take(4).forEach { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IgrisTag("WEAK", tone = "red")
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.chapter.name, style = MaterialTheme.typography.bodyMedium, color = IgrisText)
                                Text(w.reason, style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Feed
        item {
            IgrisCard {
                EyebrowLabel("ACTIVITY LOG")
                Spacer(Modifier.height(2.dp))
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                Spacer(Modifier.height(10.dp))
                val recent = buildList {
                    bundle.sessions.takeLast(3).reversed().forEach {
                        add("${it.date}: Focused ${it.minutes}m on ${it.subject} (${it.topic})")
                    }
                    bundle.tests.takeLast(2).reversed().forEach {
                        add("${it.date}: Mock \"${it.name}\" — ${it.score}/720 (${it.accuracy}%)")
                    }
                    bundle.quizAttempts.takeLast(2).reversed().forEach {
                        add("${it.date}: Quiz \"${it.chapter}\" — ${it.score}/${it.total} (${it.accuracy}%)")
                    }
                }
                if (recent.isEmpty()) {
                    Text("No sessions or tests logged yet.", style = MaterialTheme.typography.bodyMedium, color = IgrisMuted)
                } else {
                    recent.forEachIndexed { i, line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = IgrisText)
                        if (i < recent.lastIndex) {
                            HorizontalDivider(color = IgrisLine, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }

    if (showRankDialog) {
        var curRank by remember { mutableStateOf(profile?.currentRank ?: "4 Lakh") }
        var targetRank by remember { mutableStateOf(profile?.upcomingRank ?: "10k") }
        AlertDialog(
            onDismissRequest = { showRankDialog = false },
            containerColor = IgrisPanel,
            title = { Text("Update Rank Targets", color = IgrisText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = curRank,
                        onValueChange = { curRank = it },
                        label = { Text("Current Rank") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetRank,
                        onValueChange = { targetRank = it },
                        label = { Text("Target / Upcoming Rank") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRanks(curRank, targetRank)
                        showRankDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRankDialog = false }) { Text("Cancel", color = IgrisMuted) }
            }
        )
    }

    if (showExamDateDialog) {
        var dateInput by remember { mutableStateOf(examDate) }
        AlertDialog(
            onDismissRequest = { showExamDateDialog = false },
            containerColor = IgrisPanel,
            title = { Text("Set Exam Date", color = IgrisText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter NEET exam date in YYYY-MM-DD format.", color = IgrisMuted, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Exam Date (YYYY-MM-DD)") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateExamDate(dateInput.trim())
                        showExamDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed)
                ) { Text("Save Date") }
            },
            dismissButton = {
                TextButton(onClick = { showExamDateDialog = false }) { Text("Cancel", color = IgrisMuted) }
            }
        )
    }
}
