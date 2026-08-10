package com.smartteacher.app.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.smartteacher.app.R
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivityStudentDashboardBinding
import com.smartteacher.app.notification.NotificationPermissionHelper
import com.smartteacher.app.ui.Constants
import com.smartteacher.app.ui.auth.RoleSelectionActivity

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDashboardBinding
    private lateinit var session: SessionManager

    data class Module(val title: String, val icon: Int, val target: Class<*>)

    private lateinit var modules: List<Module>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationPermissionHelper.register(this)
        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        modules = listOf(
            Module(getString(R.string.assignments_homework), R.drawable.ic_assignments, StudentAssignmentsActivity::class.java),
            Module(getString(R.string.weekly_schedule), R.drawable.ic_schedule, StudentScheduleActivity::class.java),
            Module(getString(R.string.exams), R.drawable.ic_exams, StudentExamsActivity::class.java),
            Module(getString(R.string.gradebook), R.drawable.ic_gradebook, StudentGradesActivity::class.java),
            Module(getString(R.string.notes), R.drawable.ic_notes, StudentNotesActivity::class.java)
        )

        binding.toolbar.title = getString(R.string.student_dashboard)
        binding.tvWelcome.text = getString(R.string.welcome_student, session.getStudentName())

        buildGrid()
        NotificationPermissionHelper.request(this)

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
                val intent = Intent(this, module.target)
                intent.putExtra(Constants.EXTRA_GRADE, session.getStudentGrade())
                intent.putExtra(Constants.EXTRA_SECTION, session.getStudentSection())
                intent.putExtra(Constants.EXTRA_STUDENT_ID, session.getStudentId())
                startActivity(intent)
            }
            grid.addView(card)
        }
    }
}
