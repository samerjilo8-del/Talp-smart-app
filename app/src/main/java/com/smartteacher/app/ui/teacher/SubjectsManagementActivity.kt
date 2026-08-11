package com.smartteacher.app.ui.teacher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartteacher.app.R
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.databinding.ActivitySubjectsManagementBinding
import kotlinx.coroutines.launch

class SubjectsManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectsManagementBinding
    private val adapter = SubjectsAdapter()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use DataBindingUtil as requested
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subjects_management)

        session = SessionManager(this)

        binding.recyclerViewSubjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSubjects.adapter = adapter

        // Load subjects for the teacher
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
            // Adapter uses ListAdapter; submit the list
            adapter.submitList(list)
        }
    }
}
