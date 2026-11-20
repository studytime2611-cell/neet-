package com.example.data.local

import android.content.Context
import com.example.data.model.FocusSession
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class IgrisRepository(private val dao: IgrisDao) {
    constructor(context: Context) : this(IgrisDatabase.getInstance(context).igrisDao())

    companion object {
        const val LOCAL_COMMANDER_ID = "local_commander"
    }

    fun observeProfile(userId: String = LOCAL_COMMANDER_ID): Flow<UserProfile?> =
        dao.observeProfile(userId)

    fun observeTasks(userId: String = LOCAL_COMMANDER_ID): Flow<List<MissionTask>> =
        dao.observeTasks()

    fun observeChapters(userId: String = LOCAL_COMMANDER_ID): Flow<List<SubjectChapter>> =
        dao.observeChapters()

    fun observeSessions(userId: String = LOCAL_COMMANDER_ID): Flow<List<FocusSession>> =
        dao.observeSessions()

    fun observeMistakes(userId: String = LOCAL_COMMANDER_ID): Flow<List<MistakeEntry>> =
        dao.observeMistakes()

    fun observeRevisions(userId: String = LOCAL_COMMANDER_ID): Flow<List<RevisionEntry>> =
        dao.observeRevisions()

    fun observeTests(userId: String = LOCAL_COMMANDER_ID): Flow<List<MockTestEntry>> =
        dao.observeTests()

    fun observeUpcomingTests(userId: String = LOCAL_COMMANDER_ID): Flow<List<UpcomingTestEntry>> =
        dao.observeUpcomingTests()

    fun observeMockLabs(userId: String = LOCAL_COMMANDER_ID): Flow<List<MockLabPaper>> =
        dao.observeMockLabs()

    fun observeVault(userId: String = LOCAL_COMMANDER_ID): Flow<List<VaultEntry>> =
        dao.observeVault()

    fun observeQuizzes(userId: String = LOCAL_COMMANDER_ID): Flow<List<QuizQuestion>> =
        dao.observeQuizzes()

    fun observeQuizAttempts(userId: String = LOCAL_COMMANDER_ID): Flow<List<QuizAttempt>> =
        dao.observeQuizAttempts()

    suspend fun ensureUserInitialized(displayName: String?): Result<Unit> {
        return try {
            val existing = dao.getProfileOnce(LOCAL_COMMANDER_ID)
            if (existing == null) {
                val safeName = displayName?.takeIf { it.isNotBlank() }?.take(80) ?: "IGRIS"
                dao.upsertProfile(
                    UserProfile(
                        id = LOCAL_COMMANDER_ID,
                        userId = LOCAL_COMMANDER_ID,
                        name = safeName,
                        syllabusLoaded = false
                    )
                )
                loadNeetSyllabus(LOCAL_COMMANDER_ID)
            } else if (!existing.syllabusLoaded) {
                loadNeetSyllabus(LOCAL_COMMANDER_ID)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        currentRank: String? = null,
        upcomingRank: String? = null,
        examDate: String? = null,
        examDateSet: Boolean? = null,
        mockTarget: Int? = null,
        lastRevAlert: String? = null
    ): Result<Unit> {
        return try {
            val current = dao.getProfileOnce(LOCAL_COMMANDER_ID) ?: UserProfile()
            dao.upsertProfile(
                current.copy(
                    currentRank = currentRank?.trim()?.take(40) ?: current.currentRank,
                    upcomingRank = upcomingRank?.trim()?.take(40) ?: current.upcomingRank,
                    examDate = examDate?.trim()?.take(20) ?: current.examDate,
                    examDateSet = examDateSet ?: current.examDateSet,
                    mockTarget = mockTarget?.coerceIn(0, 720) ?: current.mockTarget,
                    lastRevAlert = lastRevAlert?.take(20) ?: current.lastRevAlert
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadNeetSyllabus(userId: String? = null): Result<Int> {
        return try {
            val existing = dao.getChaptersOnce()
            val existingKeys = existing.map {
                "${it.subject.trim().lowercase()}|${it.name.trim().lowercase()}"
            }.toHashSet()

            val toInsert = mutableListOf<SubjectChapter>()
            var order = existing.size

            for ((subject, names) in NeetSyllabus.chaptersBySubject) {
                names.forEachIndexed { idx, name ->
                    val key = "${subject.lowercase()}|${name.trim().lowercase()}"
                    if (!existingKeys.contains(key)) {
                        toInsert.add(
                            SubjectChapter(
                                id = "syl_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                                userId = LOCAL_COMMANDER_ID,
                                subject = subject,
                                branch = if (subject == "Chemistry") NeetSyllabus.chemistryBranchForIndex(idx) else null,
                                name = name.take(200),
                                done = false,
                                strength = "Medium",
                                weakTopics = "",
                                ticks = emptyMap(),
                                sortOrder = order++
                            )
                        )
                    }
                }
            }
            if (toInsert.isNotEmpty()) {
                dao.upsertChapters(toInsert)
            }
            val profile = dao.getProfileOnce(LOCAL_COMMANDER_ID) ?: UserProfile()
            dao.upsertProfile(profile.copy(syllabusLoaded = true))
            Result.success(toInsert.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTask(task: MissionTask): Result<String> {
        return try {
            val docId = safeDocId("task")
            dao.upsertTask(
                task.copy(
                    id = docId,
                    userId = LOCAL_COMMANDER_ID,
                    date = task.date.take(20),
                    text = task.text.trim().take(400),
                    subject = task.subject.trim().take(40),
                    missionType = if (task.missionType in NeetSyllabus.missionTypes) task.missionType else "Practice",
                    focusMins = task.focusMins?.coerceIn(0, 720),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTaskDone(taskId: String, done: Boolean, doneAt: String?): Result<Unit> {
        return try {
            val existing = dao.getTask(taskId) ?: return Result.success(Unit)
            dao.upsertTask(existing.copy(done = done, doneAt = if (done) doneAt?.take(20) else null))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskReminder(taskId: String, reminderAt: String?, reminded: Boolean): Result<Unit> {
        return try {
            val existing = dao.getTask(taskId) ?: return Result.success(Unit)
            dao.upsertTask(existing.copy(reminderAt = reminderAt?.take(32), reminded = reminded))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addChapter(subject: String, branch: String?, name: String, sortOrder: Int): Result<Unit> {
        return try {
            dao.upsertChapter(
                SubjectChapter(
                    id = safeDocId("ch"),
                    userId = LOCAL_COMMANDER_ID,
                    subject = subject.take(40),
                    branch = branch?.take(40),
                    name = name.trim().take(200),
                    done = false,
                    strength = "Medium",
                    weakTopics = "",
                    ticks = emptyMap(),
                    sortOrder = sortOrder.coerceIn(0, 10000)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChapter(
        chapterId: String,
        done: Boolean? = null,
        strength: String? = null,
        weakTopics: String? = null,
        ticks: Map<String, String>? = null
    ): Result<Unit> {
        return try {
            val existing = dao.getChapter(chapterId) ?: return Result.success(Unit)
            dao.upsertChapter(
                existing.copy(
                    done = done ?: existing.done,
                    strength = strength ?: existing.strength,
                    weakTopics = weakTopics?.trim()?.take(500) ?: existing.weakTopics,
                    ticks = ticks ?: existing.ticks
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSession(session: FocusSession, existingId: String? = null): Result<String> {
        return try {
            val docId = existingId ?: safeDocId("sess")
            val existing = existingId?.let { dao.getSession(it) }
            if (existing == null) {
                dao.upsertSession(
                    session.copy(
                        id = docId,
                        userId = LOCAL_COMMANDER_ID,
                        date = session.date.take(20),
                        subject = session.subject.take(60),
                        minutes = session.minutes.coerceIn(0, 1440),
                        questions = session.questions.coerceIn(0, 2000),
                        topic = session.topic.trim().take(400),
                        plannedMins = session.plannedMins?.coerceIn(0, 720),
                        seconds = session.seconds?.coerceIn(0, 86400),
                        createdAtMs = System.currentTimeMillis()
                    )
                )
            } else {
                dao.upsertSession(
                    existing.copy(
                        minutes = session.minutes.coerceIn(0, 1440),
                        seconds = session.seconds?.coerceIn(0, 86400)
                    )
                )
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMistake(entry: MistakeEntry): Result<Unit> {
        return try {
            dao.upsertMistake(
                entry.copy(
                    id = safeDocId("mis"),
                    userId = LOCAL_COMMANDER_ID,
                    chapter = entry.chapter.trim().take(200),
                    error = entry.error.trim().take(2000),
                    fix = entry.fix.trim().take(2000),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMistake(
        mistakeId: String,
        closed: Boolean,
        closedAt: String?,
        fixStreak: Int
    ): Result<Unit> {
        return try {
            val existing = dao.getMistake(mistakeId) ?: return Result.success(Unit)
            dao.upsertMistake(
                existing.copy(
                    closed = closed,
                    closedAt = if (closed) closedAt?.take(20) else null,
                    fixStreak = fixStreak.coerceIn(0, 100)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRevision(entry: RevisionEntry): Result<Unit> {
        return try {
            dao.upsertRevision(
                entry.copy(
                    id = safeDocId("rev"),
                    userId = LOCAL_COMMANDER_ID,
                    chapter = entry.chapter.trim().take(200),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRevisionMilestone(
        revisionId: String,
        lastRevised: String,
        milestoneR3: String? = null,
        milestoneR7: String? = null,
        milestoneR21: String? = null
    ): Result<Unit> {
        return try {
            val existing = dao.getRevision(revisionId) ?: return Result.success(Unit)
            dao.upsertRevision(
                existing.copy(
                    lastRevised = lastRevised.take(20),
                    milestoneR3 = milestoneR3?.take(20) ?: existing.milestoneR3,
                    milestoneR7 = milestoneR7?.take(20) ?: existing.milestoneR7,
                    milestoneR21 = milestoneR21?.take(20) ?: existing.milestoneR21
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMockTest(entry: MockTestEntry): Result<Unit> {
        return try {
            dao.upsertTest(
                entry.copy(
                    id = safeDocId("test"),
                    userId = LOCAL_COMMANDER_ID,
                    name = entry.name.trim().take(200),
                    score = entry.score.coerceIn(0, 720),
                    accuracy = entry.accuracy.coerceIn(0, 100),
                    physics = entry.physics.coerceIn(0, 180),
                    chemistry = entry.chemistry.coerceIn(0, 180),
                    biology = entry.biology.coerceIn(0, 360),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTestAnalysis(testId: String, done: Boolean): Result<Unit> {
        return try {
            val existing = dao.getTest(testId) ?: return Result.success(Unit)
            dao.upsertTest(existing.copy(analysisDone = done))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addUpcomingTest(entry: UpcomingTestEntry): Result<Unit> {
        return try {
            dao.upsertUpcomingTest(
                entry.copy(
                    id = safeDocId("up"),
                    userId = LOCAL_COMMANDER_ID,
                    name = entry.name.trim().take(200),
                    syllabus = entry.syllabus.trim().take(2000),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMockLabPaper(paper: MockLabPaper): Result<String> {
        return try {
            val docId = safeDocId("lab")
            dao.upsertMockLab(
                paper.copy(
                    id = docId,
                    userId = LOCAL_COMMANDER_ID,
                    name = paper.name.trim().take(200),
                    physicsC = paper.physicsC.coerceIn(0, 45),
                    physicsW = paper.physicsW.coerceIn(0, 45),
                    chemistryC = paper.chemistryC.coerceIn(0, 45),
                    chemistryW = paper.chemistryW.coerceIn(0, 45),
                    botanyC = paper.botanyC.coerceIn(0, 45),
                    botanyW = paper.botanyW.coerceIn(0, 45),
                    zoologyC = paper.zoologyC.coerceIn(0, 45),
                    zoologyW = paper.zoologyW.coerceIn(0, 45),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addVaultEntry(entry: VaultEntry): Result<Unit> {
        return try {
            dao.upsertVault(
                entry.copy(
                    id = safeDocId("vault"),
                    userId = LOCAL_COMMANDER_ID,
                    title = entry.title.trim().take(200),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addQuizQuestions(questions: List<QuizQuestion>): Result<List<QuizQuestion>> {
        return try {
            val now = System.currentTimeMillis()
            val saved = questions.mapIndexed { idx, q ->
                val safeOpts = q.opts.take(6).ifEmpty { listOf("Option A", "Option B") }
                val safeAns = q.ans.coerceIn(0, safeOpts.size - 1)
                q.copy(
                    id = safeDocId("q"),
                    userId = LOCAL_COMMANDER_ID,
                    subject = q.subject.ifBlank { "Biology" }.take(40),
                    chapter = q.chapter.ifBlank { "General Practice" }.take(200),
                    q = q.q.trim().take(3000),
                    opts = safeOpts,
                    ans = safeAns,
                    mark = NeetSyllabus.normalizeMark(q.mark),
                    exp = q.exp.trim().take(3000),
                    createdAtMs = now + idx
                )
            }
            dao.upsertQuizzes(saved)
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuizQuestionMark(questionId: String, mark: String): Result<Unit> {
        return try {
            val existing = dao.getQuizQuestion(questionId) ?: return Result.success(Unit)
            dao.upsertQuizQuestion(existing.copy(mark = NeetSyllabus.normalizeMark(mark)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllQuizzes(): Result<Unit> {
        return try {
            dao.clearAllQuizzes()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addQuizAttempt(attempt: QuizAttempt): Result<String> {
        return try {
            val docId = safeDocId("att")
            dao.upsertQuizAttempt(
                attempt.copy(
                    id = docId,
                    userId = LOCAL_COMMANDER_ID,
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(collectionName: String, docId: String): Result<Unit> {
        return try {
            when (collectionName) {
                "tasks" -> dao.deleteTask(docId)
                "chapters" -> dao.deleteChapter(docId)
                "sessions" -> dao.deleteSession(docId)
                "mistakes" -> dao.deleteMistake(docId)
                "revisions" -> dao.deleteRevision(docId)
                "tests" -> dao.deleteTest(docId)
                "upcomingTests" -> dao.deleteUpcomingTest(docId)
                "mockLabs" -> dao.deleteMockLab(docId)
                "vault" -> dao.deleteVault(docId)
                "quizzes" -> dao.deleteQuizQuestion(docId)
                "quizAttempts" -> dao.deleteQuizAttempt(docId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun safeDocId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(18)}"
}
