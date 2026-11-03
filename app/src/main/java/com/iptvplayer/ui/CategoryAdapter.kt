package com.iptvplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.R
import com.iptvplayer.model.Category

class CategoryAdapter(
    private val onClick: (Category) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.Holder>() {

    private val items = ArrayList<Category>()

    var selectedId: String? = null
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    fun submit(newItems: List<Category>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false) as TextView
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val category = items[position]
        holder.view.text = category.name
        holder.view.isSelected = category.id == selectedId
        holder.view.setOnClickListener { onClick(category) }
    }

    class Holder(val view: TextView) : RecyclerView.ViewHolder(view)
}
