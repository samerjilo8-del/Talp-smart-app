package com.smartteacher.app.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.smartteacher.app.R
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityTeacherDashboardBinding
import com.smartteacher.app.ui.auth.RoleSelectionActivity

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var session: SessionManager

    data class Module(val title: String, val icon: Int, val target: Class<*>)

    private lateinit var modules: List<Module>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        modules = listOf(
            Module(getString(R.string.student_management), R.drawable.ic_students, StudentsManagementActivity::class.java),
            Module(getString(R.string.subject_management), R.drawable.ic_subjects, SubjectsManagementActivity::class.java),
            Module(getString(R.string.assignments_homework), R.drawable.ic_assignments, AssignmentsActivity::class.java),
            Module(getString(R.string.exams), R.drawable.ic_exams, ExamsActivity::class.java),
            Module(getString(R.string.gradebook), R.drawable.ic_gradebook, GradebookActivity::class.java),
            Module(getString(R.string.notes), R.drawable.ic_notes, NotesActivity::class.java),
            Module(getString(R.string.weekly_schedule), R.drawable.ic_schedule, WeeklyScheduleActivity::class.java)
        )

        val grade = session.getTeacherGrade() ?: ""
        val section = session.getTeacherSection() ?: ""
        binding.tvClassInfo.text = "${session.getTeacherName()}\n$grade - $section"

        buildGrid()
        binding.btnLogout.setOnClickListener {
            session.logout()
            val intent = Intent(this, RoleSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun buildGrid() {
        val grid = binding.gridModules
        grid.removeAllViews()
        modules.forEach { module ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_dashboard_module, grid, false) as com.google.android.material.card.MaterialCardView
            val params = card.layoutParams as GridLayout.LayoutParams
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            card.layoutParams = params

            card.findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(module.icon)
            card.findViewById<android.widget.TextView>(R.id.tvTitle).text = module.title

            card.setOnClickListener {
                startActivity(Intent(this, module.target))
            }
            grid.addView(card)
        }
    }
}
