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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.NeetSyllabus
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.state.AiTutorUiState
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText

@Composable
fun AiTutorScreen(
    bundle: IgrisDataBundle,
    aiState: AiTutorUiState,
    viewModel: IgrisViewModel,
    onQuizStarted: () -> Unit
) {
    var subject by remember { mutableStateOf("Biology") }
    var count by remember { mutableIntStateOf(10) }
    var chapter by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }

    val suggestedChapters = remember(subject, bundle.chapters, bundle.quizzes) {
        viewModel.chaptersForSubject(subject, bundle.chapters, bundle.quizzes).take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "FIREBASE AI LOGIC • GEMINI",
                title = "AI Tutor",
                subtitle = "Generate structured NCERT MCQs, stream crisp doubt explanations, or target your open mistakes."
            )
        }

        item {
            IgrisCard {
                EyebrowLabel("TUTOR PARAMETERS")
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = NeetSyllabus.subjectsList,
                    selected = subject,
                    onSelect = { subject = it }
                )
                Spacer(Modifier.height(8.dp))
                EyebrowLabel("QUESTION COUNT")
                Spacer(Modifier.height(6.dp))
                ChipSelectorRow(
                    options = listOf("5", "10", "15", "20"),
                    selected = count.toString(),
                    onSelect = { count = it.toIntOrNull() ?: 10 }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestedChapters.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ChipSelectorRow(options = suggestedChapters, selected = chapter, onSelect = { chapter = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Specific concept / doubt / NCERT line") },
                    minLines = 2,
                    colors = igrisTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("ai_topic_input")
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.generateAiMcqs(
                                subject = subject,
                                chapter = chapter,
                                topic = topic,
                                count = count,
                                mistakes = bundle.mistakes,
                                mistakeMode = false
                            )
                        },
                        enabled = !aiState.loading,
                        colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_generate_mcq_button")
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Generate $count MCQ Quiz")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.streamAiDoubt(subject, chapter, topic) },
                            enabled = !aiState.loading && (topic.isNotBlank() || chapter.isNotBlank()),
                            border = BorderStroke(1.dp, IgrisBlue),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("ai_explain_doubt_button")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Explain Doubt", color = IgrisBlue)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.generateAiMcqs(
                                    subject = subject,
                                    chapter = chapter,
                                    topic = topic,
                                    count = count,
                                    mistakes = bundle.mistakes,
                                    mistakeMode = true
                                )
                            },
                            enabled = !aiState.loading,
                            border = BorderStroke(1.dp, IgrisGold),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("ai_mistake_quiz_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mistake Quiz", color = IgrisGold)
                        }
                    }
                }
            }
        }

        if (aiState.loading) {
            item {
                IgrisCard(borderColor = IgrisRed) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IgrisRed, strokeWidth = 2.dp)
                        Text("IGRIS AI Tutor is thinking...", color = IgrisText)
                    }
                }
            }
        }

        if (!aiState.error.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        aiState.error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (aiState.streamingText.isNotBlank()) {
            item {
                IgrisCard {
                    EyebrowLabel("AI OUTPUT")
                    Spacer(Modifier.height(6.dp))
                    Text(aiState.streamingText, style = MaterialTheme.typography.bodyLarge, color = IgrisText)
                }
            }
        }

        if (aiState.generatedQuestions.isNotEmpty()) {
            item {
                IgrisCard(borderColor = IgrisGreen) {
                    EyebrowLabel("READY TO LAUNCH", color = IgrisGreen)
                    Spacer(Modifier.height(4.dp))
                    Text("${aiState.generatedQuestions.size} structured NEET MCQs ready.", style = MaterialTheme.typography.titleMedium, color = IgrisText)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.importAiGeneratedQuestionsAndStart(onQuizStarted) },
                        colors = ButtonDefaults.buttonColors(containerColor = IgrisGreen),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_save_start_quiz_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text("Save & Start Quiz", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }
}
