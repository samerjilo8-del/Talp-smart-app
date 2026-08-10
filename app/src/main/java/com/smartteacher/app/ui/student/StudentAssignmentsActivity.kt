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
import com.smartteacher.app.backend.model.Assignment
import com.smartteacher.app.databinding.ActivityStudentListBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.ItemAssignmentBinding
import com.smartteacher.app.ui.Constants
import kotlinx.coroutines.launch

class StudentAssignmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private lateinit var content: ContentListBinding
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = getString(R.string.assignments_homework)
        binding.toolbar.setNavigationOnClickListener { finish() }

        content = ContentListBinding.bind(
            LayoutInflater.from(this).inflate(R.layout.content_list, binding.contentContainer, true)
        )
        content.recyclerView.layoutManager = LinearLayoutManager(this)
        content.recyclerView.adapter = adapter
        content.swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val grade = intent.getStringExtra(Constants.EXTRA_GRADE) ?: return
        val section = intent.getStringExtra(Constants.EXTRA_SECTION) ?: return
        content.progress.visibility = View.VISIBLE
        content.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching { Repository.getAssignmentsForClass(grade, section) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        private val items = mutableListOf<Assignment>()
        fun submit(list: List<Assignment>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemAssignmentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(a: Assignment) {
                b.btnDelete.visibility = View.GONE
                b.tvTitle.text = a.title
                b.tvSub.text = buildString {
                    if (a.subject_name.isNotEmpty()) append(a.subject_name)
                    if (a.due_date.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append(a.due_date)
                    }
                }
                b.tvBody.text = a.content
            }
        }
    }
}
