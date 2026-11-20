package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.firebase.AiTutorRepository
import com.example.data.local.IgrisRepository
import com.example.ui.components.AttachmentPreviewDialog
import com.example.ui.components.BackupDialog
import com.example.ui.components.EyebrowLabel
import com.example.ui.components.FullscreenFocusOverlay
import com.example.ui.components.ReminderAlertBanner
import com.example.ui.components.TopFocusBanner
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.MistakesScreen
import com.example.ui.screens.MockLabScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TestsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.state.IgrisDestination
import com.example.ui.state.IgrisUiState
import com.example.ui.state.IgrisViewModel
import com.example.ui.theme.DmMonoFontFamily
import com.example.ui.theme.IgrisBg
import com.example.ui.theme.IgrisGold
import com.example.ui.theme.IgrisLine
import com.example.ui.theme.IgrisMuted
import com.example.ui.theme.IgrisPanel
import com.example.ui.theme.IgrisRed
import com.example.ui.theme.IgrisSoftRed
import com.example.ui.theme.IgrisText
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun iconForDestination(dest: IgrisDestination): ImageVector = when (dest) {
    IgrisDestination.Dashboard -> Icons.Default.Dashboard
    IgrisDestination.Missions -> Icons.Default.CheckCircle
    IgrisDestination.Subjects -> Icons.Default.Book
    IgrisDestination.Focus -> Icons.Default.Timer
    IgrisDestination.Practice -> Icons.Default.Quiz
    IgrisDestination.Mistakes -> Icons.Default.BugReport
    IgrisDestination.Revision -> Icons.Default.Repeat
    IgrisDestination.Tests -> Icons.Default.Assessment
    IgrisDestination.MockLab -> Icons.Default.Science
    IgrisDestination.Vault -> Icons.Default.FolderSpecial
    IgrisDestination.AiTutor -> Icons.Default.AutoAwesome
    IgrisDestination.Analytics -> Icons.Default.Analytics
}

@Composable
fun IgrisAppRoot() {
    IgrisCommandShell()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IgrisCommandShell() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannels(context)
    }

    val repository = remember(context) { IgrisRepository(context) }
    val aiRepository = remember { AiTutorRepository() }
    val vm: IgrisViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IgrisViewModel(
                    repository = repository,
                    aiRepository = aiRepository,
                    currentUserId = IgrisRepository.LOCAL_COMMANDER_ID,
                    displayName = "IGRIS"
                ) as T
            }
        }
    )

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val timerState by vm.timerState.collectAsStateWithLifecycle()
    val quizState by vm.quizState.collectAsStateWithLifecycle()
    val savedProgress by vm.savedProgress.collectAsStateWithLifecycle()
    val aiState by vm.aiState.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()
    val activeReminder by vm.activeReminderTask.collectAsStateWithLifecycle()
    val previewAttachment by vm.previewAttachment.collectAsStateWithLifecycle()
    val retestIndex by vm.retestIndex.collectAsStateWithLifecycle()

    var currentDest by remember { mutableStateOf(IgrisDestination.Dashboard) }
    var practiceInitialTab by remember { mutableIntStateOf(0) }
    var zenMode by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearToast()
        }
    }

    LaunchedEffect(uiState) {
        val bundle = (uiState as? IgrisUiState.Success)?.data ?: return@LaunchedEffect
        while (true) {
            vm.checkDueReminders(context, bundle)
            delay(30_000L)
        }
    }

    if (currentDest != IgrisDestination.Dashboard && !quizState.active && !timerState.fullscreen && previewAttachment == null) {
        BackHandler { currentDest = IgrisDestination.Dashboard }
    }

    val navigateTo: (IgrisDestination) -> Unit = { dest ->
        if (dest != IgrisDestination.Practice) practiceInitialTab = 0
        currentDest = dest
        scope.launch { drawerState.close() }
    }

    val bottomQuickNav = listOf(
        IgrisDestination.Dashboard,
        IgrisDestination.Missions,
        IgrisDestination.Subjects,
        IgrisDestination.Focus,
        IgrisDestination.Practice
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(IgrisBg)) {
        val isWideScreen = maxWidth >= 840.dp

        val mainContent: @Composable () -> Unit = {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = IgrisBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = IgrisPanel,
                            titleContentColor = IgrisText
                        ),
                        navigationIcon = {
                            if (!isWideScreen && !zenMode) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("nav_drawer_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open navigation menu", tint = IgrisText)
                                }
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.igris_icon),
                                    contentDescription = "IGRIS icon",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentDest.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "IGRIS • NEET 2027",
                                        fontFamily = DmMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = IgrisRed
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { zenMode = !zenMode },
                                modifier = Modifier.testTag("zen_mode_button")
                            ) {
                                Icon(
                                    Icons.Default.CenterFocusStrong,
                                    contentDescription = "Toggle Zen Mode",
                                    tint = if (zenMode) IgrisGold else IgrisMuted
                                )
                            }
                            if (uiState is IgrisUiState.Success) {
                                IconButton(
                                    onClick = { showBackupDialog = true },
                                    modifier = Modifier.testTag("backup_button")
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = "Backup JSON", tint = IgrisMuted)
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWideScreen && !zenMode) {
                        NavigationBar(
                            containerColor = IgrisPanel,
                            contentColor = IgrisText
                        ) {
                            bottomQuickNav.forEach { dest ->
                                val selected = currentDest == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest) },
                                    icon = { Icon(iconForDestination(dest), contentDescription = dest.label) },
                                    label = {
                                        Text(
                                            text = when (dest) {
                                                IgrisDestination.Dashboard -> "Home"
                                                IgrisDestination.Missions -> "Missions"
                                                IgrisDestination.Subjects -> "HQ"
                                                IgrisDestination.Focus -> "Focus"
                                                IgrisDestination.Practice -> "Quiz"
                                                else -> dest.label
                                            },
                                            maxLines = 1,
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IgrisRed,
                                        selectedTextColor = IgrisText,
                                        indicatorColor = IgrisSoftRed,
                                        unselectedIconColor = IgrisMuted,
                                        unselectedTextColor = IgrisMuted
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen && !zenMode) {
                        Surface(
                            modifier = Modifier
                                .width(232.dp)
                                .fillMaxHeight(),
                            color = IgrisPanel,
                            border = BorderStroke(1.dp, IgrisLine)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                EyebrowLabel("COMMAND MODULES", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                IgrisDestination.entries.forEach { dest ->
                                    val selected = currentDest == dest
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navigateTo(dest) }
                                            .testTag("rail_nav_${dest.name.lowercase()}"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) IgrisSoftRed else Color.Transparent,
                                        border = if (selected) BorderStroke(1.dp, IgrisRed) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                iconForDestination(dest),
                                                contentDescription = null,
                                                tint = if (selected) IgrisRed else IgrisMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                dest.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White else IgrisMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TopFocusBanner(
                            timerState = timerState,
                            onToggleRun = { vm.toggleTimerRunning(context) },
                            onOpenFocus = { navigateTo(IgrisDestination.Focus) },
                            onFullscreen = { vm.setTimerFullscreen(true) },
                            onHide = { vm.hideTopFocusBanner() }
                        )
                        ReminderAlertBanner(
                            task = activeReminder,
                            onSnooze = {
                                activeReminder?.let { vm.snoozeMissionReminder(it.id, 10) }
                            },
                            onMarkDone = {
                                activeReminder?.let {
                                    vm.toggleMission(it.id, true)
                                    vm.dismissReminderAlert()
                                }
                            },
                            onDismiss = { vm.dismissReminderAlert() }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (val state = uiState) {
                                is IgrisUiState.Loading -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = IgrisRed)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Loading IGRIS Command Center...", color = IgrisMuted)
                                    }
                                }
                                is IgrisUiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Database Error", style = MaterialTheme.typography.headlineSmall, color = IgrisRed)
                                        Spacer(Modifier.height(8.dp))
                                        Text(state.message, color = IgrisMuted)
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { navigateTo(IgrisDestination.Dashboard) },
                                            colors = ButtonDefaults.buttonColors(containerColor = IgrisRed)
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                                is IgrisUiState.Success -> {
                                    val bundle = state.data
                                    when (currentDest) {
                                        IgrisDestination.Dashboard -> DashboardScreen(
                                            bundle = bundle,
                                            viewModel = vm,
                                            onNavigate = navigateTo
                                        )
                                        IgrisDestination.Missions -> MissionsScreen(
                                            bundle = bundle,
                                            timerState = timerState,
                                            viewModel = vm,
                                            onLaunchFocus = { navigateTo(IgrisDestination.Focus) }
                                        )
                                        IgrisDestination.Subjects -> SubjectsScreen(bundle = bundle, viewModel = vm)
                                        IgrisDestination.Focus -> FocusScreen(
                                            bundle = bundle,
                                            timerState = timerState,
                                            viewModel = vm
                                        )
                                        IgrisDestination.Practice -> PracticeScreen(
                                            bundle = bundle,
                                            quizState = quizState,
                                            savedProgress = savedProgress,
                                            retestIndex = retestIndex,
                                            viewModel = vm,
                                            initialTab = practiceInitialTab
                                        )
                                        IgrisDestination.Mistakes -> MistakesScreen(
                                            bundle = bundle,
                                            viewModel = vm,
                                            onOpenFixLoop = {
                                                practiceInitialTab = 2
                                                currentDest = IgrisDestination.Practice
                                            }
                                        )
                                        IgrisDestination.Revision -> RevisionScreen(bundle = bundle, viewModel = vm)
                                        IgrisDestination.Tests -> TestsScreen(bundle = bundle, viewModel = vm)
                                        IgrisDestination.MockLab -> MockLabScreen(bundle = bundle, viewModel = vm)
                                        IgrisDestination.Vault -> VaultScreen(
                                            bundle = bundle,
                                            aiState = aiState,
                                            viewModel = vm,
                                            onQuizGenerated = {
                                                practiceInitialTab = 0
                                                currentDest = IgrisDestination.Practice
                                            }
                                        )
                                        IgrisDestination.AiTutor -> AiTutorScreen(
                                            bundle = bundle,
                                            aiState = aiState,
                                            viewModel = vm,
                                            onQuizStarted = {
                                                practiceInitialTab = 0
                                                currentDest = IgrisDestination.Practice
                                            }
                                        )
                                        IgrisDestination.Analytics -> AnalyticsScreen(bundle = bundle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isWideScreen) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = IgrisPanel,
                        drawerContentColor = IgrisText,
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.igris_icon),
                                    contentDescription = "IGRIS emblem",
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("IGRIS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                    Text("NEET 2027 COMMAND", fontFamily = DmMonoFontFamily, fontSize = 9.sp, color = IgrisRed)
                                }
                            }
                            HorizontalDivider(color = IgrisLine, modifier = Modifier.padding(bottom = 8.dp))
                            IgrisDestination.entries.forEach { dest ->
                                val selected = currentDest == dest
                                NavigationDrawerItem(
                                    label = { Text(dest.label) },
                                    selected = selected,
                                    onClick = { navigateTo(dest) },
                                    icon = { Icon(iconForDestination(dest), contentDescription = null) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = IgrisSoftRed,
                                        selectedIconColor = IgrisRed,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = IgrisMuted,
                                        unselectedTextColor = IgrisMuted
                                    ),
                                    modifier = Modifier.testTag("drawer_nav_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                content = mainContent
            )
        } else {
            mainContent()
        }
    }

    FullscreenFocusOverlay(
        timerState = timerState,
        onToggleRun = { vm.toggleTimerRunning(context) },
        onStopAndSave = { vm.stopAndSaveTimerSession(context) },
        onReset = { vm.resetCurrentTimer() },
        onToggleFlipMode = { vm.setFlipClockMode(it) },
        onExit = { vm.setTimerFullscreen(false) }
    )

    AttachmentPreviewDialog(
        attachment = previewAttachment,
        onDismiss = { vm.closeAttachmentPreview() }
    )

    if (showBackupDialog && uiState is IgrisUiState.Success) {
        val bundle = (uiState as IgrisUiState.Success).data
        BackupDialog(
            jsonText = vm.buildBackupJson(bundle),
            onImportJson = { vm.importBackupJson(it) },
            onDismiss = { showBackupDialog = false }
        )
    }
}
