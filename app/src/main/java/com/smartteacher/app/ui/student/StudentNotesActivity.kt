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
import com.smartteacher.app.backend.model.Note
import com.smartteacher.app.databinding.ActivityStudentListBinding
import com.smartteacher.app.databinding.ContentListBinding
import com.smartteacher.app.databinding.ItemAssignmentBinding
import com.smartteacher.app.ui.Constants
import kotlinx.coroutines.launch

class StudentNotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private lateinit var content: ContentListBinding
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = getString(R.string.notes)
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
            val list = runCatching { Repository.getNotesForClass(grade, section) }.getOrDefault(emptyList())
            content.progress.visibility = View.GONE
            content.swipeRefresh.isRefreshing = false
            adapter.submit(list)
            content.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        private val items = mutableListOf<Note>()
        fun submit(list: List<Note>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemAssignmentBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(n: Note) {
                b.btnDelete.visibility = View.GONE
                b.tvTitle.text = n.title
                b.tvSub.text = n.created_at.take(10)
                b.tvBody.text = n.content
            }
        }
    }
}
