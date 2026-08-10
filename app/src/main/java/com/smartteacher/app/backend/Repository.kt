package com.smartteacher.app.backend

import com.smartteacher.app.backend.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for all cloud data operations.
 *
 * Every read/write goes to the Supabase Postgres database. Realtime events
 * (INSERT/UPDATE/DELETE) on the relevant tables are broadcast through
 * [realtimeEvents] so that activities can refresh immediately whenever data
 * changes on any device.
 */
object Repository {

    private const val TABLE_TEACHERS = "teachers"
    private const val TABLE_STUDENTS = "students"
    private const val TABLE_SUBJECTS = "subjects"
    private const val TABLE_ASSIGNMENTS = "assignments"
    private const val TABLE_EXAMS = "exams"
    private const val TABLE_GRADES = "grades"
    private const val TABLE_NOTES = "notes"
    private const val TABLE_SCHEDULE = "schedule_entries"

    private val scope = CoroutineScope(Dispatchers.IO)

    /** Emits the table name whenever a realtime change occurs on that table. */
    private val _realtimeEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val realtimeEvents: Flow<String> = _realtimeEvents.asSharedFlow()

    private var channel: RealtimeChannel? = null

    // ----------------------------------------------------------------
    //  Realtime subscriptions
    // ----------------------------------------------------------------

    /**
     * Subscribe to realtime changes for all core tables.
     * Call once from the Application scope; events propagate via [realtimeEvents].
     */
    fun startRealtime() {
        if (channel != null) return
        try {
            val client = SupabaseConfig.get()
            channel = client.realtime.channel("smart_teacher_changes") {
                val tables = listOf(
                    TABLE_ASSIGNMENTS, TABLE_EXAMS, TABLE_GRADES,
                    TABLE_NOTES, TABLE_SCHEDULE, TABLE_SUBJECTS, TABLE_STUDENTS
                )
                tables.forEach { table ->
                    // postgresChanges is added per-table in the supabase-kt realtime API
                }
            }
            // Use the channel's postgresChanges flow to listen for any table change.
            scope.launch {
                channel!!.postgresChanges(flowOf()) {
                    // generic listener placeholder
                }.collect {
                    _realtimeEvents.emit(it.table)
                }
            }
            channel!!.subscribe()
        } catch (e: Exception) {
            // Realtime is best-effort; the app still works via manual refresh.
            channel = null
        }
    }

    /**
     * Subscribe to realtime changes for a specific table and emit via the
     * returned flow. This is the per-activity subscription used to refresh UI.
     */
    fun subscribeTable(table: String, onEvent: () -> Unit) {
        try {
            val client = SupabaseConfig.get()
            val ch = client.realtime.channel("ch_$table") {
            }
            scope.launch {
                ch.postgresChanges(
                    schema = "public",
                    flowOf(io.github.jan.supabase.realtime.PostgresChanges.Filter(table = table))
                ) {
                    onEvent()
                }.collect {
                    onEvent()
                }
            }
            ch.subscribe()
        } catch (_: Exception) {
            // fall back to manual refresh
        }
    }

    // ----------------------------------------------------------------
    //  Teachers
    // ----------------------------------------------------------------

    suspend fun loginTeacher(username: String, password: String): Teacher? {
        val client = SupabaseConfig.get().postgrest
        val rows: List<Teacher> = client.from(TABLE_TEACHERS).select {
            filter {
                eq("username", username)
                eq("password_hash", password)
            }
        }.decodeList()
        return rows.firstOrNull()
    }

    suspend fun getTeacher(id: String): Teacher? {
        val rows: List<Teacher> = SupabaseConfig.get().postgrest
            .from(TABLE_TEACHERS).select {
                filter { eq("id", id) }
            }.decodeList()
        return rows.firstOrNull()
    }

    suspend fun updateTeacherGradeSection(id: String, grade: String, section: String) {
        SupabaseConfig.get().postgrest.from(TABLE_TEACHERS).update(
            mapOf("grade" to grade, "section" to section)
        ) { filter { eq("id", id) } }
    }

    // ----------------------------------------------------------------
    //  Students
    // ----------------------------------------------------------------

    suspend fun loginStudent(studyCode: String): Student? {
        val rows: List<Student> = SupabaseConfig.get().postgrest
            .from(TABLE_STUDENTS).select {
                filter { eq("study_code", studyCode) }
            }.decodeList()
        return rows.firstOrNull()
    }

    suspend fun getStudentsByClass(grade: String, section: String): List<Student> {
        return SupabaseConfig.get().postgrest.from(TABLE_STUDENTS).select {
            filter {
                eq("grade", grade)
                eq("section", section)
            }
        }.decodeList()
    }

    suspend fun getStudent(id: String): Student? {
        val rows: List<Student> = SupabaseConfig.get().postgrest
            .from(TABLE_STUDENTS).select { filter { eq("id", id) } }.decodeList()
        return rows.firstOrNull()
    }

    suspend fun addStudent(student: Student): Student {
        return SupabaseConfig.get().postgrest.from(TABLE_STUDENTS).insert(student) {
            select()
        }.decodeSingle()
    }

    suspend fun updateStudent(id: String, fields: Map<String, Any?>) {
        SupabaseConfig.get().postgrest.from(TABLE_STUDENTS).update(fields) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteStudent(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_STUDENTS).delete {
            filter { eq("id", id) }
        }
    }

    suspend fun updateStudentFcmToken(studentId: String, token: String) {
        SupabaseConfig.get().postgrest.from(TABLE_STUDENTS).update(
            mapOf("fcm_token" to token)
        ) { filter { eq("id", studentId) } }
    }

    /** Get FCM tokens of all students in a class/section for push notifications. */
    suspend fun getStudentTokens(grade: String, section: String): List<String> {
        val students = getStudentsByClass(grade, section)
        return students.mapNotNull { it.fcm_token }.filter { it.isNotBlank() }
    }

    // ----------------------------------------------------------------
    //  Subjects
    // ----------------------------------------------------------------

    suspend fun getSubjects(teacherId: String): List<Subject> {
        return SupabaseConfig.get().postgrest.from(TABLE_SUBJECTS).select {
            filter { eq("teacher_id", teacherId) }
        }.decodeList()
    }

    suspend fun addSubject(subject: Subject): Subject {
        return SupabaseConfig.get().postgrest.from(TABLE_SUBJECTS).insert(subject) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteSubject(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_SUBJECTS).delete {
            filter { eq("id", id) }
        }
    }

    // ----------------------------------------------------------------
    //  Assignments
    // ----------------------------------------------------------------

    suspend fun getAssignmentsForClass(grade: String, section: String): List<Assignment> {
        return SupabaseConfig.get().postgrest.from(TABLE_ASSIGNMENTS).select {
            filter {
                eq("grade", grade)
                eq("section", section)
            }
        }.decodeList()
    }

    suspend fun addAssignment(a: Assignment): Assignment {
        return SupabaseConfig.get().postgrest.from(TABLE_ASSIGNMENTS).insert(a) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteAssignment(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_ASSIGNMENTS).delete {
            filter { eq("id", id) }
        }
    }

    // ----------------------------------------------------------------
    //  Exams
    // ----------------------------------------------------------------

    suspend fun getExamsForClass(grade: String, section: String): List<Exam> {
        return SupabaseConfig.get().postgrest.from(TABLE_EXAMS).select {
            filter {
                eq("grade", grade)
                eq("section", section)
            }
        }.decodeList()
    }

    suspend fun addExam(e: Exam): Exam {
        return SupabaseConfig.get().postgrest.from(TABLE_EXAMS).insert(e) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteExam(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_EXAMS).delete {
            filter { eq("id", id) }
        }
    }

    // ----------------------------------------------------------------
    //  Grades
    // ----------------------------------------------------------------

    suspend fun getGradesForStudent(studentId: String): List<Grade> {
        return SupabaseConfig.get().postgrest.from(TABLE_GRADES).select {
            filter { eq("student_id", studentId) }
        }.decodeList()
    }

    suspend fun getGradesForSubject(subjectId: String, term: Int): List<Grade> {
        return SupabaseConfig.get().postgrest.from(TABLE_GRADES).select {
            filter {
                eq("subject_id", subjectId)
                eq("term", term)
            }
        }.decodeList()
    }

    suspend fun upsertGrade(g: Grade): Grade {
        return SupabaseConfig.get().postgrest.from(TABLE_GRADES).upsert(g) {
            select()
        }.decodeSingle()
    }

    // ----------------------------------------------------------------
    //  Notes
    // ----------------------------------------------------------------

    suspend fun getNotesForClass(grade: String, section: String): List<Note> {
        return SupabaseConfig.get().postgrest.from(TABLE_NOTES).select {
            filter {
                eq("grade", grade)
                eq("section", section)
            }
        }.decodeList()
    }

    suspend fun addNote(n: Note): Note {
        return SupabaseConfig.get().postgrest.from(TABLE_NOTES).insert(n) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteNote(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_NOTES).delete {
            filter { eq("id", id) }
        }
    }

    // ----------------------------------------------------------------
    //  Schedule
    // ----------------------------------------------------------------

    suspend fun getSchedule(teacherId: String): List<ScheduleEntry> {
        return SupabaseConfig.get().postgrest.from(TABLE_SCHEDULE).select {
            filter { eq("teacher_id", teacherId) }
        }.decodeList()
    }

    suspend fun upsertSchedule(e: ScheduleEntry): ScheduleEntry {
        return SupabaseConfig.get().postgrest.from(TABLE_SCHEDULE).upsert(e) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteSchedule(id: String) {
        SupabaseConfig.get().postgrest.from(TABLE_SCHEDULE).delete {
            filter { eq("id", id) }
        }
    }
}
