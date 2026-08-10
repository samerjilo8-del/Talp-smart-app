package com.smartteacher.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.model.Grade
import com.smartteacher.app.databinding.ActivityStudentGradesBinding
import com.smartteacher.app.databinding.ItemStudentGradeBinding
import com.smartteacher.app.ui.Constants
import kotlinx.coroutines.launch

class StudentGradesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentGradesBinding
    private val adapter = GradeAdapter()
    private val allGrades = mutableListOf<Grade>()
    private var currentTerm = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentGradesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = getString(R.string.gradebook)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabTerms.addTab(binding.tabTerms.newTab().setText(R.string.term1))
        binding.tabTerms.addTab(binding.tabTerms.newTab().setText(R.string.term2))
        binding.tabTerms.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTerm = tab.position + 1
                applyFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val studentId = intent.getStringExtra(Constants.EXTRA_STUDENT_ID) ?: return
        lifecycleScope.launch {
            val list = runCatching { Repository.getGradesForStudent(studentId) }.getOrDefault(emptyList())
            allGrades.clear(); allGrades.addAll(list)
            binding.swipeRefresh.isRefreshing = false
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = allGrades.filter { it.term == currentTerm }
        adapter.submit(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class GradeAdapter : RecyclerView.Adapter<GradeAdapter.VH>() {
        private val items = mutableListOf<Grade>()
        fun submit(list: List<Grade>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemStudentGradeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
        inner class VH(val b: ItemStudentGradeBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(g: Grade) {
                b.tvSubject.text = g.subject_name
                b.tvScore.text = "%.1f / %.0f".format(g.score, g.max_score)
            }
        }
    }
}
