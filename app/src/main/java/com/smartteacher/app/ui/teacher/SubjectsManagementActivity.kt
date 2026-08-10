package com.smartteacher.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.Subject
import com.smartteacher.app.databinding.ActivityListWithFabBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.DialogAddSubjectBinding
import com.smartteacher.app.databinding.ItemStudentBinding
import kotlinx.coroutines.launch

class SubjectsManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListWithFabBinding
    private lateinit var content: ContentListBinding
    private lateinit var session: SessionManager
    private val adapter = SubjectAdapter()
    private val subjects = mutableListOf<Subject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListWithFabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.subject_management)
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

    fun getSubjectsList(): List<Subject> = subjects

    private fun load() {
        val teacherId = session.getTeacherId() ?: return
        content.progress.visibility = View.VISIBLE
        content.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching { Repository.getSubjects(teacherId) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            subjects.clear(); subjects.addAll(list)
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddDialog() {
        val dlgBinding = DialogAddSubjectBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.Theme_SmartTeacher_Dialog)
            .setTitle(R.string.add)
            .setView(dlgBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dlgBinding.etName.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val terms = if (dlgBinding.rbTwo.isChecked) 2 else 1
                val teacherId = session.getTeacherId() ?: return@setPositiveButton
                val grade = session.getTeacherGrade() ?: return@setPositiveButton
                val section = session.getTeacherSection() ?: return@setPositiveButton
                lifecycleScope.launch {
                    runCatching {
                        Repository.addSubject(Subject(
                            teacher_id = teacherId, grade = grade, section = section,
                            name = name, terms = terms
                        ))
                    }
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
    }

    private inner class SubjectAdapter : RecyclerView.Adapter<SubjectAdapter.VH>() {
        private val items = mutableListOf<Subject>()
        fun submit(list: List<Subject>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemStudentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(s: Subject) {
                b.tvName.text = s.name
                b.tvSub.text = if (s.terms == 2) "فصلان (الفصل الأول / الفصل الثاني)" else "فصل واحد"
                b.btnDelete.setOnClickListener {
                    AlertDialog.Builder(itemView.context, R.style.Theme_SmartTeacher_Dialog)
                        .setTitle(R.string.delete)
                        .setMessage("حذف المادة ${s.name}؟")
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                runCatching { Repository.deleteSubject(s.id) }
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
