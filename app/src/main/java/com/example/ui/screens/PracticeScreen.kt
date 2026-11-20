package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MistakeEntry
import com.example.data.model.NeetSyllabus
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.components.subjectTone
import com.example.ui.state.ActiveQuizState
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.state.QuizProgressSnapshot
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisGreenBorder
import com.example.ui.theme.IgrisGreenSurface
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPanelAlt
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedDark
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisText
import com.example.util.DateUtils

private const val SAMPLE_MCQ_JSON = """[
  {
    "subject": "Biology",
    "chapter": "Cell Cycle and Cell Division",
    "q": "During which stage of meiosis I does crossing over occur?",
    "opts": ["Leptotene", "Zygotene", "Pachytene", "Diplotene"],
    "ans": 2,
    "mark": "Imp",
    "exp": "Crossing over takes place between non-sister chromatids of homologous chromosomes in Pachytene (recombinase enzyme)."
  },
  {
    "subject": "Physics",
    "chapter": "Kinematics",
    "q": "A particle is projected at 45 degrees with speed u. At the highest point, its speed is:",
    "opts": ["u", "u/2", "u/sqrt(2)", "0"],
    "ans": 2,
    "mark": "Easy",
    "exp": "At the highest point vertical velocity is zero; horizontal component u cos(45°) = u/sqrt(2) remains."
  },
  {
    "subject": "Chemistry",
    "chapter": "Chemical Bonding and Molecular Structure",
    "q": "Which of the following molecules has zero dipole moment?",
    "opts": ["NH3", "NF3", "BF3", "H2O"],
    "ans": 2,
    "mark": "Medium",
    "exp": "BF3 is trigonal planar and symmetrical, so individual bond dipoles cancel to give zero net dipole moment."
  }
]"""

@Composable
fun PracticeScreen(
    bundle: IgrisDataBundle,
    quizState: ActiveQuizState,
    savedProgress: Map<String, QuizProgressSnapshot>,
    retestIndex: Int,
    viewModel: IgrisViewModel,
    initialTab: Int = 0
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    if (quizState.active) {
        ActiveQuizRunner(
            quizState = quizState,
            existingMistakes = bundle.mistakes,
            viewModel = viewModel
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "ACTIVE RECALL ARENA",
                title = "Practice & Quiz",
                subtitle = "Bulk JSON MCQ bank, Saved Attempts, 2-clear Mistake Fix Loop & +4/-1/0 Skip Drill."
            )
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IgrisRed,
                edgePadding = 4.dp,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = IgrisRed
                    )
                }
            ) {
                listOf("MCQ Quiz", "Saved Attempts", "Mistake Re-Test", "Skip Drill").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) },
                        modifier = Modifier.testTag("practice_tab_$idx")
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> item {
                McqQuizBankSection(bundle = bundle, savedProgress = savedProgress, viewModel = viewModel)
            }
            1 -> item {
                SavedAttemptsSection(bundle = bundle, viewModel = viewModel)
            }
            2 -> item {
                MistakeRetestSection(
                    mistakes = bundle.mistakes,
                    retestIndex = retestIndex,
                    viewModel = viewModel
                )
            }
            3 -> item {
                SkipDrillSection(bank = bundle.quizzes, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun McqQuizBankSection(
    bundle: IgrisDataBundle,
    savedProgress: Map<String, QuizProgressSnapshot>,
    viewModel: IgrisViewModel
) {
    var selectedSub by remember { mutableStateOf("All") }
    var selectedChap by remember { mutableStateOf("All") }
    var selectedMark by remember { mutableStateOf("All") }
    var jsonInput by remember { mutableStateOf("") }

    val availableChapters = remember(bundle.quizzes, selectedSub) {
        listOf("All") + bundle.quizzes
            .filter { selectedSub == "All" || it.subject == selectedSub }
            .map { it.chapter }
            .distinct()
    }
    if (selectedChap !in availableChapters) selectedChap = "All"

    val matchingCount = remember(bundle.quizzes, selectedSub, selectedChap, selectedMark) {
        val normMark = if (selectedMark == "All") "All" else NeetSyllabus.normalizeMark(selectedMark)
        bundle.quizzes.count { q ->
            (selectedSub == "All" || q.subject.equals(selectedSub, true)) &&
                (selectedChap == "All" || q.chapter.equals(selectedChap, true)) &&
                (normMark == "All" || NeetSyllabus.normalizeMark(q.mark) == normMark)
        }
    }

    val key = listOf(selectedSub, selectedChap, selectedMark).joinToString("|") { it.ifBlank { "All" }.trim().lowercase() }
    val hasResume = savedProgress[key]?.let { it.idx < it.questions.size } == true

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IgrisCard {
            EyebrowLabel("QUESTION BANK (${bundle.quizzes.size} SAVED MCQS)")
            Spacer(Modifier.height(10.dp))
            EyebrowLabel("SUBJECT")
            Spacer(Modifier.height(6.dp))
            ChipSelectorRow(
                options = listOf("All", "Physics", "Chemistry", "Biology"),
                selected = selectedSub,
                onSelect = { selectedSub = it }
            )
            Spacer(Modifier.height(10.dp))
            EyebrowLabel("TAG FILTER")
            Spacer(Modifier.height(6.dp))
            ChipSelectorRow(
                options = listOf("All", "Imp", "Hard", "Medium", "Easy"),
                selected = selectedMark,
                onSelect = { selectedMark = it }
            )
            if (availableChapters.size > 1) {
                Spacer(Modifier.height(10.dp))
                EyebrowLabel("CHAPTER")
                Spacer(Modifier.height(6.dp))
                ChipSelectorRow(
                    options = availableChapters.take(12),
                    selected = selectedChap,
                    onSelect = { selectedChap = it }
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.startSavedQuiz(
                            bank = bundle.quizzes,
                            subjectFilter = selectedSub,
                            chapterFilter = selectedChap,
                            markFilter = selectedMark,
                            forceRestart = false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_quiz_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasResume) "Resume ($matchingCount)" else "Start Quiz ($matchingCount)")
                }
                if (hasResume) {
                    OutlinedButton(
                        onClick = {
                            viewModel.startSavedQuiz(
                                bank = bundle.quizzes,
                                subjectFilter = selectedSub,
                                chapterFilter = selectedChap,
                                markFilter = selectedMark,
                                forceRestart = true
                            )
                        },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart quiz", tint = IgrisText)
                    }
                }
            }
        }

        // Bulk JSON Importer
        IgrisCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    EyebrowLabel("BULK JSON IMPORTER")
                    Text("Import MCQs & Launch", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                }
                Row {
                    TextButton(onClick = { jsonInput = SAMPLE_MCQ_JSON }) {
                        Text("Sample JSON", color = IgrisGold, fontSize = 11.sp)
                    }
                    if (bundle.quizzes.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSavedQuizzes() }) {
                            Text("Clear Bank", color = IgrisMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = jsonInput,
                onValueChange = { jsonInput = it },
                label = { Text("Paste JSON array [{\"subject\",\"chapter\",\"q\",\"opts\":[...],\"ans\":0,\"mark\",\"exp\"}]") },
                minLines = 4,
                maxLines = 7,
                colors = igrisTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_json_input")
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (jsonInput.isNotBlank()) {
                        viewModel.importBulkJsonAndStart(jsonInput)
                        jsonInput = ""
                    }
                },
                enabled = jsonInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("import_json_quiz_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Import JSON & Start Quiz")
            }
        }
    }
}

@Composable
private fun ActiveQuizRunner(
    quizState: ActiveQuizState,
    existingMistakes: List<MistakeEntry>,
    viewModel: IgrisViewModel
) {
    androidx.activity.compose.BackHandler {
        viewModel.exitActiveQuiz()
    }

    val total = quizState.questions.size
    val isFinished = quizState.idx >= total

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EyebrowLabel("${quizState.subject} • ${quizState.chapter}", color = IgrisRed)
                    val secPerQ = if (quizState.idx > 0) quizState.elapsedSec / maxOf(1, quizState.idx) else quizState.elapsedSec
                    Text(
                        text = "${DateUtils.formatMMSS(quizState.elapsedSec)} • ${secPerQ}s/Q",
                        fontFamily = DmMonoFontFamily,
                        fontSize = 13.sp,
                        color = IgrisGold
                    )
                }
                IconButton(onClick = { viewModel.restartActiveQuiz() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart quiz", tint = IgrisText)
                }
                IconButton(onClick = { viewModel.exitActiveQuiz() }) {
                    Icon(Icons.Default.Close, contentDescription = "Exit quiz", tint = IgrisMuted)
                }
            }
        }

        if (isFinished) {
            item {
                val acc = if (total > 0) (quizState.score * 100 / total) else 0
                IgrisCard {
                    EyebrowLabel("QUIZ COMPLETE")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (quizState.skipDrill) "Net Score: ${quizState.drillNet} (Forced Guess: ${quizState.drillForced})"
                        else "Score: ${quizState.score}/$total ($acc%)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = IgrisGreen
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Time: ${DateUtils.formatMMSS(quizState.elapsedSec)} • Wrong answers were auto-logged to Mistake Hunter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.restartActiveQuiz() },
                            colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                            modifier = Modifier.weight(1f)
                        ) { Text("Re-Attempt") }
                        OutlinedButton(
                            onClick = { viewModel.exitActiveQuiz() },
                            border = BorderStroke(1.dp, IgrisBorderSubtle),
                            modifier = Modifier.weight(1f)
                        ) { Text("Done", color = IgrisText) }
                    }
                }
            }
            if (quizState.wrongList.isNotEmpty()) {
                item { EyebrowLabel("MISSED QUESTIONS (${quizState.wrongList.size})") }
                items(quizState.wrongList) { w ->
                    IgrisCard {
                        Text(w.q, style = MaterialTheme.typography.bodyLarge, color = IgrisText)
                        Spacer(Modifier.height(6.dp))
                        Text("Your pick: ${w.picked}", style = MaterialTheme.typography.bodySmall, color = IgrisRed)
                        Text("True Answer: ${w.correct}", style = MaterialTheme.typography.bodySmall, color = IgrisGreen)
                        if (w.exp.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(w.exp, style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                        }
                    }
                }
            }
        } else {
            val q = quizState.questions[quizState.idx]
            item {
                IgrisCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Q${quizState.idx + 1} / $total",
                            fontFamily = DmMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = IgrisText
                        )
                        if (quizState.skipDrill) {
                            IgrisTag("Net: ${quizState.drillNet} | Forced: ${quizState.drillForced}", tone = "gold")
                        } else {
                            IgrisTag("Score: ${quizState.score}/${quizState.idx + if (quizState.answered) 1 else 0}", tone = "green")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Interactive tag switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NeetSyllabus.quizMarks.forEach { mk ->
                            val active = q.mark == mk
                            Surface(
                                modifier = Modifier.clickable { viewModel.setCurrentQuestionMark(mk) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (active) IgrisSoftRed else IgrisPanelAlt,
                                border = BorderStroke(1.dp, if (active) IgrisRed else IgrisBorderSubtle)
                            ) {
                                Text(
                                    mk,
                                    fontFamily = DmMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (active) Color.White else IgrisMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(q.q, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                    Spacer(Modifier.height(12.dp))

                    q.opts.forEachIndexed { optIdx, optText ->
                        val isPicked = quizState.pickedIdx == optIdx
                        val isTrue = q.ans == optIdx
                        val (bg, border, textColor) = when {
                            !quizState.answered -> Triple(IgrisPanelAlt, IgrisBorderSubtle, IgrisText)
                            isTrue -> Triple(IgrisGreenSurface, IgrisGreenBorder, IgrisGreen)
                            isPicked -> Triple(IgrisSoftRed, IgrisRed, Color(0xFFFCA5A5))
                            else -> Triple(IgrisPanelAlt, IgrisBorderSubtle, IgrisMuted)
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !quizState.answered) {
                                    viewModel.answerQuizOption(optIdx, existingMistakes)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = BorderStroke(1.dp, border)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${('A' + optIdx)}.",
                                    fontFamily = DmMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.width(26.dp)
                                )
                                Text(optText, style = MaterialTheme.typography.bodyLarge, color = textColor)
                            }
                        }
                    }

                    if (quizState.skipDrill && !quizState.answered) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.skipQuizQuestion() },
                            border = BorderStroke(1.dp, IgrisGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = IgrisGold)
                            Spacer(Modifier.width(6.dp))
                            Text("Skip Question (0 marks, avoids -1)", color = IgrisGold)
                        }
                    }

                    if (quizState.answered) {
                        Spacer(Modifier.height(10.dp))
                        val trueAns = q.opts.getOrElse(q.ans) { "" }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = IgrisPanelAlt,
                            border = BorderStroke(1.dp, IgrisBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (quizState.pickedIdx == q.ans) "Correct! True Answer: $trueAns"
                                    else if (quizState.pickedIdx == -1) "Skipped. True Answer: $trueAns"
                                    else "Wrong! True Answer: $trueAns",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (quizState.pickedIdx == q.ans) IgrisGreen else IgrisGold
                                )
                                if (q.exp.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(q.exp, style = MaterialTheme.typography.bodyMedium, color = IgrisMuted)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.nextQuizQuestion() },
                            colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quiz_next_button")
                        ) {
                            Text(if (quizState.idx + 1 >= total) "Finish Quiz" else "Next Question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAttemptsSection(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel
) {
    var chapFilter by remember { mutableStateOf("All") }
    val chapters = remember(bundle.quizAttempts) {
        listOf("All") + bundle.quizAttempts.map { it.chapter }.distinct()
    }
    val filtered = remember(bundle.quizAttempts, chapFilter) {
        bundle.quizAttempts.filter { chapFilter == "All" || it.chapter == chapFilter }.reversed()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (chapters.size > 1) {
            ChipSelectorRow(options = chapters.take(10), selected = chapFilter, onSelect = { chapFilter = it })
        }

        // Chapter accuracy summary
        val byChap = bundle.quizAttempts.groupBy { it.chapter }
        if (byChap.isNotEmpty()) {
            IgrisCard {
                EyebrowLabel("CHAPTER ACCURACY SUMMARY")
                Spacer(Modifier.height(8.dp))
                byChap.entries.take(8).forEach { (ch, list) ->
                    val avgAcc = list.map { it.accuracy }.average().toInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ch, style = MaterialTheme.typography.bodyMedium, color = IgrisText, modifier = Modifier.weight(1f))
                        Text(
                            "${list.size} run(s) • $avgAcc%",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 11.sp,
                            color = if (avgAcc >= 75) IgrisGreen else IgrisGold
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            IgrisCard {
                Text("No quiz attempts saved yet. Complete any quiz to track accuracy & missed questions.", color = IgrisMuted)
            }
        } else {
            filtered.forEach { attempt ->
                SavedAttemptCard(attempt = attempt, bank = bundle.quizzes, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun SavedAttemptCard(
    attempt: QuizAttempt,
    bank: List<QuizQuestion>,
    viewModel: IgrisViewModel
) {
    val wrong = remember(attempt.wrongItemsJson) { viewModel.parseWrongItems(attempt.wrongItemsJson) }
    IgrisCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(attempt.chapter, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                Text("${attempt.date} • ${attempt.subject}", style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
            }
            IgrisTag("${attempt.score}/${attempt.total} (${attempt.accuracy}%)", tone = if (attempt.accuracy >= 75) "green" else "red")
            IconButton(onClick = { viewModel.deleteAttempt(attempt.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete attempt", tint = IgrisMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IgrisTag("${attempt.avgSec}s/Q", tone = "blue")
            IgrisTag("${wrong.size} wrong", tone = if (wrong.isEmpty()) "green" else "warn")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (wrong.isNotEmpty()) {
                Button(
                    onClick = { viewModel.retryAttemptWrongQuestions(attempt, bank) },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry Wrong Qs (${wrong.size})", fontSize = 12.sp)
                }
            }
            OutlinedButton(
                onClick = { viewModel.startSavedQuiz(bank, attempt.subject, attempt.chapter, "All", forceRestart = true) },
                border = BorderStroke(1.dp, IgrisBorderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Text("Re-Attempt", color = IgrisText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MistakeRetestSection(
    mistakes: List<MistakeEntry>,
    retestIndex: Int,
    viewModel: IgrisViewModel
) {
    val open = remember(mistakes) { mistakes.filter { !it.closed } }
    if (open.isEmpty()) {
        IgrisCard {
            EyebrowLabel("FIX LOOP CLEAR")
            Spacer(Modifier.height(4.dp))
            Text("No open mistakes in the Fix Loop queue!", style = MaterialTheme.typography.titleMedium, color = IgrisGreen)
        }
        return
    }
    val cur = open[retestIndex % open.size]
    IgrisCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EyebrowLabel("MISTAKE FIX LOOP (${open.size} OPEN)")
            IgrisTag("Streak ${cur.fixStreak}/2", tone = if (cur.fixStreak == 1) "gold" else "red")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IgrisTag(cur.subject, tone = subjectTone(cur.subject))
            IgrisTag(cur.chapter, tone = "neutral")
        }
        Spacer(Modifier.height(10.dp))
        Text(cur.error, style = MaterialTheme.typography.titleMedium, color = IgrisText)
        if (cur.fix.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IgrisPanelAlt,
                border = BorderStroke(1.dp, IgrisBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Fix rule: ${cur.fix}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IgrisGreen,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.recordRetestResult(cur, passed = true) },
                colors = ButtonDefaults.buttonColors(containerColor = IgrisGreen),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Solved (${cur.fixStreak + 1}/2)", color = Color(0xFF071710), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.recordRetestResult(cur, passed = false) },
                border = BorderStroke(1.dp, IgrisRed),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Still Tricky", color = IgrisRed)
            }
        }
    }
}

@Composable
private fun SkipDrillSection(
    bank: List<QuizQuestion>,
    viewModel: IgrisViewModel
) {
    IgrisCard {
        EyebrowLabel("+4 / -1 / 0 SKIP DRILL")
        Spacer(Modifier.height(4.dp))
        Text("Eliminate Negative Marking", style = MaterialTheme.typography.titleMedium, color = IgrisText)
        Spacer(Modifier.height(6.dp))
        Text(
            "Compare your disciplined NEET Net Score (+4 right, -1 wrong, 0 skip) against a Forced-Guess baseline where skipped questions cost -1.",
            style = MaterialTheme.typography.bodyMedium,
            color = IgrisMuted
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 15, 20).forEach { count ->
                Button(
                    onClick = { viewModel.startSkipDrill(bank, count) },
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${count} Qs")
                }
            }
        }
    }
}
