package com.example.data.firebase

import android.util.Log
import com.example.data.model.NeetSyllabus
import com.example.data.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiTutorRepository {

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val mcqSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "subject" to Schema.string(description = "Physics, Chemistry, or Biology"),
                        "chapter" to Schema.string(description = "Chapter name"),
                        "q" to Schema.string(description = "NEET-level MCQ question stem"),
                        "opts" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 answer options"
                        ),
                        "ans" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option"),
                        "mark" to Schema.enumeration(
                            values = listOf("Imp", "Easy", "Medium", "Hard"),
                            description = "Difficulty or importance tag"
                        ),
                        "exp" to Schema.string(description = "Concise NCERT-aligned explanation")
                    )
                ),
                description = "List of generated NEET multiple-choice questions"
            )
        )
    )

    private val structuredMcqModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mcqSchema
                temperature = 0.4f
            }
        )
    }

    suspend fun generateMcqQuiz(
        subject: String,
        chapter: String,
        topic: String,
        count: Int,
        mistakeContext: String? = null
    ): Result<List<QuizQuestion>> {
        val safeCount = count.coerceIn(3, 25)
        val prompt = buildString {
            append("Act as an expert NEET UG exam question setter for $subject (Chapter: \"$chapter\"). ")
            if (!mistakeContext.isNullOrBlank()) {
                append("Focus on fixing these recent aspirant mistakes:\n$mistakeContext\n")
            }
            if (topic.isNotBlank()) {
                append("Topic focus: $topic. ")
            }
            append("Generate exactly $safeCount high-yield NCERT-aligned NEET multiple choice questions with 4 distinct options each and concise explanations.")
        }

        return try {
            val response = withTimeout(60_000L) {
                structuredMcqModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("questions")
            val parsed = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val qText = obj.optString("q").trim()
                    val optsArr = obj.optJSONArray("opts") ?: continue
                    val opts = (0 until optsArr.length())
                        .map { optsArr.optString(it).trim() }
                        .filter { it.isNotEmpty() }
                    if (qText.isEmpty() || opts.size < 2) continue
                    val ansIdx = obj.optInt("ans", 0).coerceIn(0, opts.size - 1)
                    add(
                        QuizQuestion(
                            subject = obj.optString("subject", subject).ifBlank { subject },
                            chapter = obj.optString("chapter", chapter).ifBlank { chapter },
                            q = qText,
                            opts = opts.take(6),
                            ans = ansIdx,
                            mark = NeetSyllabus.normalizeMark(obj.optString("mark")),
                            exp = obj.optString("exp").trim()
                        )
                    )
                }
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No valid MCQs could be parsed from response."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "MCQ generation timed out", e)
            Result.failure(Exception("Generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI MCQ generation failed", e)
            Result.failure(e)
        }
    }

    fun streamTutorResponse(prompt: String): Flow<String> = flow {
        val acc = StringBuilder()
        try {
            withTimeout(60_000L) {
                textModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        acc.append(piece)
                        emit(acc.toString())
                    }
                }
            }
            if (acc.isEmpty()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming timed out", e)
            throw Exception("AI Tutor timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI streaming failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "AiTutorRepository"
    }
}
