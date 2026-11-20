package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val displayDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
    }
    private val shortDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("d MMM", Locale.ENGLISH)
    }
    private val weekdayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE", Locale.ENGLISH)
    }

    fun today(): String = isoFormat.get()!!.format(Date())

    fun addDays(isoDate: String, days: Int): String {
        val cal = parseIsoCalendar(isoDate)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return isoFormat.get()!!.format(cal.time)
    }

    fun dayKeys(n: Int): List<String> {
        val todayStr = today()
        return (0 until n).map { i -> addDays(todayStr, -(n - 1 - i)) }
    }

    fun dayLabel(isoDate: String): String {
        val cal = parseIsoCalendar(isoDate)
        return weekdayFormat.get()!!.format(cal.time)
    }

    fun shortDate(isoDate: String): String {
        return try {
            val cal = parseIsoCalendar(isoDate)
            shortDateFormat.get()!!.format(cal.time)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun dateText(isoDate: String = today()): String {
        return try {
            val cal = parseIsoCalendar(isoDate)
            displayDateFormat.get()!!.format(cal.time)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun daysUntil(isoDate: String): Int {
        return try {
            val target = parseIsoCalendar(isoDate).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((target.timeInMillis - now.timeInMillis) / 86_400_000L).toInt()
        } catch (_: Exception) {
            0
        }
    }

    data class CountdownParts(
        val days: Long,
        val hours: Long,
        val minutes: Long,
        val seconds: Long
    )

    fun countdownParts(examDateIso: String): CountdownParts {
        val cal = parseIsoCalendar(examDateIso).apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = maxOf(0L, cal.timeInMillis - System.currentTimeMillis())
        val days = diff / 86_400_000L
        val hrs = (diff % 86_400_000L) / 3_600_000L
        val mins = (diff % 3_600_000L) / 60_000L
        val secs = (diff % 60_000L) / 1_000L
        return CountdownParts(days, hrs, mins, secs)
    }

    fun formatMMSS(totalSec: Int): String {
        val safe = maxOf(0, totalSec)
        return "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
    }

    fun formatHHMMSS(totalSec: Int): Triple<String, String, String> {
        val safe = maxOf(0, totalSec)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return Triple(
            "%02d".format(Locale.US, h),
            "%02d".format(Locale.US, m),
            "%02d".format(Locale.US, s)
        )
    }

    private fun parseIsoCalendar(isoDate: String): Calendar {
        val parts = isoDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2027
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 5) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 2
        return Calendar.getInstance().apply {
            set(y, m, d, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private val physicsKeywords = Regex(
        "\\b(nlm|kinematics|projectile|friction|work power|rotational|gravitation|shm|wave|electrostatics|capacitor|current electricity|magnetism|emi|ac|optics|ray optics|wave optics|modern physics|atom|nuclei|semiconductor|vector|unit)\\b",
        RegexOption.IGNORE_CASE
    )
    private val chemKeywords = Regex(
        "\\b(mole|stoichiometry|atomic|thermodynamics|equilibrium|electrochemistry|kinetics|chemical bonding|coordination|organic|aldehyde|ketone|carboxylic|amine|haloalkane|alcohol|phenol|biomolecule|iupac|isomerism|periodic|solution|physical chem|inorganic)\\b",
        RegexOption.IGNORE_CASE
    )
    private val bioKeywords = Regex(
        "\\b(cell|mitosis|meiosis|genetics|dna|rna|transcription|translation|evolution|reproduction|human physiology|digestion|breathing|circulation|excretion|neural|endocrine|plant physiology|photosynthesis|respiration|ecology|biodiversity|biotech|microbe|human health|ncert|morphology|anatomy)\\b",
        RegexOption.IGNORE_CASE
    )
    private val lectureKeywords = Regex("\\b(lec|lecture|class|video|oneshot|one shot)\\b", RegexOption.IGNORE_CASE)
    private val revisionKeywords = Regex("\\b(rev|revision|revise|notes|ncert|recall)\\b", RegexOption.IGNORE_CASE)
    private val practiceKeywords = Regex("\\b(mcq|pyq|dpp|practice|question|numerical|test)\\b", RegexOption.IGNORE_CASE)

    private val durationMinutesRegex = Regex(
        "(\\d+(?:\\.\\d+)?)\\s*(hours?|hrs?|hr|h|minutes?|mins?|min|m)\\b",
        RegexOption.IGNORE_CASE
    )

    fun suggestMissionSubject(text: String): String? = when {
        physicsKeywords.containsMatchIn(text) -> "Physics"
        chemKeywords.containsMatchIn(text) -> "Chemistry"
        bioKeywords.containsMatchIn(text) -> "Biology"
        else -> null
    }

    fun suggestMissionType(text: String): String? = when {
        lectureKeywords.containsMatchIn(text) -> "Lecture"
        revisionKeywords.containsMatchIn(text) -> "Revision"
        practiceKeywords.containsMatchIn(text) -> "Practice"
        else -> null
    }

    fun suggestMissionFocusMinutes(text: String): Int? {
        val match = durationMinutesRegex.find(text) ?: return null
        val rawNum = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        val mins = if (unit.startsWith("h")) {
            kotlin.math.round(rawNum * 60.0).toInt()
        } else {
            kotlin.math.round(rawNum).toInt()
        }
        return mins.takeIf { it > 0 }?.coerceIn(1, 720)
    }
}
