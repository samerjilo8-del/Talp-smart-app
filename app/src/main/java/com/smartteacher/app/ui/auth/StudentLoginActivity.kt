package com.smartteacher.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityStudentLoginBinding
import com.smartteacher.app.ui.student.StudentDashboardActivity
import com.smartteacher.app.notification.NotificationPermissionHelper
import kotlinx.coroutines.launch

class StudentLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.length != 4) {
                binding.etCode.error = "أدخل 4 أرقام"
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val student = Repository.loginStudent(code)
                setLoading(false)
                if (student != null) {
                    session.setRole(SessionManager.Role.STUDENT)
                    session.saveStudent(
                        student.id, student.study_code, student.name,
                        student.grade, student.section
                    )
                    // Register FCM token for this student so the server can push
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        val token = task.result ?: return@addOnCompleteListener
                        lifecycleScope.launch {
                            runCatching { Repository.updateStudentFcmToken(student.id, token) }
                        }
                    }
                    NotificationPermissionHelper.request(this@StudentLoginActivity)
                    startActivity(Intent(this@StudentLoginActivity, StudentDashboardActivity::class.java))
                    finish()
                } else {
                    binding.etCode.error = "رمز غير صحيح"
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
