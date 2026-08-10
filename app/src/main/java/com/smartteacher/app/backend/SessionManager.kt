package com.smartteacher.app.backend

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persistent, encrypted session storage.
 *
 * Keeps the signed-in teacher or student on the device so they don't need to
 * log in again after closing and reopening the app.
 */
class SessionManager(context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "smart_teacher_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    enum class Role { NONE, TEACHER, STUDENT }

    fun setRole(role: Role) = prefs.edit().putString(KEY_ROLE, role.name).apply()

    fun getRole(): Role =
        Role.valueOf(prefs.getString(KEY_ROLE, Role.NONE.name) ?: Role.NONE.name)

    // ---- Teacher session ----
    fun saveTeacher(teacherId: String, username: String, displayName: String,
                    grade: String?, section: String?) {
        prefs.edit().apply {
            putString(KEY_TEACHER_ID, teacherId)
            putString(KEY_TEACHER_USERNAME, username)
            putString(KEY_TEACHER_NAME, displayName)
            putString(KEY_TEACHER_GRADE, grade)
            putString(KEY_TEACHER_SECTION, section)
        }.apply()
    }

    fun getTeacherId() = prefs.getString(KEY_TEACHER_ID, null)
    fun getTeacherUsername() = prefs.getString(KEY_TEACHER_USERNAME, null)
    fun getTeacherName() = prefs.getString(KEY_TEACHER_NAME, null) ?: ""
    fun getTeacherGrade() = prefs.getString(KEY_TEACHER_GRADE, null)
    fun getTeacherSection() = prefs.getString(KEY_TEACHER_SECTION, null)

    fun isTeacherSetupComplete(): Boolean =
        !getTeacherGrade().isNullOrEmpty() && !getTeacherSection().isNullOrEmpty()

    fun clearTeacher() {
        prefs.edit().apply {
            remove(KEY_TEACHER_ID)
            remove(KEY_TEACHER_USERNAME)
            remove(KEY_TEACHER_NAME)
            remove(KEY_TEACHER_GRADE)
            remove(KEY_TEACHER_SECTION)
        }.apply()
    }

    // ---- Student session ----
    fun saveStudent(studentId: String, studyCode: String, name: String,
                    grade: String, section: String) {
        prefs.edit().apply {
            putString(KEY_STUDENT_ID, studentId)
            putString(KEY_STUDENT_CODE, studyCode)
            putString(KEY_STUDENT_NAME, name)
            putString(KEY_STUDENT_GRADE, grade)
            putString(KEY_STUDENT_SECTION, section)
        }.apply()
    }

    fun getStudentId() = prefs.getString(KEY_STUDENT_ID, null)
    fun getStudentCode() = prefs.getString(KEY_STUDENT_CODE, null)
    fun getStudentName() = prefs.getString(KEY_STUDENT_NAME, null) ?: ""
    fun getStudentGrade() = prefs.getString(KEY_STUDENT_GRADE, null) ?: ""
    fun getStudentSection() = prefs.getString(KEY_STUDENT_SECTION, null) ?: ""

    fun clearStudent() {
        prefs.edit().apply {
            remove(KEY_STUDENT_ID)
            remove(KEY_STUDENT_CODE)
            remove(KEY_STUDENT_NAME)
            remove(KEY_STUDENT_GRADE)
            remove(KEY_STUDENT_SECTION)
        }.apply()
    }

    fun logout() {
        setRole(Role.NONE)
        clearTeacher()
        clearStudent()
    }

    companion object {
        private const val KEY_ROLE = "role"
        private const val KEY_TEACHER_ID = "teacher_id"
        private const val KEY_TEACHER_USERNAME = "teacher_username"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val KEY_TEACHER_GRADE = "teacher_grade"
        private const val KEY_TEACHER_SECTION = "teacher_section"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_STUDENT_CODE = "student_code"
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_STUDENT_GRADE = "student_grade"
        private const val KEY_STUDENT_SECTION = "student_section"
    }
}
