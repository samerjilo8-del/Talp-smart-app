package com.smartteacher.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.ScheduleEntry
import com.smartteacher.app.databinding.ActivityWeeklyScheduleBinding
import com.smartteacher.app.databinding.DialogEditLessonBinding
import com.smartteacher.app.databinding.ItemDayRowBinding
import com.smartteacher.app.databinding.ItemLessonBinding
import com.smartteacher.app.ui.Constants
import kotlinx.coroutines.launch

/**
 * Weekly schedule screen.
 * Days vertically (Sunday-Thursday), 6 lessons horizontally per day.
 * Each day has a distinct soft color. Accessible from both teacher and student.
 */
class WeeklyScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyScheduleBinding
    private lateinit var session: SessionManager
    private val entries = mutableListOf<ScheduleEntry>()
    private val isTeacher by lazy { session.getRole() == SessionManager.Role.TEACHER }

    private val dayColors = intArrayOf(
        R.drawable.bg_day_card_sunday,
        R.drawable.bg_day_card_monday,
        R.drawable.bg_day_card_tuesday,
        R.drawable.bg_day_card_wednesday,
        R.drawable.bg_day_card_thursday
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.weekly_schedule)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.swipeRefresh.setOnRefreshListener { load() }
        binding.fabEdit.visibility = if (isTeacher) View.VISIBLE else View.GONE
        binding.fabEdit.setOnClickListener { }
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun teacherId(): String =
        session.getTeacherId() ?: intent.getStringExtra(Constants.EXTRA_TEACHER_ID) ?: ""

    private fun load() {
        val tid = teacherId()
        if (tid.isEmpty()) {
            binding.swipeRefresh.isRefreshing = false
            return
        }
        lifecycleScope.launch {
            val list = runCatching { Repository.getSchedule(tid) }.getOrDefault(emptyList())
            entries.clear(); entries.addAll(list)
            binding.swipeRefresh.isRefreshing = false
            buildSchedule()
        }
    }

    private fun buildSchedule() {
        val container = binding.scheduleContainer
        container.removeAllViews()
        for (day in 0 until Constants.DAYS.size) {
            val row = ItemDayRowBinding.inflate(LayoutInflater.from(this), container, false)
            row.tvDay.text = Constants.DAYS[day]
            row.root.background = null
            row.tvDay.setBackgroundResource(dayColors[day])
            row.tvDay.setPadding(16, 14, 16, 14)
            row.rvLessons.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            row.rvLessons.adapter = LessonAdapter(day)
            container.addView(row.root)
        }
    }

    private fun entryFor(day: Int, lesson: Int): ScheduleEntry? =
        entries.firstOrNull { it.day == day && it.lesson == lesson }

    private inner class LessonAdapter(private val day: Int) : RecyclerView.Adapter<LessonAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = Constants.LESSONS_PER_DAY
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(pos + 1)
        inner class VH(val b: ItemLessonBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(lesson: Int) {
                b.tvLesson.text = "${getString(R.string.lesson)} $lesson"
                val e = entryFor(day, lesson)
                b.tvSubject.text = e?.subject_name?.ifBlank { "—" } ?: "—"
                b.root.setOnClickListener {
                    if (isTeacher) showEditDialog(day, lesson, e)
                }
            }
        }
    }

    private fun showEditDialog(day: Int, lesson: Int, existing: ScheduleEntry?) {
        val dlg = DialogEditLessonBinding.inflate(LayoutInflater.from(this))
        dlg.tvSlot.text = "${Constants.DAYS[day]} — ${getString(R.string.lesson)} $lesson"
        dlg.etSubject.setText(existing?.subject_name ?: "")
        AlertDialog.Builder(this, R.style.Theme_SmartTeacher_Dialog)
            .setTitle(R.string.edit)
            .setView(dlg.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val subject = dlg.etSubject.text.toString().trim()
                val tid = teacherId()
                val grade = session.getTeacherGrade() ?: intent.getStringExtra(Constants.EXTRA_GRADE) ?: ""
                val section = session.getTeacherSection() ?: intent.getStringExtra(Constants.EXTRA_SECTION) ?: ""
                lifecycleScope.launch {
                    if (subject.isEmpty()) {
                        existing?.id?.let { runCatching { Repository.deleteSchedule(it) } }
                    } else {
                        runCatching {
                            Repository.upsertSchedule(ScheduleEntry(
                                id = existing?.id ?: "",
                                teacher_id = tid, grade = grade, section = section,
                                day = day, lesson = lesson, subject_name = subject
                            ))
                        }
                    }
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
