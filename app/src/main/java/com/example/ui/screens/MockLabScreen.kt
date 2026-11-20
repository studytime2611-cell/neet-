package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.HorizontalProgressRow
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.LineDatum
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils

@Composable
fun MockLabScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel
) {
    val papers = bundle.mockLabs
    val target = bundle.profile?.mockTarget ?: 650
    var targetInput by remember(target) { mutableStateOf(target.toString()) }

    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var pC by remember { mutableStateOf("") }
    var pW by remember { mutableStateOf("") }
    var cC by remember { mutableStateOf("") }
    var cW by remember { mutableStateOf("") }
    var bC by remember { mutableStateOf("") }
    var bW by remember { mutableStateOf("") }
    var zC by remember { mutableStateOf("") }
    var zW by remember { mutableStateOf("") }

    val latest = papers.lastOrNull()
    val latestScore = latest?.totalScore() ?: 0
    val latestAcc = latest?.let { (it.accuracyFraction() * 100).toInt() } ?: 0

    // Linear regression slope projection
    val slopeProjection = remember(papers) {
        if (papers.size < 2) null else {
            val n = papers.size.toDouble()
            val xs = papers.indices.map { it.toDouble() }
            val ys = papers.map { it.totalScore().toDouble() }
            val mx = xs.average()
            val my = ys.average()
            val num = xs.zip(ys).sumOf { (x, y) -> (x - mx) * (y - my) }
            val den = xs.sumOf { (it - mx) * (it - mx) }
            val slope = if (den > 0) num / den else 0.0
            (ys.last() + slope * 3).toInt().coerceIn(0, 720)
        }
    }

    val maxConvert = latest?.totalWrong() ?: 0
    var convertCount by remember(maxConvert) { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "4-SUBJECT ANALYZER (45Q EACH • +4/-1)",
                title = "Mock Lab",
                subtitle = "Physics, Chemistry, Botany & Zoology breakdown, slope projection & wrong-to-right recovery simulator."
            )
        }

        // KPI Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("LATEST LAB", "$latestScore/720", "${latestAcc}% accuracy", valueColor = IgrisRed, modifier = Modifier.weight(1f))
                    StatCard(
                        "IF SLOPE HOLDS",
                        slopeProjection?.let { "$it/720" } ?: "--",
                        "3-test linear projection",
                        valueColor = IgrisGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    val gap = (target - latestScore).coerceAtLeast(0)
                    StatCard("TARGET GAP", "${if (gap == 0 && latest != null) "HIT!" else "-$gap"}", "Target: $target/720", valueColor = IgrisGold, modifier = Modifier.weight(1f))
                    val band = when {
                        latestScore >= 670 -> "AIR < 1.5k (AIIMS Band)"
                        latestScore >= 640 -> "AIR 1.5k–6k (Top Govt)"
                        latestScore >= 600 -> "AIR 6k–18k (Safe Govt)"
                        latestScore >= 520 -> "Borderline • Push Accuracy"
                        else -> "Foundation & NCERT Focus"
                    }
                    StatCard("UNOFFICIAL BAND", if (latest == null) "--" else band.substringBefore(" ("), band, valueColor = IgrisBlue, modifier = Modifier.weight(1f))
                }
            }
        }

        // Target Setter + Score Trend Line Chart
        item {
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Score /720") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { targetInput.toIntOrNull()?.let { viewModel.updateMockTarget(it) } },
                        border = BorderStroke(1.dp, IgrisGold),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Set Target", color = IgrisGold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                EyebrowLabel("SCORE TREND")
                Spacer(Modifier.height(8.dp))
                LineChartCanvas(
                    points = papers.map { LineDatum(it.name.take(8), it.totalScore().toFloat()) },
                    maxValue = 720f,
                    targetValue = target.toFloat(),
                    lineColor = IgrisRed
                )
            }
        }

        // Subject Split & If Wrongs Become Rights Simulator
        if (latest != null) {
            item {
                IgrisCard {
                    EyebrowLabel("LATEST PAPER SPLIT (${latest.name})")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalProgressRow("Physics (R:${latest.physicsC} W:${latest.physicsW})", "${latest.scorePhysics()}/180", latest.scorePhysics() / 180f, IgrisBlue)
                        HorizontalProgressRow("Chemistry (R:${latest.chemistryC} W:${latest.chemistryW})", "${latest.scoreChemistry()}/180", latest.scoreChemistry() / 180f, IgrisGold)
                        HorizontalProgressRow("Botany (R:${latest.botanyC} W:${latest.botanyW})", "${latest.scoreBotany()}/180", latest.scoreBotany() / 180f, IgrisGreen)
                        HorizontalProgressRow("Zoology (R:${latest.zoologyC} W:${latest.zoologyW})", "${latest.scoreZoology()}/180", latest.scoreZoology() / 180f, IgrisRed)
                    }
                    Spacer(Modifier.height(14.dp))
                    EyebrowLabel("IF WRONGS BECOME RIGHTS (+5 MARKS EACH)")
                    Spacer(Modifier.height(4.dp))
                    val c = convertCount.toInt()
                    val simScore = (latestScore + c * 5).coerceAtMost(720)
                    Text(
                        "Converting $c of $maxConvert wrong Qs -> $simScore/720 (+${c * 5} marks)",
                        style = MaterialTheme.typography.titleMedium,
                        color = IgrisGreen
                    )
                    if (maxConvert > 0) {
                        Slider(
                            value = convertCount,
                            onValueChange = { convertCount = it },
                            valueRange = 0f..maxConvert.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = IgrisRed, activeTrackColor = IgrisRed)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    EyebrowLabel("RANK GAP PLAN (THIS WEEK)")
                    val weakest = listOf(
                        "Physics" to latest.scorePhysics(),
                        "Chemistry" to latest.scoreChemistry(),
                        "Botany" to latest.scoreBotany(),
                        "Zoology" to latest.scoreZoology()
                    ).minBy { it.second }
                    Text(
                        "Priority leak: ${weakest.first} (${weakest.second}/180). Cut ${latest.totalWrong()} negative marks via Mistake Fix Loop & Skip Drill.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                }
            }
        }

        // Log Paper Form
        item {
            IgrisCard {
                EyebrowLabel("LOG 4-SUBJECT MOCK PAPER (45Q PER SUBJECT)")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Paper Name") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(0.75f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                SubjectRwRow("Physics", pC, { pC = it }, pW, { pW = it })
                Spacer(Modifier.height(6.dp))
                SubjectRwRow("Chemistry", cC, { cC = it }, cW, { cW = it })
                Spacer(Modifier.height(6.dp))
                SubjectRwRow("Botany", bC, { bC = it }, bW, { bW = it })
                Spacer(Modifier.height(6.dp))
                SubjectRwRow("Zoology", zC, { zC = it }, zW, { zW = it })
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addMockLabPaper(
                            name, date,
                            pC.toIntOrNull() ?: 0, pW.toIntOrNull() ?: 0,
                            cC.toIntOrNull() ?: 0, cW.toIntOrNull() ?: 0,
                            bC.toIntOrNull() ?: 0, bW.toIntOrNull() ?: 0,
                            zC.toIntOrNull() ?: 0, zW.toIntOrNull() ?: 0
                        )
                        name = ""; pC = ""; pW = ""; cC = ""; cW = ""; bC = ""; bW = ""; zC = ""; zW = ""
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_mock_lab_button")
                ) {
                    Text("Save Mock Lab Paper")
                }
            }
        }

        // Papers History
        item { EyebrowLabel("PAPERS (${papers.size})") }
        items(papers.reversed(), key = { it.id }) { p ->
            IgrisCard {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                        Text(
                            "${p.date} • R:${p.totalCorrect()} W:${p.totalWrong()} Skip:${p.totalSkipped()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IgrisMuted
                        )
                    }
                    IgrisTag("${p.totalScore()}/720", tone = if (p.totalScore() >= target) "green" else "gold")
                    IconButton(onClick = { viewModel.deleteMockLabPaper(p.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete paper", tint = IgrisMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectRwRow(
    label: String,
    rightVal: String,
    onRightChange: (String) -> Unit,
    wrongVal: String,
    onWrongChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = IgrisText, modifier = Modifier.weight(0.9f))
        OutlinedTextField(
            value = rightVal,
            onValueChange = { onRightChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Right") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = igrisTextFieldColors(),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = wrongVal,
            onValueChange = { onWrongChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Wrong") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = igrisTextFieldColors(),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
