package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.SectionHeader
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.components.subjectTone
import com.example.ui.state.AiTutorUiState
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.IgrisBlue
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisText

@Composable
fun VaultScreen(
    bundle: IgrisDataBundle,
    aiState: AiTutorUiState,
    viewModel: IgrisViewModel,
    onQuizGenerated: () -> Unit
) {
    val context = LocalContext.current
    var subjectFilter by remember { mutableStateOf("All") }

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var chapter by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Short Notes PDF") }
    var link by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) fileUri = it }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) fileUri = it }

    val suggestedChapters = remember(subject, bundle.chapters, bundle.quizzes) {
        viewModel.chaptersForSubject(subject, bundle.chapters, bundle.quizzes).take(8)
    }
    val filtered = remember(bundle.vault, subjectFilter) {
        bundle.vault.filter { subjectFilter == "All" || it.subject == subjectFilter }.reversed()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "OFFLINE STUDY LIBRARY + AI QUIZ",
                title = "Study Vault",
                subtitle = "Store short notes, formula sheets, PDFs & Drive links, then tap Quiz for instant AI MCQs."
            )
        }

        // Upload Form
        item {
            IgrisCard {
                EyebrowLabel("ADD TO VAULT")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Electrostatics Formula Sheet)") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = listOf("Physics", "Chemistry", "Biology", "Formula / Notes"),
                    selected = subject,
                    onSelect = { subject = it }
                )
                Spacer(Modifier.height(8.dp))
                ChipSelectorRow(
                    options = listOf("Short Notes PDF", "Formula Sheet", "Diagram", "PYQ Pack", "Drive Link"),
                    selected = category,
                    onSelect = { category = it }
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
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Optional Google Drive / Web URL") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Image", color = IgrisText, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { pdfPicker.launch(arrayOf("application/pdf", "image/*")) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick PDF", color = IgrisText, fontSize = 11.sp)
                    }
                }
                if (fileUri != null) {
                    Spacer(Modifier.height(6.dp))
                    IgrisTag("Offline file attached", tone = "green")
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addVaultItem(context, title, subject, chapter, category, link, fileUri)
                        title = ""; link = ""; fileUri = null
                    },
                    enabled = title.isNotBlank() && (fileUri != null || link.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_vault_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save to Vault")
                }
            }
        }

        // Filter
        item {
            ChipSelectorRow(
                options = listOf("All", "Physics", "Chemistry", "Biology", "Formula / Notes"),
                selected = subjectFilter,
                onSelect = { subjectFilter = it }
            )
        }

        if (aiState.loading) {
            item {
                IgrisCard(borderColor = IgrisRed) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IgrisRed, strokeWidth = 2.dp)
                        Text("Generating 12 NCERT MCQs from Study Vault...", color = IgrisText)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                IgrisCard {
                    Text("No study materials saved for this filter yet.", color = IgrisMuted)
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                IgrisCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                            Text(
                                "${item.date} • ${item.category}${if (item.chapter.isNotBlank()) " • ${item.chapter}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = IgrisMuted
                            )
                        }
                        IgrisTag(item.subject, tone = subjectTone(item.subject))
                        IconButton(onClick = { viewModel.deleteVaultItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete vault entry", tint = IgrisMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.localFilePath.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.openAttachmentPreview(item.fileName ?: item.title, item.localFilePath, item.fileType ?: "*/*")
                                },
                                border = BorderStroke(1.dp, IgrisBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open File", color = IgrisBlue, fontSize = 12.sp)
                            }
                        } else if (!item.link.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                                    }
                                },
                                border = BorderStroke(1.dp, IgrisBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open Link", color = IgrisBlue, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.generateQuizFromVault(item, onQuizGenerated) },
                            enabled = !aiState.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quiz (12 Q)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
