package com.example.ui.state

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiTutorRepository
import com.example.data.local.IgrisRepository
import com.example.data.model.FocusSession
import com.example.data.model.MarkSummaryItem
import com.example.data.model.MistakeEntry
import com.example.data.model.MissionTask
import com.example.data.model.MockLabPaper
import com.example.data.model.MockTestEntry
import com.example.data.model.NeetSyllabus
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RevisionEntry
import com.example.data.model.SubjectChapter
import com.example.data.model.UpcomingTestEntry
import com.example.data.model.UserProfile
import com.example.data.model.VaultEntry
import com.example.data.model.WrongMcqItem
import com.example.util.DateUtils
import com.example.util.LocalFileHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class IgrisDestination(val label: String) {
    Dashboard("Dashboard"),
    Missions("Daily Missions"),
    Subjects("Subject HQ"),
    Focus("Focus Room"),
    Practice("Practice & Quiz"),
    Mistakes("Mistake Hunter"),
    Revision("Revision Log"),
    Tests("Mock Tests"),
    MockLab("Mock Lab"),
    Vault("Study Vault"),
    AiTutor("AI Tutor"),
    Analytics("Analytics")
}

data class IgrisDataBundle(
    val profile: UserProfile? = null,
    val tasks: List<MissionTask> = emptyList(),
    val chapters: List<SubjectChapter> = emptyList(),
    val sessions: List<FocusSession> = emptyList(),
    val mistakes: List<MistakeEntry> = emptyList(),
    val revisions: List<RevisionEntry> = emptyList(),
    val tests: List<MockTestEntry> = emptyList(),
    val upcomingTests: List<UpcomingTestEntry> = emptyList(),
    val mockLabs: List<MockLabPaper> = emptyList(),
    val vault: List<VaultEntry> = emptyList(),
    val quizzes: List<QuizQuestion> = emptyList(),
    val quizAttempts: List<QuizAttempt> = emptyList()
)

sealed interface IgrisUiState {
    data object Loading : IgrisUiState
    data class Success(val data: IgrisDataBundle) : IgrisUiState
    data class Error(val message: String) : IgrisUiState
}

data class FocusTimerState(
    val initialSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val running: Boolean = false,
    val fullscreen: Boolean = false,
    val bannerHidden: Boolean = false,
    val missionId: String? = null,
    val missionLabel: String = "",
    val ranSeconds: Int = 0,
    val activeSessionId: String? = null,
    val subject: String = "Physics",
    val topic: String = "",
    val flipClockMode: Boolean = false
)

data class QuizProgressSnapshot(
    val questions: List<QuizQuestion>,
    val idx: Int,
    val score: Int,
    val wrongList: List<WrongMcqItem>,
    val elapsedSec: Int,
    val answered: Boolean,
    val pickedIdx: Int?
)

data class ActiveQuizState(
    val active: Boolean = false,
    val quizKey: String? = null,
    val subject: String = "All",
    val chapter: String = "All",
    val questions: List<QuizQuestion> = emptyList(),
    val idx: Int = 0,
    val score: Int = 0,
    val answered: Boolean = false,
    val pickedIdx: Int? = null,
    val wrongList: List<WrongMcqItem> = emptyList(),
    val elapsedSec: Int = 0,
    val skipDrill: Boolean = false,
    val drillNet: Int = 0,
    val drillForced: Int = 0,
    val drillRight: Int = 0,
    val drillWrong: Int = 0,
    val drillSkip: Int = 0
)

data class AiTutorUiState(
    val loading: Boolean = false,
    val streamingText: String = "",
    val generatedQuestions: List<QuizQuestion> = emptyList(),
    val error: String? = null
)

data class PreviewAttachment(
    val title: String,
    val localPath: String,
    val fileType: String
)

class IgrisViewModel(
    private val repository: IgrisRepository,
    private val aiRepository: AiTutorRepository,
    private val currentUserId: String,
    displayName: String?
) : ViewModel() {

    private val _timerState = MutableStateFlow(FocusTimerState())
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private val _quizState = MutableStateFlow(ActiveQuizState())
    val quizState: StateFlow<ActiveQuizState> = _quizState.asStateFlow()

    private val _savedProgress = MutableStateFlow<Map<String, QuizProgressSnapshot>>(emptyMap())
    val savedProgress: StateFlow<Map<String, QuizProgressSnapshot>> = _savedProgress.asStateFlow()

    private val _aiState = MutableStateFlow(AiTutorUiState())
    val aiState: StateFlow<AiTutorUiState> = _aiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _activeReminderTask = MutableStateFlow<MissionTask?>(null)
    val activeReminderTask: StateFlow<MissionTask?> = _activeReminderTask.asStateFlow()

    private val _previewAttachment = MutableStateFlow<PreviewAttachment?>(null)
    val previewAttachment: StateFlow<PreviewAttachment?> = _previewAttachment.asStateFlow()

    private val _retestIndex = MutableStateFlow(0)
    val retestIndex: StateFlow<Int> = _retestIndex.asStateFlow()

    private var timerJob: Job? = null
    private var quizClockJob: Job? = null
    private var lastSessionFlushMs: Long = 0L

    private val groupA = combine(
        repository.observeProfile(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeChapters(currentUserId),
        repository.observeSessions(currentUserId)
    ) { profile, tasks, chapters, sessions ->
        IgrisDataBundle(
            profile = profile,
            tasks = tasks,
            chapters = chapters,
            sessions = sessions
        )
    }

    private val groupB = combine(
        repository.observeMistakes(currentUserId),
        repository.observeRevisions(currentUserId),
        repository.observeTests(currentUserId),
        repository.observeUpcomingTests(currentUserId)
    ) { mistakes, revisions, tests, upcoming ->
        IgrisDataBundle(
            mistakes = mistakes,
            revisions = revisions,
            tests = tests,
            upcomingTests = upcoming
        )
    }

    private val groupC = combine(
        repository.observeMockLabs(currentUserId),
        repository.observeVault(currentUserId),
        repository.observeQuizzes(currentUserId),
        repository.observeQuizAttempts(currentUserId)
    ) { labs, vault, quizzes, attempts ->
        IgrisDataBundle(
            mockLabs = labs,
            vault = vault,
            quizzes = quizzes,
            quizAttempts = attempts
        )
    }

    val uiState: StateFlow<IgrisUiState> = combine(groupA, groupB, groupC) { a, b, c ->
        IgrisDataBundle(
            profile = a.profile,
            tasks = a.tasks,
            chapters = a.chapters.sortedBy { it.sortOrder },
            sessions = a.sessions,
            mistakes = b.mistakes,
            revisions = b.revisions,
            tests = b.tests,
            upcomingTests = b.upcomingTests,
            mockLabs = c.mockLabs,
            vault = c.vault,
            quizzes = c.quizzes,
            quizAttempts = c.quizAttempts
        )
    }
        .map<IgrisDataBundle, IgrisUiState> { IgrisUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing IGRIS data", error)
            emit(IgrisUiState.Error(error.message ?: "Failed to load local database."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = IgrisUiState.Loading
        )

    init {
        viewModelScope.launch {
            val res = repository.ensureUserInitialized(displayName)
            if (res.isFailure) {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to initialize profile.")
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun openAttachmentPreview(title: String, path: String, fileType: String) {
        _previewAttachment.value = PreviewAttachment(title, path, fileType)
    }

    fun closeAttachmentPreview() {
        _previewAttachment.value = null
    }

    fun dismissReminderAlert() {
        _activeReminderTask.value = null
    }

    fun checkDueReminders(context: Context, bundle: IgrisDataBundle) {
        val nowMs = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        val dueTask = bundle.tasks.firstOrNull { t ->
            if (t.done || t.reminderAt.isNullOrBlank() || t.reminded) return@firstOrNull false
            val parsed = try {
                sdf.parse(t.reminderAt)?.time
            } catch (_: Exception) {
                null
            }
            parsed != null && parsed <= nowMs
        }
        if (dueTask != null) {
            _activeReminderTask.value = dueTask
            NotificationHelper.sendNotification(
                context = context,
                id = dueTask.id.hashCode(),
                title = "IGRIS Mission Reminder (${dueTask.subject})",
                body = dueTask.text,
                isFocus = false
            )
            viewModelScope.launch {
                repository.updateTaskReminder(dueTask.id, dueTask.reminderAt, true)
            }
            return
        }

        val todayStr = DateUtils.today()
        val dueRevs = dueRevisions(bundle.revisions)
        val lastAlert = bundle.profile?.lastRevAlert
        if (dueRevs.isNotEmpty() && lastAlert != todayStr) {
            val first = dueRevs.first()
            val extra = if (dueRevs.size > 1) " (+${dueRevs.size - 1} more)" else ""
            val alertTask = MissionTask(
                id = "rev_${first.id}",
                subject = first.subject,
                text = "Spaced revision due: ${first.chapter}$extra"
            )
            _activeReminderTask.value = alertTask
            NotificationHelper.sendNotification(
                context = context,
                id = 9091,
                title = "IGRIS Revision Due",
                body = "${dueRevs.size} chapter(s) need spaced revision today.",
                isFocus = false
            )
            viewModelScope.launch {
                repository.updateProfile(lastRevAlert = todayStr)
            }
        }
    }

    // --- Profile ---
    fun updateRanks(currentRank: String, upcomingRank: String) {
        viewModelScope.launch {
            val res = repository.updateProfile(
                currentRank = currentRank.ifBlank { "4 Lakh" },
                upcomingRank = upcomingRank.ifBlank { "10k" }
            )
            showToast(if (res.isSuccess) "Rank targets updated." else "Could not update ranks.")
        }
    }

    fun updateExamDate(dateIso: String) {
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dateIso)) {
            showToast("Use YYYY-MM-DD date format.")
            return
        }
        viewModelScope.launch {
            val res = repository.updateProfile(examDate = dateIso, examDateSet = true)
            showToast(if (res.isSuccess) "Exam date saved." else "Failed to save exam date.")
        }
    }

    fun updateMockTarget(target: Int) {
        viewModelScope.launch {
            repository.updateProfile(mockTarget = target.coerceIn(0, 720))
        }
    }

    // --- Missions ---
    fun addMission(
        text: String,
        subject: String,
        missionType: String,
        focusMins: Int?,
        reminderAt: String?
    ) {
        if (text.isBlank()) return
        val resolvedFocusMins = focusMins?.takeIf { it > 0 }
            ?: DateUtils.suggestMissionFocusMinutes(text)
        val taskDate = if (!reminderAt.isNullOrBlank() && reminderAt.length >= 10) {
            reminderAt.take(10)
        } else {
            DateUtils.today()
        }
        viewModelScope.launch {
            val trimmedText = text.trim()
            val res = repository.addTask(
                MissionTask(
                    date = taskDate,
                    text = trimmedText,
                    subject = subject,
                    missionType = missionType,
                    done = false,
                    focusMins = resolvedFocusMins,
                    reminderAt = reminderAt?.takeIf { it.isNotBlank() },
                    reminded = false
                )
            )
            if (res.isSuccess) {
                val newTaskId = res.getOrNull()
                if (resolvedFocusMins != null && resolvedFocusMins > 0 && !_timerState.value.running && _timerState.value.ranSeconds == 0) {
                    _timerState.update {
                        it.copy(
                            initialSeconds = resolvedFocusMins * 60,
                            remainingSeconds = resolvedFocusMins * 60,
                            missionId = newTaskId,
                            missionLabel = trimmedText,
                            subject = subject.ifBlank { "General" },
                            topic = trimmedText,
                            activeSessionId = null
                        )
                    }
                    showToast("Mission ($missionType) linked with ${resolvedFocusMins}m Focus timer.")
                } else {
                    showToast("Mission ($missionType) added.")
                }
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to add mission.")
            }
        }
    }

    fun toggleMission(taskId: String, done: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleTaskDone(taskId, done, if (done) DateUtils.today() else null)
            if (res.isFailure) showToast("Could not update mission.")
        }
    }

    fun snoozeMissionReminder(taskId: String, mins: Int = 10) {
        if (taskId.startsWith("rev_")) {
            _activeReminderTask.value = null
            return
        }
        val cal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, mins)
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        val nextIso = sdf.format(cal.time)
        _activeReminderTask.value = null
        viewModelScope.launch {
            repository.updateTaskReminder(taskId, nextIso, false)
            showToast("Snoozed for $mins mins.")
        }
    }

    fun clearMissionReminder(taskId: String) {
        viewModelScope.launch {
            repository.updateTaskReminder(taskId, null, false)
            showToast("Mission reminder removed.")
        }
    }

    fun deleteMission(taskId: String) {
        viewModelScope.launch {
            repository.deleteDocument("tasks", taskId)
            showToast("Mission removed.")
        }
    }

    fun generateWeakMissions(bundle: IgrisDataBundle) {
        val weak = backlogDueList(bundle.chapters).take(5)
        if (weak.isEmpty()) {
            showToast("No weak or revision-pending chapters found.")
            return
        }
        val todayStr = DateUtils.today()
        viewModelScope.launch {
            var count = 0
            for (item in weak) {
                val text = "${item.chapter.name}: 20 MCQ + short revision"
                val exists = bundle.tasks.any { !it.done && it.date == todayStr && it.text.contains(item.chapter.name) }
                if (exists) continue
                val res = repository.addTask(
                    MissionTask(
                        date = todayStr,
                        text = text,
                        subject = item.chapter.subject,
                        missionType = "Practice",
                        done = false,
                        focusMins = 45,
                        source = "weak-auto"
                    )
                )
                if (res.isSuccess) count++
            }
            showToast(if (count > 0) "$count weak-chapter mission(s) added." else "Already queued for today.")
        }
    }

    // --- Subject HQ ---
    fun loadNeetSyllabus() {
        viewModelScope.launch {
            val res = repository.loadNeetSyllabus(currentUserId)
            if (res.isSuccess) {
                val added = res.getOrDefault(0)
                showToast(if (added > 0) "$added NEET chapters loaded." else "All syllabus chapters already present.")
            } else {
                showToast("Failed to load syllabus.")
            }
        }
    }

    fun addChapter(subject: String, branch: String?, name: String, currentCount: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val res = repository.addChapter(subject, branch, name, currentCount + 1)
            showToast(if (res.isSuccess) "Chapter added." else "Failed to add chapter.")
        }
    }

    fun toggleChapterCell(chapter: SubjectChapter, key: String) {
        viewModelScope.launch {
            if (key == "done") {
                repository.updateChapter(chapter.id, done = !chapter.done)
            } else {
                val updated = chapter.ticks.toMutableMap()
                if (updated.containsKey(key)) updated.remove(key) else updated[key] = DateUtils.today()
                repository.updateChapter(chapter.id, ticks = updated)
            }
        }
    }

    fun updateChapterStrength(chapter: SubjectChapter, strength: String) {
        viewModelScope.launch {
            repository.updateChapter(chapter.id, strength = strength)
            showToast("Marked as $strength")
        }
    }

    fun updateChapterWeakTopics(chapter: SubjectChapter, weakTopics: String) {
        viewModelScope.launch {
            repository.updateChapter(chapter.id, weakTopics = weakTopics)
            showToast("Weak subtopics saved.")
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteDocument("chapters", chapterId)
            showToast("Chapter deleted.")
        }
    }

    // --- Focus Timer ---
    fun setTimerMinutes(minutes: Int) {
        val safeMins = minutes.coerceIn(1, 720)
        pauseTimerAndFlush()
        _timerState.update { cur ->
            cur.copy(
                initialSeconds = safeMins * 60,
                remainingSeconds = safeMins * 60,
                running = false,
                missionId = null,
                missionLabel = "",
                ranSeconds = 0,
                activeSessionId = null
            )
        }
    }

    fun setTimerSessionMeta(subject: String, topic: String) {
        _timerState.update {
            it.copy(
                subject = subject.ifBlank { "Physics" },
                topic = topic
            )
        }
    }

    fun setFlipClockMode(enabled: Boolean) {
        _timerState.update { it.copy(flipClockMode = enabled) }
    }

    fun syncMissionDraftToFocusTimer(minutes: Int?, subject: String, label: String) {
        val safeMins = minutes?.takeIf { it > 0 }?.coerceIn(1, 720) ?: return
        val cur = _timerState.value
        if (cur.running || cur.ranSeconds > 0) return
        val trimmedLabel = label.trim()
        _timerState.update {
            it.copy(
                initialSeconds = safeMins * 60,
                remainingSeconds = safeMins * 60,
                missionId = null,
                missionLabel = trimmedLabel,
                subject = subject.ifBlank { "Physics" },
                topic = trimmedLabel,
                activeSessionId = null
            )
        }
    }

    fun unlinkMissionFromTimer() {
        _timerState.update {
            it.copy(
                missionId = null,
                missionLabel = ""
            )
        }
    }

    fun startMissionFocus(task: MissionTask, autoStart: Boolean = false) {
        val mins = (task.focusMins ?: 25).coerceIn(1, 720)
        val cur = _timerState.value
        if (cur.missionId == task.id) {
            if (autoStart && !cur.running) {
                toggleTimerRunning(null)
            } else if (!autoStart) {
                showToast("Mission already loaded in Focus Room (${mins}m).")
            }
            return
        }
        pauseTimerAndFlush()
        _timerState.update {
            it.copy(
                initialSeconds = mins * 60,
                remainingSeconds = mins * 60,
                running = false,
                missionId = task.id,
                missionLabel = task.text,
                subject = task.subject.ifBlank { "General" },
                topic = task.text,
                ranSeconds = 0,
                activeSessionId = null
            )
        }
        if (autoStart) {
            toggleTimerRunning(null)
        } else {
            showToast("Focus timer linked to \"${task.text}\" (${mins}m)")
        }
    }

    fun toggleTimerRunning(context: Context?) {
        val cur = _timerState.value
        if (cur.running) {
            pauseTimerAndFlush()
            return
        }
        val isFreshRestart = cur.remainingSeconds <= 0
        val startSec = if (isFreshRestart) cur.initialSeconds else cur.remainingSeconds
        _timerState.update {
            it.copy(
                remainingSeconds = startSec,
                ranSeconds = if (isFreshRestart) 0 else it.ranSeconds,
                running = true,
                bannerHidden = false
            )
        }
        lastSessionFlushMs = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val s = _timerState.value
                if (!s.running) break
                val nextRem = (s.remainingSeconds - 1).coerceAtLeast(0)
                val nextRan = s.ranSeconds + 1
                _timerState.update { it.copy(remainingSeconds = nextRem, ranSeconds = nextRan) }

                if (nextRem <= 0) {
                    completeTimerSession(context)
                    break
                } else if (s.missionId != null && System.currentTimeMillis() - lastSessionFlushMs >= 15_000L) {
                    lastSessionFlushMs = System.currentTimeMillis()
                    flushMissionSession()
                }
            }
        }
    }

    fun stopAndSaveTimerSession(context: Context?) {
        timerJob?.cancel()
        timerJob = null
        val s = _timerState.value
        val elapsed = s.ranSeconds
        _timerState.update {
            it.copy(
                remainingSeconds = s.initialSeconds,
                running = false,
                ranSeconds = 0,
                activeSessionId = null
            )
        }
        if (elapsed < 5) {
            showToast("Timer stopped. Run at least 5 seconds to log a session.")
            return
        }
        val loggedMins = maxOf(1, kotlin.math.round(elapsed / 60.0).toInt())
        val plannedMins = maxOf(1, kotlin.math.round(s.initialSeconds / 60.0).toInt())
        val sessionSubject = s.subject.ifBlank { "Focus Timer" }
        val sessionTopic = s.topic.trim().ifBlank {
            s.missionLabel.ifBlank { "Study session (${DateUtils.formatMMSS(elapsed)})" }
        }
        viewModelScope.launch {
            val res = repository.saveSession(
                FocusSession(
                    date = DateUtils.today(),
                    subject = sessionSubject,
                    minutes = loggedMins,
                    questions = 0,
                    topic = sessionTopic,
                    missionId = s.missionId,
                    plannedMins = plannedMins,
                    seconds = elapsed
                ),
                existingId = s.activeSessionId
            )
            if (res.isSuccess) {
                showToast("Session saved: $loggedMins min ($sessionSubject)")
                context?.let {
                    NotificationHelper.sendNotification(
                        context = it,
                        id = 7772,
                        title = "IGRIS Study Session Logged",
                        body = "$loggedMins min on $sessionSubject ($sessionTopic) saved.",
                        isFocus = true
                    )
                }
            } else {
                showToast("Failed to save study session.")
            }
        }
    }

    fun resetCurrentTimer() {
        val cur = _timerState.value
        pauseTimerAndFlush()
        _timerState.update {
            it.copy(
                remainingSeconds = cur.initialSeconds,
                running = false,
                ranSeconds = 0,
                activeSessionId = null
            )
        }
    }

    fun setTimerFullscreen(fullscreen: Boolean) {
        _timerState.update { it.copy(fullscreen = fullscreen) }
    }

    fun hideTopFocusBanner() {
        _timerState.update { it.copy(bannerHidden = true) }
    }

    private fun pauseTimerAndFlush() {
        timerJob?.cancel()
        timerJob = null
        if (_timerState.value.running) {
            _timerState.update { it.copy(running = false) }
        }
        if (_timerState.value.missionId != null) {
            viewModelScope.launch { flushMissionSession() }
        }
    }

    private suspend fun flushMissionSession() {
        val s = _timerState.value
        val mId = s.missionId ?: return
        val mins = kotlin.math.round(s.ranSeconds / 60.0).toInt()
        if (mins < 1 && s.activeSessionId == null) return
        val tasks = (uiState.value as? IgrisUiState.Success)?.data?.tasks.orEmpty()
        val task = tasks.find { it.id == mId }
        val res = repository.saveSession(
            FocusSession(
                date = DateUtils.today(),
                subject = task?.subject ?: s.subject.ifBlank { "Focus Timer" },
                minutes = maxOf(1, mins),
                questions = 0,
                topic = task?.text ?: s.topic.ifBlank { s.missionLabel.ifBlank { "Mission focus" } },
                missionId = mId,
                plannedMins = kotlin.math.round(s.initialSeconds / 60.0).toInt(),
                seconds = s.ranSeconds
            ),
            existingId = s.activeSessionId
        )
        res.getOrNull()?.let { sid ->
            _timerState.update { it.copy(activeSessionId = sid) }
        }
    }

    private suspend fun completeTimerSession(context: Context?) {
        timerJob?.cancel()
        val s = _timerState.value
        val completedMins = maxOf(1, kotlin.math.round(s.initialSeconds / 60.0).toInt())
        val sessionSubject = s.subject.ifBlank { "Focus Timer" }
        val sessionTopic = s.topic.trim().ifBlank {
            if (s.flipClockMode) "Completed flip timer session" else "Completed focus session"
        }
        if (s.missionId != null) {
            _timerState.update { it.copy(running = false, ranSeconds = s.initialSeconds) }
            flushMissionSession()
            _timerState.update { it.copy(ranSeconds = 0, activeSessionId = null) }
        } else {
            _timerState.update { it.copy(running = false, ranSeconds = 0, activeSessionId = null) }
            repository.saveSession(
                FocusSession(
                    date = DateUtils.today(),
                    subject = sessionSubject,
                    minutes = completedMins,
                    questions = 0,
                    topic = sessionTopic,
                    plannedMins = completedMins,
                    seconds = s.initialSeconds
                )
            )
        }
        showToast("Focus complete! $completedMins min $sessionSubject session logged.")
        context?.let {
            NotificationHelper.sendNotification(
                context = it,
                id = 7771,
                title = "IGRIS Focus Complete!",
                body = "$completedMins min $sessionSubject session logged.",
                isFocus = true
            )
        }
    }

    fun logManualSession(subject: String, minutes: Int, topic: String, questions: Int) {
        if (minutes < 1) {
            showToast("Enter valid minutes.")
            return
        }
        viewModelScope.launch {
            val res = repository.saveSession(
                FocusSession(
                    date = DateUtils.today(),
                    subject = subject,
                    minutes = minutes,
                    questions = questions.coerceAtLeast(0),
                    topic = topic.ifBlank { "Study session" }
                )
            )
            showToast(if (res.isSuccess) "Study session saved." else "Failed to save session.")
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteDocument("sessions", sessionId)
            showToast("Session removed.")
        }
    }

    // --- Practice & Quiz ---
    private fun quizKey(sub: String, chap: String, mark: String): String =
        listOf(sub, chap, mark).joinToString("|") { it.ifBlank { "All" }.trim().lowercase() }

    fun startSavedQuiz(
        bank: List<QuizQuestion>,
        subjectFilter: String = "All",
        chapterFilter: String = "All",
        markFilter: String = "All",
        forceRestart: Boolean = false
    ) {
        val targetMark = if (markFilter == "All") "All" else NeetSyllabus.normalizeMark(markFilter)
        val normSub = subjectFilter.trim().lowercase()
        val normChap = chapterFilter.trim().lowercase()
        val pool = bank.filter { q ->
            val qMark = NeetSyllabus.normalizeMark(q.mark)
            (normSub == "all" || q.subject.trim().lowercase() == normSub) &&
                (normChap == "all" || q.chapter.trim().lowercase() == normChap) &&
                (targetMark == "All" || qMark == targetMark)
        }
        if (pool.isEmpty()) {
            showToast("No ${if (targetMark == "All") "saved" else targetMark} MCQs found for this selection.")
            return
        }
        val labelChap = if (chapterFilter == "All") {
            if (targetMark == "All") "All Chapters" else "$targetMark Marked"
        } else chapterFilter

        val key = quizKey(subjectFilter, chapterFilter, markFilter)
        val existingProg = if (!forceRestart) _savedProgress.value[key] else null
        if (existingProg != null && existingProg.idx < existingProg.questions.size) {
            _quizState.value = ActiveQuizState(
                active = true,
                quizKey = key,
                subject = subjectFilter,
                chapter = labelChap,
                questions = existingProg.questions,
                idx = existingProg.idx,
                score = existingProg.score,
                answered = existingProg.answered,
                pickedIdx = existingProg.pickedIdx,
                wrongList = existingProg.wrongList,
                elapsedSec = existingProg.elapsedSec
            )
            startQuizClock()
            showToast("Resumed Q${existingProg.idx + 1}/${existingProg.questions.size}")
            return
        }
        beginQuizSession(pool, subjectFilter, labelChap, key, skipDrill = false)
    }

    fun startSkipDrill(bank: List<QuizQuestion>, count: Int) {
        if (bank.size < 3) {
            showToast("Import at least 3 MCQs first.")
            return
        }
        val pool = bank.shuffled().take(minOf(count, bank.size))
        beginQuizSession(pool, "Mixed", "NEET Skip Drill", key = null, skipDrill = true)
        showToast("Skip drill: ${pool.size} Qs • +4 / -1 / 0 skip")
    }

    fun resetChapterProgress(subject: String, chapter: String) {
        val key = quizKey(subject, chapter, "All")
        _savedProgress.update { it - key }
        showToast("Chapter quiz progress reset.")
    }

    fun importBulkJsonAndStart(rawJson: String) {
        val parsed = parseQuizJson(rawJson)
        if (parsed.isFailure) {
            showToast(parsed.exceptionOrNull()?.message ?: "Invalid JSON format.")
            return
        }
        val questions = parsed.getOrThrow()
        viewModelScope.launch {
            val res = repository.addQuizQuestions(questions)
            if (res.isSuccess) {
                val saved = res.getOrThrow()
                val chapters = saved.map { it.chapter }.distinct()
                val chapLabel = if (chapters.size == 1) chapters.first() else "Mixed Chapters"
                beginQuizSession(saved, saved.first().subject, chapLabel, null, skipDrill = false)
                showToast("${saved.size} MCQs imported & quiz started!")
            } else {
                showToast("Failed to save MCQs.")
            }
        }
    }

    private fun beginQuizSession(
        questions: List<QuizQuestion>,
        subject: String,
        chapter: String,
        key: String?,
        skipDrill: Boolean
    ) {
        _quizState.value = ActiveQuizState(
            active = true,
            quizKey = key,
            subject = subject,
            chapter = chapter,
            questions = questions.map { it.copy(mark = NeetSyllabus.normalizeMark(it.mark)) },
            skipDrill = skipDrill
        )
        startQuizClock()
    }

    private fun startQuizClock() {
        quizClockJob?.cancel()
        quizClockJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val st = _quizState.value
                if (!st.active || st.idx >= st.questions.size) break
                _quizState.update { it.copy(elapsedSec = it.elapsedSec + 1) }
            }
        }
    }

    fun exitActiveQuiz() {
        quizClockJob?.cancel()
        val st = _quizState.value
        val key = st.quizKey
        if (key != null && st.idx < st.questions.size) {
            _savedProgress.update {
                it + (key to QuizProgressSnapshot(
                    questions = st.questions,
                    idx = st.idx,
                    score = st.score,
                    wrongList = st.wrongList,
                    elapsedSec = st.elapsedSec,
                    answered = st.answered,
                    pickedIdx = st.pickedIdx
                ))
            }
        }
        _quizState.value = ActiveQuizState()
    }

    fun restartActiveQuiz() {
        val st = _quizState.value
        if (st.questions.isEmpty()) return
        st.quizKey?.let { key -> _savedProgress.update { it - key } }
        beginQuizSession(st.questions, st.subject, st.chapter, st.quizKey, st.skipDrill)
    }

    fun setCurrentQuestionMark(mark: String) {
        val norm = NeetSyllabus.normalizeMark(mark)
        val st = _quizState.value
        val curQ = st.questions.getOrNull(st.idx) ?: return
        val updated = st.questions.toMutableList().apply {
            this[st.idx] = curQ.copy(mark = norm)
        }
        _quizState.update { it.copy(questions = updated) }
        if (curQ.id.isNotBlank()) {
            viewModelScope.launch { repository.updateQuizQuestionMark(curQ.id, norm) }
        }
        showToast("Tagged as $norm")
    }

    fun answerQuizOption(choiceIdx: Int, existingMistakes: List<MistakeEntry>) {
        val st = _quizState.value
        if (st.answered || st.idx >= st.questions.size) return
        val curQ = st.questions[st.idx]
        val isCorrect = choiceIdx == curQ.ans
        val trueAns = curQ.opts.getOrElse(curQ.ans) { "" }
        val pickedAns = curQ.opts.getOrElse(choiceIdx) { "" }
        val normMark = NeetSyllabus.normalizeMark(curQ.mark)

        var newScore = st.score
        var newNet = st.drillNet
        var newForced = st.drillForced
        var newRight = st.drillRight
        var newWrong = st.drillWrong
        val newWrongList = st.wrongList.toMutableList()

        if (isCorrect) {
            newScore++
            if (st.skipDrill) {
                newNet += 4
                newForced += 4
                newRight++
            }
        } else {
            if (st.skipDrill) {
                newNet -= 1
                newForced -= 1
                newWrong++
            }
            newWrongList.add(WrongMcqItem(curQ.q, pickedAns, trueAns, normMark, curQ.exp))
            val snippet = curQ.q.take(45)
            val alreadyTracked = existingMistakes.any { !it.closed && it.error.contains(snippet) }
            if (!alreadyTracked) {
                viewModelScope.launch {
                    repository.addMistake(
                        MistakeEntry(
                            date = DateUtils.today(),
                            subject = curQ.subject,
                            type = if (st.skipDrill) "Time management" else "Concept gap",
                            chapter = curQ.chapter,
                            error = "Wrong MCQ [$normMark]: ${curQ.q} (Picked: $pickedAns) | True Answer: $trueAns",
                            fix = if (curQ.exp.isNotBlank()) "True Answer: $trueAns. ${curQ.exp}" else "True Answer: $trueAns."
                        )
                    )
                }
            }
        }

        val updatedState = st.copy(
            answered = true,
            pickedIdx = choiceIdx,
            score = newScore,
            wrongList = newWrongList,
            drillNet = newNet,
            drillForced = newForced,
            drillRight = newRight,
            drillWrong = newWrong
        )
        _quizState.value = updatedState
        if (updatedState.idx + 1 >= updatedState.questions.size) {
            saveQuizAttempt(updatedState)
        }
    }

    fun skipQuizQuestion() {
        val st = _quizState.value
        if (!st.skipDrill || st.answered || st.idx >= st.questions.size) return
        val updatedState = st.copy(
            answered = true,
            pickedIdx = -1,
            drillSkip = st.drillSkip + 1,
            drillForced = st.drillForced - 1
        )
        _quizState.value = updatedState
        if (updatedState.idx + 1 >= updatedState.questions.size) {
            saveQuizAttempt(updatedState)
        }
    }

    fun nextQuizQuestion() {
        val st = _quizState.value
        _quizState.update {
            it.copy(
                idx = it.idx + 1,
                answered = false,
                pickedIdx = null
            )
        }
        if (st.idx + 1 >= st.questions.size) {
            quizClockJob?.cancel()
        }
    }

    private fun saveQuizAttempt(st: ActiveQuizState) {
        quizClockJob?.cancel()
        if (st.questions.isEmpty()) return
        val total = st.questions.size
        val acc = kotlin.math.round(st.score * 100.0 / total).toInt()
        val avgSec = maxOf(1, kotlin.math.round(st.elapsedSec.toDouble() / total).toInt())

        val wrongArr = JSONArray().apply {
            st.wrongList.forEach { w ->
                put(JSONObject().apply {
                    put("q", w.q)
                    put("picked", w.picked)
                    put("correct", w.correct)
                    put("mark", w.mark)
                    put("exp", w.exp)
                })
            }
        }
        val marksArr = JSONArray().apply {
            st.questions.forEach { q ->
                put(JSONObject().apply {
                    put("q", q.q)
                    put("mark", NeetSyllabus.normalizeMark(q.mark))
                })
            }
        }

        st.quizKey?.let { key -> _savedProgress.update { it - key } }

        viewModelScope.launch {
            val res = repository.addQuizAttempt(
                QuizAttempt(
                    date = DateUtils.today(),
                    subject = st.subject,
                    chapter = st.chapter,
                    score = st.score,
                    total = total,
                    accuracy = acc,
                    elapsedSec = st.elapsedSec,
                    avgSec = avgSec,
                    wrongItemsJson = wrongArr.toString(),
                    marksSummaryJson = marksArr.toString()
                )
            )
            if (res.isSuccess) {
                showToast("Attempt saved ($acc% • ${avgSec}s/Q)!")
            }
        }
    }

    fun retryAttemptWrongQuestions(attempt: QuizAttempt, bank: List<QuizQuestion>) {
        val wrong = parseWrongItems(attempt.wrongItemsJson)
        if (wrong.isEmpty()) {
            showToast("No wrong questions in this attempt!")
            return
        }
        val rebuilt = wrong.map { w ->
            val matched = bank.find { it.q == w.q }
            if (matched != null) {
                matched
            } else {
                val opts = listOfNotNull(
                    w.picked.takeIf { it.isNotBlank() },
                    w.correct.takeIf { it.isNotBlank() }
                ).distinct().let { if (it.size < 2) listOf("Option A", "Option B") else it }
                val ansIdx = maxOf(0, opts.indexOf(w.correct))
                QuizQuestion(
                    subject = attempt.subject,
                    chapter = attempt.chapter,
                    q = w.q,
                    opts = opts,
                    ans = ansIdx,
                    mark = NeetSyllabus.normalizeMark(w.mark),
                    exp = w.exp.ifBlank { "True Answer: ${w.correct}" }
                )
            }
        }
        beginQuizSession(rebuilt, attempt.subject, "${attempt.chapter} (Wrong Qs)", null, skipDrill = false)
        showToast("Starting ${rebuilt.size} missed question(s)!")
    }

    fun deleteAttempt(attemptId: String) {
        viewModelScope.launch {
            repository.deleteDocument("quizAttempts", attemptId)
            showToast("Attempt deleted.")
        }
    }

    fun clearSavedQuizzes() {
        viewModelScope.launch {
            repository.clearAllQuizzes()
            _savedProgress.value = emptyMap()
            _quizState.value = ActiveQuizState()
            showToast("Saved MCQs cleared.")
        }
    }

    // --- Mistakes ---
    fun addMistake(
        context: Context,
        subject: String,
        type: String,
        chapter: String,
        error: String,
        fix: String,
        attachmentUri: Uri?
    ) {
        if (chapter.isBlank() || error.isBlank()) return
        viewModelScope.launch {
            val savedFile = attachmentUri?.let { LocalFileHelper.copyUriToVault(context, it).getOrNull() }
            val res = repository.addMistake(
                MistakeEntry(
                    date = DateUtils.today(),
                    subject = subject,
                    type = type,
                    chapter = chapter.trim(),
                    error = error.trim(),
                    fix = fix.trim(),
                    localFilePath = savedFile?.localFilePath,
                    fileName = savedFile?.fileName,
                    fileType = savedFile?.fileType
                )
            )
            showToast(if (res.isSuccess) "Mistake recorded." else "Failed to record mistake.")
        }
    }

    fun toggleMistakeClosed(m: MistakeEntry) {
        viewModelScope.launch {
            val nextClosed = !m.closed
            repository.updateMistake(
                mistakeId = m.id,
                closed = nextClosed,
                closedAt = if (nextClosed) DateUtils.today() else null,
                fixStreak = m.fixStreak
            )
            showToast(if (nextClosed) "Mistake closed." else "Mistake reopened.")
        }
    }

    fun recordRetestResult(m: MistakeEntry, passed: Boolean) {
        viewModelScope.launch {
            if (passed) {
                val nextStreak = m.fixStreak + 1
                val closed = nextStreak >= 2
                repository.updateMistake(
                    mistakeId = m.id,
                    closed = closed,
                    closedAt = if (closed) DateUtils.today() else null,
                    fixStreak = nextStreak
                )
                showToast(
                    if (closed) "2 clears in a row — mistake closed."
                    else "1 more clear needed before it closes (fix loop)."
                )
            } else {
                repository.updateMistake(m.id, closed = false, closedAt = null, fixStreak = 0)
                showToast("Still open. Loop resets — try again after a short revision.")
            }
            _retestIndex.update { it + 1 }
        }
    }

    fun focusMistakeInRetest(mistakeId: String, openMistakes: List<MistakeEntry>) {
        val idx = openMistakes.indexOfFirst { it.id == mistakeId }
        if (idx >= 0) {
            _retestIndex.value = idx
            showToast("Fix loop opened for this mistake.")
        }
    }

    fun deleteMistake(m: MistakeEntry) {
        LocalFileHelper.deleteLocalFile(m.localFilePath)
        viewModelScope.launch {
            repository.deleteDocument("mistakes", m.id)
            showToast("Mistake removed.")
        }
    }

    // --- Revisions ---
    fun addRevision(chapter: String, subject: String, kind: String) {
        if (chapter.isBlank()) return
        val d = DateUtils.today()
        viewModelScope.launch {
            val res = repository.addRevision(
                RevisionEntry(
                    date = d,
                    r3 = DateUtils.addDays(d, 3),
                    r7 = DateUtils.addDays(d, 7),
                    r21 = DateUtils.addDays(d, 21),
                    chapter = chapter.trim(),
                    subject = subject,
                    kind = kind
                )
            )
            showToast(if (res.isSuccess) "Revision logged (+3d / +7d / +21d scheduled)." else "Failed to log revision.")
        }
    }

    fun markRevisionDone(r: RevisionEntry) {
        val t = DateUtils.today()
        viewModelScope.launch {
            when {
                r.r3 <= t && r.milestoneR3.isNullOrBlank() ->
                    repository.updateRevisionMilestone(r.id, t, milestoneR3 = t)
                r.r7 <= t && r.milestoneR7.isNullOrBlank() ->
                    repository.updateRevisionMilestone(r.id, t, milestoneR7 = t)
                r.r21 <= t && r.milestoneR21.isNullOrBlank() ->
                    repository.updateRevisionMilestone(r.id, t, milestoneR21 = t)
                else -> {
                    showToast("No pending milestone for this entry.")
                    return@launch
                }
            }
            showToast("Revision milestone marked.")
        }
    }

    // --- Mock Tests & Upcoming Tests ---
    fun addUpcomingTest(
        context: Context,
        name: String,
        date: String,
        time: String,
        syllabus: String,
        attachmentUri: Uri?
    ) {
        if (name.isBlank() || date.isBlank()) return
        viewModelScope.launch {
            val savedFile = attachmentUri?.let { LocalFileHelper.copyUriToVault(context, it).getOrNull() }
            val res = repository.addUpcomingTest(
                UpcomingTestEntry(
                    name = name.trim(),
                    date = date.trim(),
                    time = time.trim(),
                    syllabus = syllabus.trim(),
                    localFilePath = savedFile?.localFilePath,
                    fileName = savedFile?.fileName,
                    fileType = savedFile?.fileType
                )
            )
            showToast(if (res.isSuccess) "Upcoming test added." else "Failed to add upcoming test.")
        }
    }

    fun deleteUpcomingTest(item: UpcomingTestEntry) {
        LocalFileHelper.deleteLocalFile(item.localFilePath)
        viewModelScope.launch {
            repository.deleteDocument("upcomingTests", item.id)
            showToast("Upcoming test removed.")
        }
    }

    fun addMockTest(
        context: Context,
        name: String,
        score: Int,
        accuracy: Int,
        physics: Int,
        chemistry: Int,
        biology: Int,
        analysisDone: Boolean,
        attachmentUri: Uri?
    ) {
        if (name.isBlank()) return
        val finalScore = if (score > 0) score else (physics + chemistry + biology)
        viewModelScope.launch {
            val savedFile = attachmentUri?.let { LocalFileHelper.copyUriToVault(context, it).getOrNull() }
            val res = repository.addMockTest(
                MockTestEntry(
                    date = DateUtils.today(),
                    name = name.trim(),
                    score = finalScore.coerceIn(0, 720),
                    accuracy = accuracy.coerceIn(0, 100),
                    physics = physics.coerceIn(0, 180),
                    chemistry = chemistry.coerceIn(0, 180),
                    biology = biology.coerceIn(0, 360),
                    analysis = "",
                    analysisDone = analysisDone,
                    localFilePath = savedFile?.localFilePath,
                    fileName = savedFile?.fileName,
                    fileType = savedFile?.fileType
                )
            )
            showToast(if (res.isSuccess) "Mock test saved." else "Failed to save mock test.")
        }
    }

    fun toggleTestAnalysis(test: MockTestEntry) {
        viewModelScope.launch {
            repository.toggleTestAnalysis(test.id, !test.analysisDone)
        }
    }

    fun deleteMockTest(test: MockTestEntry) {
        LocalFileHelper.deleteLocalFile(test.localFilePath)
        viewModelScope.launch {
            repository.deleteDocument("tests", test.id)
            showToast("Mock test removed.")
        }
    }

    // --- Mock Lab ---
    fun addMockLabPaper(
        name: String,
        date: String,
        pC: Int, pW: Int,
        cC: Int, cW: Int,
        bC: Int, bW: Int,
        zC: Int, zW: Int
    ) {
        if (name.isBlank()) return
        if (pC + pW > 45 || cC + cW > 45 || bC + bW > 45 || zC + zW > 45) {
            showToast("Right + Wrong cannot exceed 45 per subject.")
            return
        }
        viewModelScope.launch {
            val res = repository.addMockLabPaper(
                MockLabPaper(
                    name = name.trim(),
                    date = date.ifBlank { DateUtils.today() },
                    physicsC = pC, physicsW = pW,
                    chemistryC = cC, chemistryW = cW,
                    botanyC = bC, botanyW = bW,
                    zoologyC = zC, zoologyW = zW
                )
            )
            showToast(if (res.isSuccess) "Mock Lab paper saved." else "Failed to save paper.")
        }
    }

    fun deleteMockLabPaper(id: String) {
        viewModelScope.launch {
            repository.deleteDocument("mockLabs", id)
            showToast("Paper removed.")
        }
    }

    // --- Study Vault ---
    fun addVaultItem(
        context: Context,
        title: String,
        subject: String,
        chapter: String,
        category: String,
        link: String,
        fileUri: Uri?
    ) {
        if (title.isBlank()) return
        val cleanLink = link.trim()
        if (cleanLink.isNotEmpty() && !cleanLink.startsWith("http://", true) && !cleanLink.startsWith("https://", true)) {
            showToast("Link must start with http:// or https://")
            return
        }
        if (fileUri == null && cleanLink.isEmpty()) {
            showToast("Select a file or enter a Drive link.")
            return
        }
        viewModelScope.launch {
            val savedFile = fileUri?.let { LocalFileHelper.copyUriToVault(context, it).getOrNull() }
            val res = repository.addVaultEntry(
                VaultEntry(
                    date = DateUtils.today(),
                    title = title.trim(),
                    subject = subject,
                    chapter = chapter,
                    category = category,
                    localFilePath = savedFile?.localFilePath,
                    fileName = savedFile?.fileName,
                    fileType = savedFile?.fileType,
                    link = cleanLink.ifBlank { null }
                )
            )
            showToast(if (res.isSuccess) "Saved to Study Vault." else "Failed to save item.")
        }
    }

    fun deleteVaultItem(item: VaultEntry) {
        LocalFileHelper.deleteLocalFile(item.localFilePath)
        viewModelScope.launch {
            repository.deleteDocument("vault", item.id)
            showToast("Removed from Study Vault.")
        }
    }

    fun generateQuizFromVault(item: VaultEntry, onStarted: () -> Unit) {
        val sub = if (item.subject == "Formula / Notes") "Physics" else item.subject
        val chap = item.chapter.ifBlank { item.title }
        val topic = "Material: \"${item.title}\" (${item.category}). High-yield NCERT & NEET PYQ style."
        _aiState.value = AiTutorUiState(loading = true)
        viewModelScope.launch {
            val gen = aiRepository.generateMcqQuiz(sub, chap, topic, 12)
            if (gen.isSuccess) {
                val qs = gen.getOrThrow()
                val savedRes = repository.addQuizQuestions(qs)
                val savedQs = savedRes.getOrDefault(qs)
                _aiState.value = AiTutorUiState(loading = false, generatedQuestions = savedQs)
                beginQuizSession(savedQs, sub, chap, null, skipDrill = false)
                onStarted()
                showToast("${savedQs.size} MCQs generated from Vault!")
            } else {
                val msg = gen.exceptionOrNull()?.localizedMessage ?: "Failed to generate quiz."
                _aiState.value = AiTutorUiState(loading = false, error = msg)
                showToast(msg)
            }
        }
    }

    // --- AI Tutor ---
    fun generateAiMcqs(subject: String, chapter: String, topic: String, count: Int, mistakes: List<MistakeEntry>, mistakeMode: Boolean) {
        val mistakeContext = if (mistakeMode) {
            mistakes.filter { !it.closed }.takeLast(5)
                .joinToString("\n") { "- [${it.subject} - ${it.chapter}] ${it.error}" }
                .ifBlank { "- Focus on NCERT exceptions and numerical speed." }
        } else null

        _aiState.value = AiTutorUiState(loading = true)
        viewModelScope.launch {
            val res = aiRepository.generateMcqQuiz(
                subject = subject,
                chapter = chapter.ifBlank { "NCERT High-Yield" },
                topic = topic.ifBlank { "High-yield NEET concepts & PYQs" },
                count = count,
                mistakeContext = mistakeContext
            )
            if (res.isSuccess) {
                val qs = res.getOrThrow()
                _aiState.value = AiTutorUiState(
                    loading = false,
                    generatedQuestions = qs,
                    streamingText = "Generated ${qs.size} NCERT-aligned NEET MCQs for ${chapter.ifBlank { subject }}. Tap \"Save & Start Quiz\" below to practice them immediately!"
                )
            } else {
                _aiState.value = AiTutorUiState(
                    loading = false,
                    error = res.exceptionOrNull()?.localizedMessage ?: "AI generation failed."
                )
            }
        }
    }

    fun streamAiDoubt(subject: String, chapter: String, topic: String) {
        val prompt = "Explain \"${topic.ifBlank { chapter }}\" from $subject (${chapter.ifBlank { "NEET syllabus" }}) for a NEET UG aspirant. Keep it crisp, highlight NCERT traps or common formula mistakes, and include 2 quick self-check questions at the end."
        _aiState.value = AiTutorUiState(loading = true, streamingText = "")
        viewModelScope.launch {
            aiRepository.streamTutorResponse(prompt)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _aiState.value = AiTutorUiState(
                        loading = false,
                        error = e.localizedMessage ?: "AI Tutor request failed."
                    )
                }
                .collect { partial ->
                    _aiState.update { it.copy(loading = false, streamingText = partial, error = null) }
                }
        }
    }

    fun importAiGeneratedQuestionsAndStart(onStarted: () -> Unit) {
        val qs = _aiState.value.generatedQuestions
        if (qs.isEmpty()) {
            showToast("Generate MCQs first.")
            return
        }
        viewModelScope.launch {
            val res = repository.addQuizQuestions(qs)
            val saved = res.getOrDefault(qs)
            val chap = saved.map { it.chapter }.distinct().let { if (it.size == 1) it.first() else "Mixed Chapters" }
            beginQuizSession(saved, saved.first().subject, chap, null, skipDrill = false)
            onStarted()
            showToast("${saved.size} MCQs saved & quiz started!")
        }
    }

    // --- Backup JSON ---
    fun buildBackupJson(bundle: IgrisDataBundle): String {
        return JSONObject().apply {
            put("exportedAt", DateUtils.today())
            put("tasks", JSONArray().apply {
                bundle.tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("date", t.date); put("text", t.text); put("subject", t.subject)
                        put("missionType", t.missionType); put("done", t.done)
                        t.focusMins?.let { put("focusMins", it) }
                    })
                }
            })
            put("sessions", JSONArray().apply {
                bundle.sessions.forEach { s ->
                    put(JSONObject().apply {
                        put("date", s.date); put("subject", s.subject)
                        put("minutes", s.minutes); put("questions", s.questions); put("topic", s.topic)
                    })
                }
            })
            put("mistakes", JSONArray().apply {
                bundle.mistakes.forEach { m ->
                    put(JSONObject().apply {
                        put("date", m.date); put("subject", m.subject); put("type", m.type)
                        put("chapter", m.chapter); put("error", m.error); put("fix", m.fix)
                        put("closed", m.closed); put("fixStreak", m.fixStreak)
                    })
                }
            })
            put("tests", JSONArray().apply {
                bundle.tests.forEach { t ->
                    put(JSONObject().apply {
                        put("date", t.date); put("name", t.name); put("score", t.score)
                        put("accuracy", t.accuracy); put("physics", t.physics)
                        put("chemistry", t.chemistry); put("biology", t.biology)
                        put("analysisDone", t.analysisDone)
                    })
                }
            })
        }.toString(2)
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(rawJson)
                var imported = 0
                root.optJSONArray("tasks")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val text = o.optString("text").trim()
                        if (text.isBlank()) continue
                        repository.addTask(
                            MissionTask(
                                date = o.optString("date", DateUtils.today()),
                                text = text,
                                subject = o.optString("subject", "General"),
                                missionType = o.optString("missionType", "Practice"),
                                done = o.optBoolean("done", false),
                                focusMins = if (o.has("focusMins")) o.optInt("focusMins") else null
                            )
                        )
                        imported++
                    }
                }
                root.optJSONArray("sessions")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        repository.saveSession(
                            FocusSession(
                                date = o.optString("date", DateUtils.today()),
                                subject = o.optString("subject", "Physics"),
                                minutes = o.optInt("minutes", 25),
                                questions = o.optInt("questions", 0),
                                topic = o.optString("topic", "")
                            )
                        )
                        imported++
                    }
                }
                root.optJSONArray("mistakes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val err = o.optString("error").trim()
                        if (err.isBlank()) continue
                        repository.addMistake(
                            MistakeEntry(
                                date = o.optString("date", DateUtils.today()),
                                subject = o.optString("subject", "Physics"),
                                type = o.optString("type", "Concept gap"),
                                chapter = o.optString("chapter", "General"),
                                error = err,
                                fix = o.optString("fix", ""),
                                closed = o.optBoolean("closed", false),
                                fixStreak = o.optInt("fixStreak", 0)
                            )
                        )
                        imported++
                    }
                }
                showToast("Backup imported ($imported records).")
            } catch (_: Exception) {
                showToast("Invalid backup JSON file.")
            }
        }
    }

    // --- Domain Analytics Helpers ---
    data class BacklogItem(val chapter: SubjectChapter, val reason: String)

    fun backlogDueList(chapters: List<SubjectChapter>): List<BacklogItem> {
        return chapters.mapNotNull { c ->
            val revDone = listOf("r1", "r2", "r3", "r4", "r5").count { c.ticks.containsKey(it) }
            val isWeak = c.strength == "Weak" || c.weakTopics.isNotBlank()
            when {
                isWeak -> BacklogItem(
                    c,
                    if (c.weakTopics.isNotBlank()) "Weak: ${c.weakTopics}" else "Weak chapter"
                )
                c.done && revDone == 0 -> BacklogItem(c, "Completed • Rev 1 pending")
                else -> null
            }
        }
    }

    fun dueRevisions(revisions: List<RevisionEntry>): List<RevisionEntry> {
        val t = DateUtils.today()
        return revisions.filter { r ->
            (r.r3 <= t && r.milestoneR3.isNullOrBlank()) ||
                (r.r7 <= t && r.milestoneR7.isNullOrBlank()) ||
                (r.r21 <= t && r.milestoneR21.isNullOrBlank())
        }.sortedBy { r ->
            listOfNotNull(
                r.r3.takeIf { r.milestoneR3.isNullOrBlank() },
                r.r7.takeIf { r.milestoneR7.isNullOrBlank() },
                r.r21.takeIf { r.milestoneR21.isNullOrBlank() }
            ).minOrNull() ?: r.date
        }
    }

    fun streakCount(bundle: IgrisDataBundle): Int {
        fun qualifies(day: String): Boolean {
            val focusMin = bundle.sessions.filter { it.date == day }.sumOf { it.minutes }
            if (focusMin >= 360) return true
            val dayTasks = bundle.tasks.filter { it.date == day }
            if (dayTasks.isEmpty()) return false
            val done = dayTasks.count { it.done }
            return done.toDouble() / dayTasks.size >= 0.6
        }

        var d = DateUtils.today()
        if (!qualifies(d)) d = DateUtils.addDays(d, -1)
        var count = 0
        while (qualifies(d) && count < 3650) {
            count++
            d = DateUtils.addDays(d, -1)
        }
        return count
    }

    fun chaptersForSubject(subject: String, chapters: List<SubjectChapter>, quizzes: List<QuizQuestion>): List<String> {
        val fromDb = chapters.filter { it.subject == subject }.map { it.name }
        val fromSyl = NeetSyllabus.chaptersBySubject[subject].orEmpty()
        val fromQuiz = quizzes.filter { it.subject == subject }.map { it.chapter }.filter { it.isNotBlank() }
        return (fromDb + fromSyl + fromQuiz).distinct()
    }

    fun parseWrongItems(json: String): List<WrongMcqItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                WrongMcqItem(
                    q = o.optString("q"),
                    picked = o.optString("picked"),
                    correct = o.optString("correct"),
                    mark = o.optString("mark", "Medium"),
                    exp = o.optString("exp")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseMarksSummary(json: String): List<MarkSummaryItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                MarkSummaryItem(
                    q = o.optString("q"),
                    mark = o.optString("mark", "Medium")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseQuizJson(raw: String): Result<List<QuizQuestion>> {
        return try {
            var t = raw.trim()
            val start = t.indexOf('[')
            val end = t.lastIndexOf(']')
            if (start >= 0 && end > start) t = t.substring(start, end + 1)
            val arr = JSONArray(t)
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val qText = obj.optString("q").trim()
                val optsArr = obj.optJSONArray("opts") ?: continue
                val opts = (0 until optsArr.length()).map { optsArr.optString(it) }.filter { it.isNotBlank() }
                if (qText.isBlank() || opts.size < 2) continue
                val ansIdx = when (val rawAns = obj.opt("ans")) {
                    is Number -> rawAns.toInt().coerceIn(0, opts.size - 1)
                    is String -> {
                        val up = rawAns.trim().uppercase()
                        when (up) {
                            "A" -> 0
                            "B" -> 1
                            "C" -> 2
                            "D" -> 3
                            else -> opts.indexOfFirst { it.equals(rawAns.trim(), ignoreCase = true) }.coerceAtLeast(0)
                        }
                    }
                    else -> 0
                }
                val mk = NeetSyllabus.normalizeMark(obj.optString("mark", obj.optString("tag")))
                list.add(
                    QuizQuestion(
                        subject = obj.optString("subject", "Biology").ifBlank { "Biology" },
                        chapter = obj.optString("chapter", "General Practice").ifBlank { "General Practice" },
                        q = qText,
                        opts = opts.take(6),
                        ans = ansIdx,
                        mark = mk,
                        exp = obj.optString("exp").trim()
                    )
                )
            }
            if (list.isEmpty()) Result.failure(IllegalArgumentException("No valid MCQs found in JSON."))
            else Result.success(list)
        } catch (_: Exception) {
            Result.failure(IllegalArgumentException("Invalid JSON format. Check brackets and commas."))
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        quizClockJob?.cancel()
    }

    private companion object {
        const val TAG = "IgrisViewModel"
    }
}
