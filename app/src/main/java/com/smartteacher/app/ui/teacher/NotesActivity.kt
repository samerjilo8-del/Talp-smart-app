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
import com.smartteacher.app.backend.model.Note
import com.smartteacher.app.databinding.ActivityListWithFabBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.DialogAddNoteBinding
import com.smartteacher.app.databinding.ItemAssignmentBinding
import com.smartteacher.app.notification.NotificationTrigger
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListWithFabBinding
    private lateinit var content: ContentListBinding
    private lateinit var session: SessionManager
    private val adapter = NoteAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListWithFabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.toolbar.title = getString(R.string.notes)
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

    private fun load() {
        val grade = session.getTeacherGrade() ?: return
        val section = session.getTeacherSection() ?: return
        content.progress.visibility = View.VISIBLE
        content.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching { Repository.getNotesForClass(grade, section) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddDialog() {
        val dlgBinding = DialogAddNoteBinding.inflate(layoutInflater)
        AlertDialog.Builder(this, R.style.Theme_SmartTeacher_Dialog)
            .setTitle(R.string.add)
            .setView(dlgBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dlgBinding.etTitle.text.toString().trim()
                val contentText = dlgBinding.etContent.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton
                val grade = session.getTeacherGrade() ?: return@setPositiveButton
                val section = session.getTeacherSection() ?: return@setPositiveButton
                val teacherId = session.getTeacherId() ?: return@setPositiveButton
                lifecycleScope.launch {
                    runCatching {
                        Repository.addNote(Note(
                            teacher_id = teacherId, grade = grade, section = section,
                            title = title, content = contentText
                        ))
                    }
                    NotificationTrigger.trigger(
                        grade, section, "note",
                        getString(R.string.notif_new_note),
                        title
                    )
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private inner class NoteAdapter : RecyclerView.Adapter<NoteAdapter.VH>() {
        private val items = mutableListOf<Note>()
        fun submit(list: List<Note>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemAssignmentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(n: Note) {
                b.tvTitle.text = n.title
                b.tvSub.text = n.created_at.take(10)
                b.tvBody.text = n.content
                b.btnDelete.setOnClickListener {
                    AlertDialog.Builder(itemView.context, R.style.Theme_SmartTeacher_Dialog)
                        .setTitle(R.string.delete)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                runCatching { Repository.deleteNote(n.id) }
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
