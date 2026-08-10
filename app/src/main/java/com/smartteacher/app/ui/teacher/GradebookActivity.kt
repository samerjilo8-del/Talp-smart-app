package com.smartteacher.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.Grade
import com.smartteacher.app.backend.model.Student
import com.smartteacher.app.backend.model.Subject
import com.smartteacher.app.databinding.ActivityGradebookBinding
import com.smartteacher.app.databinding.ItemGradeBinding
import kotlinx.coroutines.launch

class GradebookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGradebookBinding
    private lateinit var session: SessionManager
    private val gradeAdapter = GradeAdapter()
    private val subjects = mutableListOf<Subject>()
    private val students = mutableListOf<Student>()
    private val existingGrades = mutableMapOf<String, Grade>() // studentId -> Grade
    private var currentTerm = 1
    private var selectedSubject: Subject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradebookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.gradebook)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabTerms.addTab(binding.tabTerms.newTab().setText(R.string.term1))
        binding.tabTerms.addTab(binding.tabTerms.newTab().setText(R.string.term2))
        binding.tabTerms.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTerm = tab.position + 1
                refreshForSubject()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.etSubject.setOnItemClickListener { _, _, position, _ ->
            selectedSubject = subjects.getOrNull(position)
            refreshForSubject()
        }

        loadInitial()
    }

    private fun loadInitial() {
        val teacherId = session.getTeacherId() ?: return
        val grade = session.getTeacherGrade() ?: return
        val section = session.getTeacherSection() ?: return
        lifecycleScope.launch {
            val subs = runCatching { Repository.getSubjects(teacherId) }.getOrDefault(emptyList())
            val studs = runCatching { Repository.getStudentsByClass(grade, section) }.getOrDefault(emptyList())
            subjects.clear(); subjects.addAll(subs)
            students.clear(); students.addAll(studs)
            binding.etSubject.setAdapter(
                ArrayAdapter(this@GradebookActivity, android.R.layout.simple_list_item_1, subs.map { it.name })
            )
            if (subs.isNotEmpty()) {
                binding.etSubject.setText(subs[0].name, false)
                selectedSubject = subs[0]
                refreshForSubject()
            }
        }
    }

    private fun refreshForSubject() {
        val subject = selectedSubject ?: return
        // Hide Term 2 tab if subject uses only one term
        binding.tabTerms.getTabAt(1)?.view?.visibility = if (subject.terms == 2) View.VISIBLE else View.GONE
        if (subject.terms == 1 && currentTerm == 2) {
            binding.tabTerms.getTabAt(0)?.select()
            currentTerm = 1
        }
        lifecycleScope.launch {
            val grades = runCatching { Repository.getGradesForSubject(subject.id, currentTerm) }.getOrDefault(emptyList())
            existingGrades.clear()
            grades.forEach { existingGrades[it.student_id] = it }
            setupList()
        }
    }

    private fun setupList() {
        val rv = binding.contentContainer.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = gradeAdapter
        gradeAdapter.submit(students.toList())
    }

    private inner class GradeAdapter : RecyclerView.Adapter<GradeAdapter.VH>() {
        private val items = mutableListOf<Student>()
        fun submit(list: List<Student>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemGradeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemGradeBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(s: Student) {
                b.tvName.text = s.name
                val existing = existingGrades[s.id]
                b.etScore.setText(existing?.score?.toString() ?: "")
                b.btnSave.setOnClickListener {
                    val scoreText = b.etScore.text.toString().trim()
                    val score = scoreText.toDoubleOrNull() ?: return@setOnClickListener
                    val subject = selectedSubject ?: return@setOnClickListener
                    val teacherId = session.getTeacherId() ?: return@setOnClickListener
                    lifecycleScope.launch {
                        runCatching {
                            Repository.upsertGrade(Grade(
                                id = existing?.id ?: "",
                                teacher_id = teacherId,
                                student_id = s.id,
                                subject_id = subject.id,
                                subject_name = subject.name,
                                term = currentTerm,
                                score = score,
                                max_score = existing?.max_score ?: 100.0
                            ))
                        }
                        refreshForSubject()
                        android.widget.Toast.makeText(itemView.context, "تم حفظ الدرجة", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
