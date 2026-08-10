package com.smartteacher.app.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.Exam
import com.smartteacher.app.backend.model.Subject
import com.smartteacher.app.databinding.ActivityListWithFabBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.DialogAddExamBinding
import com.smartteacher.app.databinding.ItemAssignmentBinding
import com.smartteacher.app.notification.NotificationTrigger
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListWithFabBinding
    private lateinit var content: ContentListBinding
    private lateinit var session: SessionManager
    private val adapter = ExamAdapter()
    private val subjects = mutableListOf<Subject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListWithFabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.exams)
        binding.toolbar.setNavigationOnClickListener { finish() }

        content = ContentListBinding.bind(
            LayoutInflater.from(this).inflate(R.layout.content_list, binding.contentContainer, true)
        )
        content.recyclerView.layoutManager = LinearLayoutManager(this)
        content.recyclerView.adapter = adapter

        content.swipeRefresh.setOnRefreshListener { load() }
        binding.fabAdd.setOnClickListener { showAddDialog() }
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun loadSubjects(onLoaded: () -> Unit) {
        val teacherId = session.getTeacherId() ?: return
        lifecycleScope.launch {
            val list = runCatching { Repository.getSubjects(teacherId) }.getOrDefault(emptyList())
            subjects.clear(); subjects.addAll(list)
            onLoaded()
        }
    }

    private fun load() {
        val grade = session.getTeacherGrade() ?: return
        val section = session.getTeacherSection() ?: return
        content.progress.visibility = View.VISIBLE
        content.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching { Repository.getExamsForClass(grade, section) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        loadSubjects { }
    }

    private fun showAddDialog() {
        loadSubjects {
            val dlgBinding = DialogAddExamBinding.inflate(layoutInflater)
            val subjectNames = subjects.map { it.name }
            if (subjectNames.isNotEmpty()) {
                dlgBinding.etSubject.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, subjectNames)
                )
            }
            val terms = listOf(getString(R.string.term1), getString(R.string.term2))
            dlgBinding.etTerm.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_list_item_1, terms)
            )
            dlgBinding.etTerm.setText(terms[0], false)
            dlgBinding.etDate.setOnClickListener { pickDate(dlgBinding) }

            AlertDialog.Builder(this, R.style.Theme_SmartTeacher_Dialog)
                .setTitle(R.string.add)
                .setView(dlgBinding.root)
                .setPositiveButton(R.string.save) { _, _ ->
                    val title = dlgBinding.etTitle.text.toString().trim()
                    val subjectName = dlgBinding.etSubject.text.toString().trim()
                    val termText = dlgBinding.etTerm.text.toString().trim()
                    val date = dlgBinding.etDate.text.toString().trim()
                    val notes = dlgBinding.etNotes.text.toString().trim()
                    if (title.isEmpty()) return@setPositiveButton
                    val term = if (termText == terms[1]) 2 else 1
                    val grade = session.getTeacherGrade() ?: return@setPositiveButton
                    val section = session.getTeacherSection() ?: return@setPositiveButton
                    val teacherId = session.getTeacherId() ?: return@setPositiveButton
                    val subjectId = subjects.firstOrNull { it.name == subjectName }?.id
                    lifecycleScope.launch {
                        runCatching {
                            Repository.addExam(Exam(
                                teacher_id = teacherId, grade = grade, section = section,
                                subject_id = subjectId, subject_name = subjectName,
                                title = title, term = term, exam_date = date, notes = notes
                            ))
                        }
                        NotificationTrigger.trigger(
                            grade, section, "exam",
                            getString(R.string.notif_new_exam),
                            "$title${if (subjectName.isNotEmpty()) " - $subjectName" else ""}"
                        )
                        load()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun pickDate(d: DialogAddExamBinding) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, day ->
            d.etDate.setText("%04d-%02d-%02d".format(y, m + 1, day))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private inner class ExamAdapter : RecyclerView.Adapter<ExamAdapter.VH>() {
        private val items = mutableListOf<Exam>()
        fun submit(list: List<Exam>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemAssignmentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(e: Exam) {
                b.tvTitle.text = e.title
                val termStr = if (e.term == 2) getString(R.string.term2) else getString(R.string.term1)
                b.tvSub.text = buildString {
                    if (e.subject_name.isNotEmpty()) append(e.subject_name)
                    if (isNotEmpty()) append(" | ")
                    append(termStr)
                    if (e.exam_date.isNotEmpty()) append(" | ").append(e.exam_date)
                }
                b.tvBody.text = e.notes
                b.btnDelete.setOnClickListener {
                    AlertDialog.Builder(itemView.context, R.style.Theme_SmartTeacher_Dialog)
                        .setTitle(R.string.delete)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                runCatching { Repository.deleteExam(e.id) }
                                load()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }
    }
}
