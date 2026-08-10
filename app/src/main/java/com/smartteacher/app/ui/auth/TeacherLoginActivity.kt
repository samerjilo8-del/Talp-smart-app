package com.smartteacher.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityTeacherLoginBinding
import com.smartteacher.app.ui.teacher.TeacherDashboardActivity
import kotlinx.coroutines.launch

class TeacherLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                binding.etUsername.error = if (username.isEmpty()) getString(R.string.username) else null
                binding.etPassword.error = if (password.isEmpty()) getString(R.string.password) else null
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val teacher = Repository.loginTeacher(username, password)
                setLoading(false)
                if (teacher != null) {
                    session.setRole(SessionManager.Role.TEACHER)
                    session.saveTeacher(
                        teacher.id, teacher.username, teacher.display_name,
                        teacher.grade, teacher.section
                    )
                    // If grade/section not set yet -> first-time setup
                    if (teacher.grade.isNullOrBlank() || teacher.section.isNullOrBlank()) {
                        startActivity(Intent(this@TeacherLoginActivity, TeacherSetupActivity::class.java))
                    } else {
                        startActivity(Intent(this@TeacherLoginActivity, TeacherDashboardActivity::class.java))
                    }
                    finish()
                } else {
                    binding.etPassword.error = "بيانات الدخول غير صحيحة"
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
