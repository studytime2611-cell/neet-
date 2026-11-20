package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
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
import com.example.data.model.NeetSyllabus
import com.example.data.model.SubjectChapter
import com.example.ui.components.ChipSelectorRow
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.IgrisCard
import com.example.ui.components.IgrisTag
import com.example.ui.components.SectionHeader
import com.example.ui.components.igrisTextFieldColors
import com.example.ui.state.IgrisDataBundle
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBorderSubtle
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisGoldBorder
import com.example.ui.theme.IgrisGoldSurface
import com.example.ui.theme.IgrisGreen
import com.example.ui.theme.IgrisGreenBorder
import com.example.ui.theme.IgrisGreenSurface
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPanelAlt
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisRedDark
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectsScreen(
    bundle: IgrisDataBundle,
    viewModel: IgrisViewModel
) {
    val tabs = NeetSyllabus.trackTabs
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    var strengthFilter by remember { mutableStateOf("All") }
    var newChapterName by remember { mutableStateOf("") }

    val activeTab = tabs[selectedTabIdx]
    val filteredChapters = remember(bundle.chapters, activeTab, strengthFilter) {
        bundle.chapters.filter { ch ->
            val matchesSubject = ch.subject == activeTab.subject &&
                (activeTab.branch == null || ch.branch == activeTab.branch)
            val matchesStrength = strengthFilter == "All" || ch.strength == strengthFilter
            matchesSubject && matchesStrength
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "NCERT CHAPTER MATRIX",
                title = "Subject HQ",
                subtitle = "Track chapter completion, strength, weak topics, resources & dated Rev 1..5 ticks.",
                trailing = {
                    OutlinedButton(
                        onClick = { viewModel.loadNeetSyllabus() },
                        border = BorderStroke(1.dp, IgrisBorderSubtle),
                        modifier = Modifier.testTag("load_syllabus_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = IgrisRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Load 74 Ch", color = IgrisText, fontSize = 11.sp)
                    }
                }
            )
        }

        // 5 Tabs: Physics, Physical Chem, Inorganic Chem, Organic Chem, Biology
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IgrisRed,
                edgePadding = 4.dp,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTabIdx]),
                        color = IgrisRed
                    )
                }
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTabIdx == idx,
                        onClick = { selectedTabIdx = idx },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("subject_tab_${tab.key.lowercase()}")
                    )
                }
            }
        }

        // Strength Filter & Add Chapter
        item {
            IgrisCard {
                EyebrowLabel("FILTER BY STRENGTH")
                Spacer(Modifier.height(6.dp))
                ChipSelectorRow(
                    options = listOf("All", "Weak", "Medium", "Strong"),
                    selected = strengthFilter,
                    onSelect = { strengthFilter = it }
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newChapterName,
                        onValueChange = { newChapterName = it },
                        label = { Text("Add custom ${activeTab.label} chapter") },
                        colors = igrisTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_chapter_input")
                    )
                    Button(
                        onClick = {
                            if (newChapterName.isNotBlank()) {
                                viewModel.addChapter(
                                    subject = activeTab.subject,
                                    branch = activeTab.branch,
                                    name = newChapterName.trim(),
                                    currentCount = bundle.chapters.size
                                )
                                newChapterName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IgrisRed),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_chapter_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add chapter")
                    }
                }
            }
        }

        if (filteredChapters.isEmpty()) {
            item {
                IgrisCard {
                    Text(
                        text = if (bundle.chapters.isEmpty()) "No chapters loaded yet. Tap \"Load 74 Ch\" above to populate the official NEET 2027 syllabus."
                        else "No ${activeTab.label} chapters match the \"$strengthFilter\" filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IgrisMuted
                    )
                }
            }
        } else {
            items(filteredChapters, key = { it.id }) { chapter ->
                ChapterMatrixCard(
                    chapter = chapter,
                    resourceCols = activeTab.cols.map { it.key to it.label },
                    revCount = activeTab.revCount,
                    onToggleTick = { key -> viewModel.toggleChapterCell(chapter, key) },
                    onChangeStrength = { next -> viewModel.updateChapterStrength(chapter, next) },
                    onSaveWeakTopics = { text -> viewModel.updateChapterWeakTopics(chapter, text) },
                    onDelete = { viewModel.deleteChapter(chapter.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterMatrixCard(
    chapter: SubjectChapter,
    resourceCols: List<Pair<String, String>>,
    revCount: Int,
    onToggleTick: (String) -> Unit,
    onChangeStrength: (String) -> Unit,
    onSaveWeakTopics: (String) -> Unit,
    onDelete: () -> Unit
) {
    var weakDraft by remember(chapter.id, chapter.weakTopics) { mutableStateOf(chapter.weakTopics) }

    IgrisCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onToggleTick("done") },
                shape = RoundedCornerShape(8.dp),
                color = if (chapter.done) IgrisGreenSurface else IgrisPanelAlt,
                border = BorderStroke(1.dp, if (chapter.done) IgrisGreenBorder else IgrisBorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (chapter.done) {
                        Icon(Icons.Default.Check, contentDescription = "Chapter done", tint = IgrisGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IgrisText
                )
                if (chapter.branch != null) {
                    Text(
                        text = "${chapter.subject} • ${chapter.branch}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IgrisMuted
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete chapter", tint = IgrisMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        // Strength chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EyebrowLabel("STRENGTH:")
            listOf("Weak", "Medium", "Strong").forEach { s ->
                val active = chapter.strength == s
                val (bg, fg, br) = when (s) {
                    "Weak" -> Triple(IgrisSoftRed, Color(0xFFFCA5A5), IgrisRedDark)
                    "Strong" -> Triple(IgrisGreenSurface, IgrisGreen, IgrisGreenBorder)
                    else -> Triple(IgrisGoldSurface, IgrisGold, IgrisGoldBorder)
                }
                Surface(
                    modifier = Modifier.clickable { onChangeStrength(s) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (active) bg else IgrisPanelAlt,
                    border = BorderStroke(1.dp, if (active) br else IgrisBorderSubtle)
                ) {
                    Text(
                        text = s,
                        fontFamily = DmMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) fg else IgrisMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // Weak topics inline editor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = weakDraft,
                onValueChange = { weakDraft = it },
                label = { Text("Weak subtopics / formulas") },
                colors = igrisTextFieldColors(),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (weakDraft != chapter.weakTopics) {
                OutlinedButton(
                    onClick = { onSaveWeakTopics(weakDraft.trim()) },
                    border = BorderStroke(1.dp, IgrisGoldBorder)
                ) {
                    Text("Save", color = IgrisGold, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        EyebrowLabel("RESOURCES")
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            resourceCols.forEach { (key, label) ->
                val tickedDate = chapter.ticks[key]
                val isTicked = !tickedDate.isNullOrBlank()
                Surface(
                    modifier = Modifier.clickable { onToggleTick(key) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTicked) IgrisGreenSurface else IgrisPanelAlt,
                    border = BorderStroke(1.dp, if (isTicked) IgrisGreenBorder else IgrisBorderSubtle)
                ) {
                    Text(
                        text = if (isTicked) "✓ $label ($tickedDate)" else label,
                        fontFamily = DmMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (isTicked) IgrisGreen else IgrisMuted,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        EyebrowLabel("REVISIONS (DATED TICKS)")
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..revCount) {
                val rKey = "r$i"
                val rDate = chapter.ticks[rKey]
                val ticked = !rDate.isNullOrBlank()
                Surface(
                    modifier = Modifier.clickable { onToggleTick(rKey) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (ticked) IgrisSoftRed else IgrisPanelAlt,
                    border = BorderStroke(1.dp, if (ticked) IgrisRed else IgrisBorderSubtle)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (ticked) "✓ Rev $i" else "Rev $i",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ticked) Color.White else IgrisMuted
                        )
                        if (ticked) {
                            Text(
                                text = rDate!!,
                                fontFamily = DmMonoFontFamily,
                                fontSize = 9.sp,
                                color = IgrisGold
                            )
                        }
                    }
                }
            }
        }
    }
}
