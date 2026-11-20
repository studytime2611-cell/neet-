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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NeetSyllabus
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.SectionHeader
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.components.subjectTone
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText

@Composable
fun RevisionScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel
) {
    var subject by remember { mutableStateOf("Biology") }
    var chapter by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("NCERT") }

    val dueRevs = remember(bundle.revisions) { viewModel.dueRevisions(bundle.revisions) }
    val hqRevHistory = remember(bundle.chapters) {
        bundle.chapters.flatMap { ch ->
            listOf("r1", "r2", "r3", "r4", "r5").mapNotNull { key ->
                val dt = ch.ticks[key]
                if (dt.isNullOrBlank()) null else Triple(dt, ch.subject, "${ch.name} (${key.uppercase()})")
            }
        }.sortedByDescending { it.first }
    }
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
                eyebrow = "SPACED REPETITION (+3D / +7D / +21D)",
                title = "Revision Log",
                subtitle = "Automatic spaced repetition queue synced with Subject HQ Rev 1–5 ticks."
            )
        }

        // Due Queue
        item {
            IgrisCard(borderColor = if (dueRevs.isNotEmpty()) IgrisGold else MaterialTheme.colorScheme.outline) {
                EyebrowLabel("DUE FOR REVISION (${dueRevs.size})", color = IgrisGold)
                Spacer(Modifier.height(8.dp))
                if (dueRevs.isEmpty()) {
                    Text("No chapters overdue for spaced revision today!", style = MaterialTheme.typography.bodyMedium, color = IgrisGreen)
                } else {
                    dueRevs.forEach { rev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rev.chapter, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                                Text(
                                    "${rev.subject} • ${rev.kind} • +3d ${rev.r3} | +7d ${rev.r7} | +21d ${rev.r21}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IgrisMuted
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.markRevisionDone(rev) },
                                colors = ButtonDefaults.buttonColors(containerColor = IgrisGreen),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                Spacer(Modifier.width(4.dp))
                                Text("Revised", color = MaterialTheme.colorScheme.onSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Log Revision Form
        item {
            IgrisCard {
                EyebrowLabel("LOG REVISION SESSION")
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = NeetSyllabus.subjectsList,
                    selected = subject,
                    onSelect = { subject = it }
                )
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = listOf("NCERT", "Short Notes", "Formula Sheet", "PYQs", "Mistakes"),
                    selected = kind,
                    onSelect = { kind = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter revised") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestedChapters.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ChipSelectorRow(
                        options = suggestedChapters,
                        selected = chapter,
                        onSelect = { chapter = it }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addRevision(chapter, subject, kind)
                        chapter = ""
                    },
                    enabled = chapter.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("log_revision_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Log Revision (+3d/+7d/+21d)")
                }
            }
        }

        // Full History
        item {
            EyebrowLabel("REVISION HISTORY (${bundle.revisions.size} LOGGED • ${hqRevHistory.size} HQ TICKS)")
        }
        items(bundle.revisions.reversed(), key = { it.id }) { r ->
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.chapter, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                        Text("${r.date} • ${r.kind}", style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                    }
                    IgrisTag(r.subject, tone = subjectTone(r.subject))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IgrisTag("+3d ${r.r3}", tone = if (!r.milestoneR3.isNullOrBlank()) "green" else "neutral")
                    IgrisTag("+7d ${r.r7}", tone = if (!r.milestoneR7.isNullOrBlank()) "green" else "neutral")
                    IgrisTag("+21d ${r.r21}", tone = if (!r.milestoneR21.isNullOrBlank()) "green" else "neutral")
                }
            }
        }
        items(hqRevHistory.take(15)) { (dt, sub, label) ->
            IgrisCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = IgrisText, modifier = Modifier.weight(1f))
                    Text(dt, fontFamily = DmMonoFontFamily, fontSize = 11.sp, color = IgrisMuted)
                    Spacer(Modifier.width(6.dp))
                    IgrisTag(sub, tone = subjectTone(sub))
                }
            }
        }
    }
}
