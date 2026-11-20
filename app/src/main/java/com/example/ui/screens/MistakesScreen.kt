package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MistakeEntry
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
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText

@Composable
fun MistakesScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel,
    onOpenFixLoop: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) } // 0 = Open, 1 = Closed
    var subject by remember { mutableStateOf("Physics") }
    var errType by remember { mutableStateOf("Concept gap") }
    var chapter by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var fixText by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedUri = uri
    }

    val openMistakes = remember(bundle.mistakes) { bundle.mistakes.filter { !it.closed } }
    val closedMistakes = remember(bundle.mistakes) { bundle.mistakes.filter { it.closed } }
    val visible = if (tab == 0) openMistakes else closedMistakes
    val chapterGroups = remember(visible) {
        visible.groupBy { it.chapter.ifBlank { "General" } }.entries.toList()
    }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
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
                eyebrow = "ERROR TELEMETRY",
                title = "Mistake Hunter",
                subtitle = "Log mistakes by chapter with optional Photo/PDF snapshots and clear them via the Fix Loop."
            )
        }

        // Add Mistake Form
        item {
            IgrisCard {
                EyebrowLabel("RECORD MISTAKE")
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = NeetSyllabus.subjectsList,
                    selected = subject,
                    onSelect = { subject = it }
                )
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = listOf("Concept gap", "Silly error", "Formula slip", "NCERT trap", "Time management"),
                    selected = errType,
                    onSelect = { errType = it }
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
                    ChipSelectorRow(
                        options = suggestedChapters,
                        selected = chapter,
                        onSelect = { chapter = it }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = errorText,
                    onValueChange = { errorText = it },
                    label = { Text("What went wrong?") },
                    minLines = 2,
                    colors = igrisTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fixText,
                    onValueChange = { fixText = it },
                    label = { Text("Correct concept / Fix rule") },
                    minLines = 2,
                    colors = igrisTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Attach Photo", color = IgrisText, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { pdfPicker.launch(arrayOf("application/pdf", "image/*")) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Attach PDF", color = IgrisText, fontSize = 11.sp)
                    }
                }
                if (pickedUri != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IgrisTag("Attachment ready", tone = "green")
                        TextButton(onClick = { pickedUri = null }) {
                            Text("Remove", color = IgrisMuted, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addMistake(context, subject, errType, chapter, errorText, fixText, pickedUri)
                        errorText = ""
                        fixText = ""
                        pickedUri = null
                    },
                    enabled = chapter.isNotBlank() && errorText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_mistake_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Mistake")
                }
            }
        }

        item {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IgrisRed,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(pos[tab]),
                        color = IgrisRed
                    )
                }
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Open (${openMistakes.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Closed (${closedMistakes.size})") })
            }
        }

        if (chapterGroups.isEmpty()) {
            item {
                IgrisCard {
                    Text(
                        if (tab == 0) "No open mistakes logged." else "No closed mistakes yet.",
                        color = IgrisMuted
                    )
                }
            }
        } else {
            items(chapterGroups, key = { it.key }) { (chapName, itemsList) ->
                val expanded = expandedMap[chapName] ?: true
                IgrisCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMap[chapName] = !expanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chapName, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                            Text("${itemsList.size} entry(s)", style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                        }
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle accordion",
                            tint = IgrisMuted
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        itemsList.forEach { m ->
                            MistakeItemView(
                                m = m,
                                onFixLoop = {
                                    viewModel.focusMistakeInRetest(m.id, openMistakes)
                                    onOpenFixLoop()
                                },
                                onToggleClosed = { viewModel.toggleMistakeClosed(m) },
                                onPreviewAttachment = {
                                    m.localFilePath?.let { p ->
                                        viewModel.openAttachmentPreview(m.fileName ?: m.chapter, p, m.fileType ?: "image/*")
                                    }
                                },
                                onDelete = { viewModel.deleteMistake(m) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeItemView(
    m: MistakeEntry,
    onFixLoop: () -> Unit,
    onToggleClosed: () -> Unit,
    onPreviewAttachment: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IgrisTag(m.subject, tone = subjectTone(m.subject))
            IgrisTag(m.type, tone = "gold")
            if (!m.closed) IgrisTag("${m.fixStreak}/2", tone = "red")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete mistake", tint = IgrisMuted, modifier = Modifier.size(16.dp))
            }
        }
        Text(m.error, style = MaterialTheme.typography.bodyLarge, color = IgrisText)
        if (m.fix.isNotBlank()) {
            Text("Fix: ${m.fix}", style = MaterialTheme.typography.bodyMedium, color = IgrisGreen)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!m.closed) {
                OutlinedButton(
                    onClick = onFixLoop,
                    border = BorderStroke(1.dp, IgrisGold)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fix Loop", color = IgrisGold, fontSize = 11.sp)
                }
            }
            OutlinedButton(
                onClick = onToggleClosed,
                border = BorderStroke(1.dp, IgrisBorderSubtle)
            ) {
                Text(if (m.closed) "Reopen" else "Mark Fixed", color = IgrisText, fontSize = 11.sp)
            }
            if (!m.localFilePath.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onPreviewAttachment,
                    border = BorderStroke(1.dp, IgrisBlue)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View", color = IgrisBlue, fontSize = 11.sp)
                }
            }
        }
    }
}
