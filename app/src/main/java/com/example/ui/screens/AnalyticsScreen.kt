package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.NeetSyllabus
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.BarDatum
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.HorizontalProgressRow
import com.example.ui.components.IgrisCard
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.LineDatum
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.state.IgrisDataBundle
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPurple
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils

@Composable
fun AnalyticsScreen(bundle: IgrisDataBundle) {
    val totalFocusHrs = remember(bundle.sessions) { bundle.sessions.sumOf { it.minutes } / 60.0 }
    val totalQs = remember(bundle.sessions) { bundle.sessions.sumOf { it.questions } }
    val doneCh = remember(bundle.chapters) { bundle.chapters.count { it.done } }
    val totalCh = bundle.chapters.size
    val rev1Done = remember(bundle.chapters) { bundle.chapters.count { it.ticks.containsKey("r1") } }

    val days = remember { DateUtils.dayKeys(7) }
    val focusBars = remember(bundle.sessions, days) {
        days.map { d ->
            val m = bundle.sessions.filter { it.date == d }.sumOf { it.minutes }
            BarDatum(DateUtils.dayLabel(d), m.toFloat(), if (m >= 60) "${m / 60}h" else "${m}m")
        }
    }
    val missionBars = remember(bundle.tasks, days) {
        days.map { d ->
            val c = bundle.tasks.count { it.done && (it.doneAt == d || it.date == d) }
            BarDatum(DateUtils.dayLabel(d), c.toFloat(), c.toString())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "PERFORMANCE TELEMETRY",
                title = "Analytics",
                subtitle = "Study volume, mission velocity, mock trend, subject balance & error clusters."
            )
        }

        // 4 KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("TOTAL FOCUS", "${String.format("%.1f", totalFocusHrs)}h", "$totalQs MCQs logged", valueColor = IgrisRed, modifier = Modifier.weight(1f))
                    StatCard(
                        "CHAPTERS DONE",
                        if (totalCh > 0) "${doneCh * 100 / totalCh}%" else "0%",
                        "$doneCh / $totalCh syllabus",
                        valueColor = IgrisGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        "REV 1 COMPLETE",
                        if (totalCh > 0) "${rev1Done * 100 / totalCh}%" else "0%",
                        "$rev1Done chapters revised",
                        valueColor = IgrisPurple,
                        modifier = Modifier.weight(1f)
                    )
                    val closedM = bundle.mistakes.count { it.closed }
                    StatCard(
                        "MISTAKES CLEARED",
                        "$closedM / ${bundle.mistakes.size}",
                        "2-clear fix loop",
                        valueColor = IgrisGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7-Day Focus Bar Chart
        item {
            IgrisCard {
                EyebrowLabel("7-DAY FOCUS MINUTES")
                Spacer(Modifier.height(10.dp))
                BarChartCanvas(focusBars, barColor = IgrisRed)
            }
        }

        // 7-Day Missions Closed
        item {
            IgrisCard {
                EyebrowLabel("7-DAY MISSIONS CLOSED")
                Spacer(Modifier.height(10.dp))
                BarChartCanvas(missionBars, barColor = IgrisGreen)
            }
        }

        // Mock Trend Line Chart
        item {
            IgrisCard {
                EyebrowLabel("MOCK TEST TREND (/720)")
                Spacer(Modifier.height(10.dp))
                LineChartCanvas(
                    points = bundle.tests.map { LineDatum(it.name.take(8), it.score.toFloat()) },
                    maxValue = 720f,
                    targetValue = (bundle.profile?.mockTarget ?: 650).toFloat(),
                    lineColor = IgrisGold
                )
            }
        }

        // Subject-wise Mock %
        if (bundle.tests.isNotEmpty()) {
            item {
                val pAvg = bundle.tests.map { it.physics / 180f }.average().toFloat()
                val cAvg = bundle.tests.map { it.chemistry / 180f }.average().toFloat()
                val bAvg = bundle.tests.map { it.biology / 360f }.average().toFloat()
                IgrisCard {
                    EyebrowLabel("SUBJECT-WISE MOCK ACCURACY %")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalProgressRow("Physics", "${(pAvg * 100).toInt()}%", pAvg, IgrisBlue)
                        HorizontalProgressRow("Chemistry", "${(cAvg * 100).toInt()}%", cAvg, IgrisGold)
                        HorizontalProgressRow("Biology", "${(bAvg * 100).toInt()}%", bAvg, IgrisGreen)
                    }
                }
            }
        }

        // Focus Time by Subject
        item {
            val totalM = maxOf(1, bundle.sessions.sumOf { it.minutes })
            IgrisCard {
                EyebrowLabel("FOCUS TIME BY SUBJECT")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeetSyllabus.subjectsList.forEach { sub ->
                        val mins = bundle.sessions.filter { it.subject == sub }.sumOf { it.minutes }
                        val color = when (sub) {
                            "Physics" -> IgrisBlue
                            "Chemistry" -> IgrisGold
                            else -> IgrisGreen
                        }
                        HorizontalProgressRow(sub, "${mins}m", mins.toFloat() / totalM, color)
                    }
                }
            }
        }

        // Mistakes by Subject & Type
        item {
            val totalErr = maxOf(1, bundle.mistakes.size)
            val byType = bundle.mistakes.groupBy { it.type }
            IgrisCard {
                EyebrowLabel("MISTAKES BY SUBJECT & ERROR TYPE")
                Spacer(Modifier.height(10.dp))
                if (bundle.mistakes.isEmpty()) {
                    Text("No mistakes recorded yet.", color = IgrisMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeetSyllabus.subjectsList.forEach { sub ->
                            val cnt = bundle.mistakes.count { it.subject == sub }
                            HorizontalProgressRow(sub, "$cnt error(s)", cnt.toFloat() / totalErr, IgrisRed)
                        }
                        byType.entries.forEach { (type, list) ->
                            HorizontalProgressRow(type, "${list.size}", list.size.toFloat() / totalErr, IgrisGold)
                        }
                    }
                }
            }
        }
    }
}
