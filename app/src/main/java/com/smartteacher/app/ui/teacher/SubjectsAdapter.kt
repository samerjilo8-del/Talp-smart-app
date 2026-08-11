package com.smartteacher.app.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartteacher.app.backend.model.Subject

class SubjectsAdapter : ListAdapter<Subject, SubjectsAdapter.VH>(SubjectDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // استخدام android.R.layout.simple_list_item_1 كما طُلِب
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(android.R.id.text1)
        fun bind(subject: Subject) {
            tv.text = subject.name
        }
    }

    class SubjectDiff : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem == newItem
    }
}
