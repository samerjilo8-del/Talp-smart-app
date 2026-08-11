package com.samerjilo8.smartteacher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val subjects = listOf(
            "رياضيات",
            "فيزياء",
            "كيمياء",
            "علوم",
            "تاريخ",
            "جغرافيا",
            "لغة عربية",
            "لغة إنجليزية"
        )

        val adapter = SubjectAdapter(subjects)
        recyclerView.adapter = adapter
    }
}
