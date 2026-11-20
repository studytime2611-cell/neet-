package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.FocusSession
import com.example.data.model.MistakeEntry
import com.example.data.model.MissionTask
import com.example.data.model.MockLabPaper
import com.example.data.model.MockTestEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RevisionEntry
import com.example.data.model.SubjectChapter
import com.example.data.model.UpcomingTestEntry
import com.example.data.model.UserProfile
import com.example.data.model.VaultEntry
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class IgrisTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val arr = JSONArray()
        value.orEmpty().forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        val obj = JSONObject()
        value.orEmpty().forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun toStringMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optString(k)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

@Dao
interface IgrisDao {
    // Profile
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun observeProfile(id: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileOnce(id: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    // Tasks
    @Query("SELECT * FROM mission_tasks ORDER BY createdAtMs ASC")
    fun observeTasks(): Flow<List<MissionTask>>

    @Query("SELECT * FROM mission_tasks WHERE id = :id LIMIT 1")
    suspend fun getTask(id: String): MissionTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: MissionTask)

    @Query("DELETE FROM mission_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    // Chapters
    @Query("SELECT * FROM subject_chapters ORDER BY sortOrder ASC")
    fun observeChapters(): Flow<List<SubjectChapter>>

    @Query("SELECT * FROM subject_chapters")
    suspend fun getChaptersOnce(): List<SubjectChapter>

    @Query("SELECT * FROM subject_chapters WHERE id = :id LIMIT 1")
    suspend fun getChapter(id: String): SubjectChapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(chapters: List<SubjectChapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapter(chapter: SubjectChapter)

    @Query("DELETE FROM subject_chapters WHERE id = :id")
    suspend fun deleteChapter(id: String)

    // Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY createdAtMs ASC")
    fun observeSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): FocusSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: FocusSession)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // Mistakes
    @Query("SELECT * FROM mistake_entries ORDER BY createdAtMs ASC")
    fun observeMistakes(): Flow<List<MistakeEntry>>

    @Query("SELECT * FROM mistake_entries WHERE id = :id LIMIT 1")
    suspend fun getMistake(id: String): MistakeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMistake(entry: MistakeEntry)

    @Query("DELETE FROM mistake_entries WHERE id = :id")
    suspend fun deleteMistake(id: String)

    // Revisions
    @Query("SELECT * FROM revision_entries ORDER BY createdAtMs ASC")
    fun observeRevisions(): Flow<List<RevisionEntry>>

    @Query("SELECT * FROM revision_entries WHERE id = :id LIMIT 1")
    suspend fun getRevision(id: String): RevisionEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(entry: RevisionEntry)

    @Query("DELETE FROM revision_entries WHERE id = :id")
    suspend fun deleteRevision(id: String)

    // Mock Tests
    @Query("SELECT * FROM mock_tests ORDER BY createdAtMs ASC")
    fun observeTests(): Flow<List<MockTestEntry>>

    @Query("SELECT * FROM mock_tests WHERE id = :id LIMIT 1")
    suspend fun getTest(id: String): MockTestEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTest(entry: MockTestEntry)

    @Query("DELETE FROM mock_tests WHERE id = :id")
    suspend fun deleteTest(id: String)

    // Upcoming Tests
    @Query("SELECT * FROM upcoming_tests ORDER BY createdAtMs ASC")
    fun observeUpcomingTests(): Flow<List<UpcomingTestEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUpcomingTest(entry: UpcomingTestEntry)

    @Query("DELETE FROM upcoming_tests WHERE id = :id")
    suspend fun deleteUpcomingTest(id: String)

    // Mock Lab Papers
    @Query("SELECT * FROM mock_lab_papers ORDER BY createdAtMs ASC")
    fun observeMockLabs(): Flow<List<MockLabPaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMockLab(paper: MockLabPaper)

    @Query("DELETE FROM mock_lab_papers WHERE id = :id")
    suspend fun deleteMockLab(id: String)

    // Vault
    @Query("SELECT * FROM vault_entries ORDER BY createdAtMs ASC")
    fun observeVault(): Flow<List<VaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVault(entry: VaultEntry)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteVault(id: String)

    // Quizzes
    @Query("SELECT * FROM quiz_questions ORDER BY createdAtMs ASC")
    fun observeQuizzes(): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE id = :id LIMIT 1")
    suspend fun getQuizQuestion(id: String): QuizQuestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizzes(questions: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizQuestion(question: QuizQuestion)

    @Query("DELETE FROM quiz_questions")
    suspend fun clearAllQuizzes()

    @Query("DELETE FROM quiz_questions WHERE id = :id")
    suspend fun deleteQuizQuestion(id: String)

    // Quiz Attempts
    @Query("SELECT * FROM quiz_attempts ORDER BY createdAtMs ASC")
    fun observeQuizAttempts(): Flow<List<QuizAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizAttempt(attempt: QuizAttempt)

    @Query("DELETE FROM quiz_attempts WHERE id = :id")
    suspend fun deleteQuizAttempt(id: String)
}

@Database(
    entities = [
        UserProfile::class,
        MissionTask::class,
        SubjectChapter::class,
        FocusSession::class,
        MistakeEntry::class,
        RevisionEntry::class,
        MockTestEntry::class,
        UpcomingTestEntry::class,
        MockLabPaper::class,
        VaultEntry::class,
        QuizQuestion::class,
        QuizAttempt::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(IgrisTypeConverters::class)
abstract class IgrisDatabase : RoomDatabase() {
    abstract fun igrisDao(): IgrisDao

    companion object {
        @Volatile
        private var INSTANCE: IgrisDatabase? = null

        fun getInstance(context: Context): IgrisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IgrisDatabase::class.java,
                    "igris_neet_command.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
