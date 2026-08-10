package com.smartteacher.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityTeacherSetupBinding
import com.smartteacher.app.ui.Constants
import com.smartteacher.app.ui.teacher.TeacherDashboardActivity
import kotlinx.coroutines.launch

/**
 * First-time teacher setup: pick Grade and Section. The selection is saved
 * both locally (session) and in the cloud (teacher row) so it is remembered
 * across devices and doesn't need to be chosen repeatedly.
 */
class TeacherSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherSetupBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        Constants.GRADES.forEachIndexed { i, g ->
            val rb = RadioButton(this).apply {
                text = g
                id = i
                textSize = 16f
            }
            binding.rgGrade.addView(rb)
        }

        Constants.SECTIONS.forEachIndexed { i, s ->
            val rb = RadioButton(this).apply {
                text = s
                id = i
                textSize = 16f
            }
            binding.rgSection.addView(rb)
        }

        binding.btnSave.setOnClickListener {
            val gradeId = binding.rgGrade.checkedRadioButtonId
            val sectionId = binding.rgSection.checkedRadioButtonId
            if (gradeId == -1 || sectionId == -1) {
                android.widget.Toast.makeText(this, "اختر الصف والشعبة", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val grade = Constants.GRADES[gradeId]
            val section = Constants.SECTIONS[sectionId]
            setLoading(true)
            val teacherId = session.getTeacherId() ?: run {
                setLoading(false)
                return@setOnClickListener
            }
            lifecycleScope.launch {
                runCatching { Repository.updateTeacherGradeSection(teacherId, grade, section) }
                session.saveTeacher(teacherId, session.getTeacherUsername() ?: "",
                    session.getTeacherName(), grade, section)
                setLoading(false)
                startActivity(Intent(this@TeacherSetupActivity, TeacherDashboardActivity::class.java))
                finishAffinity()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }
}
