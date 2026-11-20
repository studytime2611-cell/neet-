package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
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
fun TestsScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel
) {
    val context = LocalContext.current

    // Upcoming form state
    var upName by remember { mutableStateOf("") }
    var upDate by remember { mutableStateOf(DateUtils.addDays(DateUtils.today(), 7)) }
    var upTime by remember { mutableStateOf("14:00") }
    var upSyllabus by remember { mutableStateOf("") }
    var upUri by remember { mutableStateOf<Uri?>(null) }

    // Mock record form state
    var mockName by remember { mutableStateOf("") }
    var phys by remember { mutableStateOf("") }
    var chem by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var acc by remember { mutableStateOf("") }
    var analysisDone by remember { mutableStateOf(false) }
    var mockUri by remember { mutableStateOf<Uri?>(null) }

    val upImgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) upUri = it }
    val upPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) upUri = it }
    val mockImgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) mockUri = it }
    val mockPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) mockUri = it }

    val pInt = phys.toIntOrNull() ?: 0
    val cInt = chem.toIntOrNull() ?: 0
    val bInt = bio.toIntOrNull() ?: 0
    val autoTotal = (pInt + cInt + bInt).coerceIn(0, 720)

    val latest = bundle.tests.lastOrNull()
    val best = bundle.tests.maxOfOrNull { it.score } ?: 0
    val avg = if (bundle.tests.isNotEmpty()) bundle.tests.map { it.score }.average().toInt() else 0
    val avgAcc = if (bundle.tests.isNotEmpty()) bundle.tests.map { it.accuracy }.average().toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "EXAM SCHEDULE & SCORES",
                title = "Mock Tests",
                subtitle = "Schedule upcoming tests with syllabus sheets and track /720 score history."
            )
        }

        // 4 KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("LATEST", "${latest?.score ?: 0}/720", latest?.name ?: "No mock yet", valueColor = IgrisRed, modifier = Modifier.weight(1f))
                    StatCard("PERSONAL BEST", "$best/720", "${bundle.tests.size} test(s)", valueColor = IgrisGreen, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("AVERAGE", "$avg/720", "All tests", valueColor = IgrisGold, modifier = Modifier.weight(1f))
                    StatCard("AVG ACCURACY", "$avgAcc%", "Post-test target 88%+", valueColor = IgrisBlue, modifier = Modifier.weight(1f))
                }
            }
        }

        // Upcoming Tests Scheduler
        item {
            IgrisCard {
                EyebrowLabel("UPCOMING TEST SCHEDULER")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = upName,
                    onValueChange = { upName = it },
                    label = { Text("Test Name (e.g. AITS Full Test #4)") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = upDate,
                        onValueChange = { upDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = upTime,
                        onValueChange = { upTime = it },
                        label = { Text("Time") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = upSyllabus,
                    onValueChange = { upSyllabus = it },
                    label = { Text("Syllabus topics") },
                    colors = igrisTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { upImgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Syllabus Photo", color = IgrisText, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { upPdfPicker.launch(arrayOf("application/pdf", "image/*")) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IgrisGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Syllabus PDF", color = IgrisText, fontSize = 11.sp)
                    }
                }
                if (upUri != null) {
                    Spacer(Modifier.height(4.dp))
                    IgrisTag("Attachment ready", tone = "green")
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addUpcomingTest(context, upName, upDate, upTime, upSyllabus, upUri)
                        upName = ""; upSyllabus = ""; upUri = null
                    },
                    enabled = upName.isNotBlank() && upDate.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Schedule Upcoming Test")
                }
            }
        }

        if (bundle.upcomingTests.isNotEmpty()) {
            item { EyebrowLabel("UPCOMING QUEUE (${bundle.upcomingTests.size})") }
            items(bundle.upcomingTests.sortedBy { it.date }, key = { it.id }) { up ->
                val daysDiff = DateUtils.daysUntil(up.date)
                val (badge, tone) = when {
                    daysDiff == 0 -> "TODAY" to "red"
                    daysDiff == 1 -> "Tomorrow" to "gold"
                    daysDiff > 1 -> "in $daysDiff days" to "blue"
                    else -> "Passed ${-daysDiff}d ago" to "neutral"
                }
                IgrisCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(up.name, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                            Text("${up.date} ${up.time}", style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                            if (up.syllabus.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(up.syllabus, style = MaterialTheme.typography.bodyMedium, color = IgrisText)
                            }
                        }
                        IgrisTag(badge, tone = tone)
                        IconButton(onClick = { viewModel.deleteUpcomingTest(up) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete upcoming test", tint = IgrisMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (!up.localFilePath.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { viewModel.openAttachmentPreview(up.fileName ?: up.name, up.localFilePath, up.fileType ?: "*/*") },
                            border = BorderStroke(1.dp, IgrisBlue)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = IgrisBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("View Syllabus File", color = IgrisBlue, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Record Mock Test Form
        item {
            IgrisCard {
                EyebrowLabel("RECORD MOCK RESULT")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mockName,
                    onValueChange = { mockName = it },
                    label = { Text("Mock Test Name") },
                    colors = igrisTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phys,
                        onValueChange = { phys = it.filter { c -> c.isDigit() } },
                        label = { Text("Phys /180") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = chem,
                        onValueChange = { chem = it.filter { c -> c.isDigit() } },
                        label = { Text("Chem /180") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.filter { c -> c.isDigit() } },
                        label = { Text("Bio /360") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = acc,
                        onValueChange = { acc = it.filter { c -> c.isDigit() } },
                        label = { Text("Accuracy %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Total: $autoTotal / 720",
                        fontFamily = DmMonoFontFamily,
                        fontSize = 14.sp,
                        color = IgrisGold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = analysisDone,
                        onCheckedChange = { analysisDone = it },
                        colors = CheckboxDefaults.colors(checkedColor = IgrisGreen)
                    )
                    Text("Post-test paper analysis completed", style = MaterialTheme.typography.bodyMedium, color = IgrisText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { mockImgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Attach OMR/Photo", color = IgrisText, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { mockPdfPicker.launch(arrayOf("application/pdf", "image/*")) },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Attach Paper PDF", color = IgrisText, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addMockTest(
                            context, mockName, autoTotal, acc.toIntOrNull() ?: 0,
                            pInt, cInt, bInt, analysisDone, mockUri
                        )
                        mockName = ""; phys = ""; chem = ""; bio = ""; acc = ""; analysisDone = false; mockUri = null
                    },
                    enabled = mockName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_mock_test_button")
                ) {
                    Text("Save Mock Test")
                }
            }
        }

        // Test History
        item { EyebrowLabel("TEST HISTORY (${bundle.tests.size})") }
        items(bundle.tests.reversed(), key = { it.id }) { t ->
            IgrisCard {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium, color = IgrisText)
                        Text("${t.date} • P:${t.physics} C:${t.chemistry} B:${t.biology}", style = MaterialTheme.typography.bodySmall, color = IgrisMuted)
                    }
                    IgrisTag("${t.score}/720", tone = if (t.score >= 600) "green" else "gold")
                    IconButton(onClick = { viewModel.deleteMockTest(t) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete test", tint = IgrisMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IgrisTag("${t.accuracy}% acc", tone = "blue")
                    OutlinedButton(
                        onClick = { viewModel.toggleTestAnalysis(t) },
                        border = BorderStroke(1.dp, if (t.analysisDone) IgrisGreen else IgrisBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(if (t.analysisDone) "✓ Analyzed" else "Mark Analyzed", color = if (t.analysisDone) IgrisGreen else IgrisMuted, fontSize = 11.sp)
                    }
                    if (!t.localFilePath.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { viewModel.openAttachmentPreview(t.fileName ?: t.name, t.localFilePath, t.fileType ?: "*/*") },
                            border = BorderStroke(1.dp, IgrisBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("View Paper", color = IgrisBlue, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
