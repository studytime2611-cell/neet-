package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "local_commander",
    val userId: String = "local_commander",
    val name: String = "IGRIS",
    val currentRank: String = "4 Lakh",
    val upcomingRank: String = "10k",
    val examDate: String = "2027-05-02",
    val examDateSet: Boolean = false,
    val mockTarget: Int = 650,
    val syllabusLoaded: Boolean = false,
    val lastRevAlert: String? = null
)

@Entity(tableName = "mission_tasks")
data class MissionTask(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val text: String = "",
    val subject: String = "General",
    val missionType: String = "Practice",
    val done: Boolean = false,
    val doneAt: String? = null,
    val focusMins: Int? = null,
    val reminderAt: String? = null,
    val reminded: Boolean = false,
    val source: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "subject_chapters")
data class SubjectChapter(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val subject: String = "",
    val branch: String? = null,
    val name: String = "",
    val done: Boolean = false,
    val strength: String = "Medium",
    val weakTopics: String = "",
    val ticks: Map<String, String> = emptyMap(),
    val sortOrder: Int = 0
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val subject: String = "Focus Timer",
    val minutes: Int = 0,
    val questions: Int = 0,
    val topic: String = "",
    val missionId: String? = null,
    val plannedMins: Int? = null,
    val seconds: Int? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistake_entries")
data class MistakeEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val subject: String = "Physics",
    val type: String = "Concept gap",
    val chapter: String = "",
    val error: String = "",
    val fix: String = "",
    val closed: Boolean = false,
    val closedAt: String? = null,
    val fixStreak: Int = 0,
    val localFilePath: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "revision_entries")
data class RevisionEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val r3: String = "",
    val r7: String = "",
    val r21: String = "",
    val chapter: String = "",
    val subject: String = "Biology",
    val kind: String = "NCERT",
    val lastRevised: String? = null,
    val milestoneR3: String? = null,
    val milestoneR7: String? = null,
    val milestoneR21: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_tests")
data class MockTestEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val name: String = "",
    val score: Int = 0,
    val accuracy: Int = 0,
    val physics: Int = 0,
    val chemistry: Int = 0,
    val biology: Int = 0,
    val analysis: String = "",
    val analysisDone: Boolean = false,
    val localFilePath: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "upcoming_tests")
data class UpcomingTestEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val syllabus: String = "",
    val localFilePath: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_lab_papers")
data class MockLabPaper(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val name: String = "",
    val date: String = "",
    val physicsC: Int = 0,
    val physicsW: Int = 0,
    val chemistryC: Int = 0,
    val chemistryW: Int = 0,
    val botanyC: Int = 0,
    val botanyW: Int = 0,
    val zoologyC: Int = 0,
    val zoologyW: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    fun scorePhysics() = physicsC * 4 - physicsW
    fun scoreChemistry() = chemistryC * 4 - chemistryW
    fun scoreBotany() = botanyC * 4 - botanyW
    fun scoreZoology() = zoologyC * 4 - zoologyW
    fun totalCorrect() = physicsC + chemistryC + botanyC + zoologyC
    fun totalWrong() = physicsW + chemistryW + botanyW + zoologyW
    fun totalAttempted() = totalCorrect() + totalWrong()
    fun totalSkipped() = 180 - totalAttempted()
    fun totalScore() = scorePhysics() + scoreChemistry() + scoreBotany() + scoreZoology()
    fun accuracyFraction(): Float = if (totalAttempted() > 0) totalCorrect().toFloat() / totalAttempted() else 0f
}

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val title: String = "",
    val subject: String = "Physics",
    val chapter: String = "",
    val category: String = "Short Notes PDF",
    val localFilePath: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val link: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val subject: String = "Biology",
    val chapter: String = "General Practice",
    val q: String = "",
    val opts: List<String> = emptyList(),
    val ans: Int = 0,
    val mark: String = "Medium",
    val exp: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey val id: String = "",
    val userId: String = "local_commander",
    val date: String = "",
    val subject: String = "Mixed",
    val chapter: String = "General Practice",
    val score: Int = 0,
    val total: Int = 0,
    val accuracy: Int = 0,
    val elapsedSec: Int = 0,
    val avgSec: Int = 0,
    val wrongItemsJson: String = "[]",
    val marksSummaryJson: String = "[]",
    val createdAtMs: Long = System.currentTimeMillis()
)

data class WrongMcqItem(
    val q: String,
    val picked: String,
    val correct: String,
    val mark: String,
    val exp: String
)

data class MarkSummaryItem(
    val q: String,
    val mark: String
)

data class TrackColumn(
    val key: String,
    val label: String,
    val revision: Boolean = false
)

data class TrackTabConfig(
    val key: String,
    val label: String,
    val subject: String,
    val branch: String?,
    val cols: List<TrackColumn>,
    val revCount: Int
)

object NeetSyllabus {
    val missionTypes = listOf("Practice", "Revision", "Lecture")
    val quizMarks = listOf("Imp", "Easy", "Medium", "Hard")
    val subjectsList = listOf("Physics", "Chemistry", "Biology")

    val chaptersBySubject: Map<String, List<String>> = mapOf(
        "Physics" to listOf(
            "Physics and Measurement", "Kinematics", "Laws of Motion", "Work, Energy and Power",
            "Rotational Motion", "Gravitation", "Properties of Solids and Liquids", "Thermodynamics",
            "Kinetic Theory of Gases", "Oscillations and Waves", "Electrostatics", "Current Electricity",
            "Magnetic Effects of Current and Magnetism", "Electromagnetic Induction and Alternating Currents",
            "Electromagnetic Waves", "Optics", "Dual Nature of Matter and Radiation",
            "Atoms and Nuclei", "Electronic Devices", "Experimental Skills"
        ),
        "Chemistry" to listOf(
            "Some Basic Concepts in Chemistry", "Atomic Structure", "Chemical Bonding and Molecular Structure",
            "Chemical Thermodynamics", "Solutions", "Equilibrium", "Redox Reactions and Electrochemistry",
            "Chemical Kinetics", "Classification of Elements and Periodicity in Properties", "p-Block Elements",
            "d- and f-Block Elements", "Coordination Compounds", "Purification and Characterisation of Organic Compounds",
            "Some Basic Principles of Organic Chemistry", "Hydrocarbons", "Organic Compounds Containing Halogens",
            "Organic Compounds Containing Oxygen", "Organic Compounds Containing Nitrogen",
            "Biomolecules", "Principles Related to Practical Chemistry"
        ),
        "Biology" to listOf(
            "The Living World", "Biological Classification", "Plant Kingdom", "Animal Kingdom",
            "Morphology of Flowering Plants", "Anatomy of Flowering Plants",
            "Floral Families (Malvaceae, Cruciferae, Leguminosae, Compositae, Gramineae)", "Animal Tissues",
            "Frog (Morphology and Anatomy)", "Cell: The Unit of Life", "Biomolecules and Enzymes",
            "Cell Cycle and Cell Division", "Photosynthesis in Higher Plants", "Respiration in Plants",
            "Plant Growth and Development", "Breathing and Exchange of Gases", "Body Fluids and Circulation",
            "Excretory Products and Their Elimination", "Locomotion and Movement",
            "Neural Control and Coordination", "Chemical Coordination and Integration",
            "Sexual Reproduction in Flowering Plants", "Human Reproduction", "Reproductive Health",
            "Principles of Inheritance and Variation", "Molecular Basis of Inheritance", "Evolution",
            "Human Health and Disease", "Microbes in Human Welfare",
            "Biotechnology: Principles and Processes", "Biotechnology and Its Applications",
            "Organisms and Populations", "Ecosystem", "Biodiversity and Conservation"
        )
    )

    private val chemCols = listOf(
        TrackColumn("module", "Module"),
        TrackColumn("dpp", "DPP"),
        TrackColumn("pyqj", "JEE Mains PYQ"),
        TrackColumn("pyqn", "NEET PYQ"),
        TrackColumn("air", "AIR Assignment")
    )

    val trackTabs = listOf(
        TrackTabConfig(
            key = "Physics",
            label = "Physics",
            subject = "Physics",
            branch = null,
            cols = listOf(
                TrackColumn("module", "Module"),
                TrackColumn("dpp", "DPP"),
                TrackColumn("pyqj", "PYQ JEE"),
                TrackColumn("pyqn", "PYQ NEET")
            ),
            revCount = 4
        ),
        TrackTabConfig("Physical", "Physical Chem", "Chemistry", "Physical", chemCols, 4),
        TrackTabConfig("Inorganic", "Inorganic Chem", "Chemistry", "Inorganic", chemCols, 4),
        TrackTabConfig("Organic", "Organic Chem", "Chemistry", "Organic", chemCols, 4),
        TrackTabConfig(
            key = "Biology",
            label = "Biology",
            subject = "Biology",
            branch = null,
            cols = listOf(
                TrackColumn("punch", "NCERT Punch"),
                TrackColumn("module", "Module"),
                TrackColumn("ncert", "NCERT Line-by-Line")
            ),
            revCount = 5
        )
    )

    fun chemistryBranchForIndex(index: Int): String = when {
        index < 8 -> "Physical"
        index < 12 -> "Inorganic"
        else -> "Organic"
    }

    fun normalizeMark(raw: String?): String = when (raw?.trim()?.lowercase()) {
        "imp", "important" -> "Imp"
        "easy" -> "Easy"
        "hard" -> "Hard"
        "medium", "med" -> "Medium"
        else -> "Medium"
    }
}
