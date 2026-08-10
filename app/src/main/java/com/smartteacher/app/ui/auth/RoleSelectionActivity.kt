package com.smartteacher.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityRoleSelectionBinding
import com.smartteacher.app.ui.student.StudentDashboardActivity
import com.smartteacher.app.ui.teacher.TeacherDashboardActivity

/** First screen: choose Teacher or Student login. */
class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        // Auto-resume existing persistent session
        when (session.getRole()) {
            SessionManager.Role.TEACHER -> {
                if (session.getTeacherId() != null) {
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                    finish()
                    return
                }
            }
            SessionManager.Role.STUDENT -> {
                if (session.getStudentId() != null) {
                    startActivity(Intent(this, StudentDashboardActivity::class.java))
                    finish()
                    return
                }
            }
            else -> {}
        }

        binding.btnTeacher.setOnClickListener {
            startActivity(Intent(this, TeacherLoginActivity::class.java))
        }
        binding.btnStudent.setOnClickListener {
            startActivity(Intent(this, StudentLoginActivity::class.java))
        }
    }
}
