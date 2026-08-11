package com.smartteacher.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.backend.model.Subject
import kotlinx.coroutines.launch

/**
 * Activity to display and manage subjects.
 * Expects layout: res/layout/activity_subjects_management.xml with RecyclerView id = recyclerViewSubjects
 * Item layout: res/layout/item_subject.xml with TextView id = tvSubjectName
 */
class SubjectsManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val adapter = SubjectAdapter()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects_management)

        session = SessionManager(this)

        recyclerView = findViewById(R.id.recyclerViewSubjects)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Load subjects for the teacher (no UI progress shown here; keep it simple)
        loadSubjects()
    }

    override fun onResume() {
        super.onResume()
        loadSubjects()
    }

    private fun loadSubjects() {
        val teacherId = session.getTeacherId() ?: return
        lifecycleScope.launch {
            val list = runCatching { Repository.getSubjects(teacherId) }.getOrDefault(emptyList())
            adapter.submit(list)
        }
    }
}

/**
 * RecyclerView adapter for Subject items.
 * Implemented outside the Activity as requested.
 */
class SubjectAdapter : RecyclerView.Adapter<SubjectAdapter.VH>() {
    private val items = mutableListOf<Subject>()

    fun submit(list: List<Subject>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSubjectName)
        fun bind(s: Subject) {
            tvName.text = s.name
        }
    }
}
