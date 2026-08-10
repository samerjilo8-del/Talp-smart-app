package com.smartteacher.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.ScheduleEntry
import com.smartteacher.app.databinding.ActivityStudentListBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.ItemDayRowBinding
import com.smartteacher.app.databinding.ItemLessonBinding
import com.smartteacher.app.ui.Constants
import kotlinx.coroutines.launch

/** Student weekly schedule (read-only, same layout/colors as teacher). */
class StudentScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private lateinit var content: ContentListBinding
    private lateinit var session: SessionManager
    private val entries = mutableListOf<ScheduleEntry>()

    private val dayColors = intArrayOf(
        R.drawable.bg_day_card_sunday,
        R.drawable.bg_day_card_monday,
        R.drawable.bg_day_card_tuesday,
        R.drawable.bg_day_card_wednesday,
        R.drawable.bg_day_card_thursday
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.weekly_schedule)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // We need a custom scroll container for the schedule grid.
        content = ContentListBinding.bind(
            LayoutInflater.from(this).inflate(R.layout.content_list, binding.contentContainer, true)
        )
        content.recyclerView.visibility = View.GONE
        content.swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val teacherId = session.getTeacherId() ?: ""
        // Students identify the schedule by their teacher. Since students are
        // linked to a grade+section that belongs to the teacher, we look up the
        // schedule via the teacher that matches their grade/section. For
        // simplicity, the schedule is keyed by teacher_id stored in session is
        // not available to students; instead we load by grade/section through
        // the student's teacher assignment.
        val grade = intent.getStringExtra(Constants.EXTRA_GRADE) ?: session.getStudentGrade()
        val section = intent.getStringExtra(Constants.EXTRA_SECTION) ?: session.getStudentSection()
        lifecycleScope.launch {
            // Fetch the student to obtain the teacher context: schedule_entries
            // are filtered by teacher_id. We resolve the teacher from the same
            // grade/section via the subjects table which stores teacher_id.
            val tid = resolveTeacherId(grade, section)
            val list = if (tid.isNotEmpty())
                runCatching { Repository.getSchedule(tid) }.getOrDefault(emptyList())
            else emptyList()
            entries.clear(); entries.addAll(list)
            content.swipeRefresh.isRefreshing = false
            buildSchedule()
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private suspend fun resolveTeacherId(grade: String?, section: String?): String {
        if (grade == null || section == null) return ""
        // Subjects carry teacher_id + grade + section; use the first match.
        val client = com.smartteacher.app.backend.SupabaseConfig.get().postgrest
        val rows: List<com.smartteacher.app.backend.model.Subject> = runCatching {
            client.from("subjects").select {
                filter {
                    eq("grade", grade)
                    eq("section", section)
                }
            }.decodeList()
        }.getOrDefault(emptyList())
        return rows.firstOrNull()?.teacher_id ?: ""
    }

    private fun buildSchedule() {
        val container = content.recyclerView.parent as ViewGroup
        // Remove previous day rows
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child.id != content.swipeRefresh.id) container.removeView(child)
        }
        for (day in 0 until Constants.DAYS.size) {
            val row = ItemDayRowBinding.inflate(LayoutInflater.from(this), container, false)
            row.tvDay.text = Constants.DAYS[day]
            row.tvDay.setBackgroundResource(dayColors[day])
            row.tvDay.setPadding(16, 14, 16, 14)
            row.rvLessons.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            row.rvLessons.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                        ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
                    ) {}
                override fun getItemCount() = Constants.LESSONS_PER_DAY
                override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                    val lesson = pos + 1
                    val lb = ItemLessonBinding.bind(h.itemView)
                    lb.tvLesson.text = "${getString(R.string.lesson)} $lesson"
                    val e = entries.firstOrNull { it.day == day && it.lesson == lesson }
                    lb.tvSubject.text = e?.subject_name?.ifBlank { "—" } ?: "—"
                }
            }
            container.addView(row.root)
        }
    }
}
