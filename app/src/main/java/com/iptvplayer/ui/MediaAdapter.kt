package com.iptvplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.R

data class MediaRow(
    val id: String,
    val leading: String,
    val title: String,
    val subtitle: String?,
    val url: String,
    val fallbackUrl: String?,
)

class MediaAdapter(
    private val onClick: (MediaRow) -> Unit,
) : RecyclerView.Adapter<MediaAdapter.Holder>() {

    private val items = ArrayList<MediaRow>()

    fun submit(newItems: List<MediaRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.leading.text = item.leading
        holder.leading.visibility = if (item.leading.isBlank()) View.GONE else View.VISIBLE
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle.orEmpty()
        holder.subtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnFocusChangeListener { _, hasFocus -> holder.applyColors(hasFocus) }
        holder.applyColors(holder.itemView.isFocused)
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val leading: TextView = view.findViewById(R.id.mediaLeading)
        val title: TextView = view.findViewById(R.id.mediaTitle)
        val subtitle: TextView = view.findViewById(R.id.mediaSubtitle)

        fun applyColors(focused: Boolean) {
            val context = itemView.context
            title.setTextColor(
                if (focused) context.resources.getColor(R.color.white)
                else context.resources.getColor(R.color.text)
            )
            leading.setTextColor(
                if (focused) context.resources.getColor(R.color.blue_100)
                else context.resources.getColor(R.color.text_muted)
            )
            subtitle.setTextColor(
                if (focused) context.resources.getColor(R.color.blue_100)
                else context.resources.getColor(R.color.text_muted)
            )
        }
    }
}
