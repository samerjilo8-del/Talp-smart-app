package com.smartteacher.app.backend.model

import kotlinx.serialization.Serializable

/** A teacher account. */
@Serializable
data class Teacher(
    val id: String = "",
    val username: String = "",
    val password_hash: String = "",
    val display_name: String = "",
    val grade: String? = null,       // e.g. "الصف الثاني"
    val section: String? = null,     // e.g. "الشعبة 1"
    val created_at: String = ""
)

/** A student. Linked to grade + section and identified by a 4-digit study code. */
@Serializable
data class Student(
    val id: String = "",
    val study_code: String = "",     // 4-digit code used for login
    val name: String = "",
    val grade: String = "",          // e.g. "الصف الثاني"
    val section: String = "",        // e.g. "الشعبة 1"
    val fcm_token: String? = null,
    val created_at: String = ""
)

/** A subject. Uses one or two terms. */
@Serializable
data class Subject(
    val id: String = "",
    val teacher_id: String = "",
    val grade: String = "",
    val section: String = "",
    val name: String = "",
    val terms: Int = 1,              // 1 = one term, 2 = two terms
    val created_at: String = ""
)

/** An assignment / homework broadcast to a whole class/section. */
@Serializable
data class Assignment(
    val id: String = "",
    val teacher_id: String = "",
    val grade: String = "",
    val section: String = "",
    val subject_id: String? = null,
    val subject_name: String = "",
    val title: String = "",
    val content: String = "",
    val due_date: String = "",
    val created_at: String = ""
)

/** An exam broadcast to a whole class/section. */
@Serializable
data class Exam(
    val id: String = "",
    val teacher_id: String = "",
    val grade: String = "",
    val section: String = "",
    val subject_id: String? = null,
    val subject_name: String = "",
    val title: String = "",
    val term: Int = 1,               // 1 = Term 1, 2 = Term 2
    val exam_date: String = "",
    val notes: String = "",
    val created_at: String = ""
)

/** A student's grade for a specific subject and term. */
@Serializable
data class Grade(
    val id: String = "",
    val teacher_id: String = "",
    val student_id: String = "",
    val subject_id: String = "",
    val subject_name: String = "",
    val term: Int = 1,               // 1 = Term 1, 2 = Term 2
    val score: Double = 0.0,
    val max_score: Double = 100.0,
    val created_at: String = ""
)

/** A note broadcast to a whole class/section. */
@Serializable
data class Note(
    val id: String = "",
    val teacher_id: String = "",
    val grade: String = "",
    val section: String = "",
    val title: String = "",
    val content: String = "",
    val created_at: String = ""
)

/** A weekly schedule entry. day: 0=Sun..4=Thu, lesson: 1..6 */
@Serializable
data class ScheduleEntry(
    val id: String = "",
    val teacher_id: String = "",
    val grade: String = "",
    val section: String = "",
    val day: Int = 0,
    val lesson: Int = 1,
    val subject_name: String = "",
    val created_at: String = ""
)
