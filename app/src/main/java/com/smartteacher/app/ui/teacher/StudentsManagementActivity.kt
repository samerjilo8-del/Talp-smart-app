package com.smartteacher.app.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.Student
import com.smartteacher.app.databinding.ActivityListWithFabBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.DialogAddStudentBinding
import com.smartteacher.app.databinding.ItemStudentBinding
import kotlinx.coroutines.launch

class StudentsManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListWithFabBinding
    private lateinit var content: ContentListBinding
    private lateinit var session: SessionManager
    private val adapter = StudentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListWithFabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.student_management)
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

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val grade = session.getTeacherGrade() ?: return
        val section = session.getTeacherSection() ?: return
        content.progress.visibility = View.VISIBLE
        content.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching { Repository.getStudentsByClass(grade, section) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddDialog() {
        val dlgBinding = DialogAddStudentBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.Theme_SmartTeacher_Dialog)
            .setTitle(R.string.add)
            .setView(dlgBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dlgBinding.etName.text.toString().trim()
                val code = dlgBinding.etCode.text.toString().trim()
                if (name.isEmpty() || code.length != 4) {
                    android.widget.Toast.makeText(this, "أدخل الاسم ورمزاً من 4 أرقام", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val grade = session.getTeacherGrade() ?: return@setPositiveButton
                val section = session.getTeacherSection() ?: return@setPositiveButton
                lifecycleScope.launch {
                    runCatching {
                        Repository.addStudent(Student(
                            study_code = code, name = name,
                            grade = grade, section = section
                        ))
                    }
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
    }

    private inner class StudentAdapter : RecyclerView.Adapter<StudentAdapter.VH>() {
        private val items = mutableListOf<Student>()
        fun submit(list: List<Student>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(val b: ItemStudentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(s: Student) {
                b.tvName.text = s.name
                b.tvSub.text = "الرمز: ${s.study_code} | ${s.grade} - ${s.section}"
                b.btnDelete.setOnClickListener {
                    AlertDialog.Builder(itemView.context, R.style.Theme_SmartTeacher_Dialog)
                        .setTitle(R.string.delete)
                        .setMessage("حذف الطالب ${s.name}؟")
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                runCatching { Repository.deleteStudent(s.id) }
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
