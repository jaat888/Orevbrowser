package com.privbrowse.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R
import com.privbrowse.app.data.LinkItem

class LinkAdapter(
    private val items: List<LinkItem>,
    private val onClick: (LinkItem) -> Unit,
    private val onLongClick: ((LinkItem) -> Unit)? = null
) : RecyclerView.Adapter<LinkAdapter.LinkViewHolder>() {

    class LinkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val url: TextView = view.findViewById(R.id.url)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title.ifBlank { item.url }
        holder.url.text = item.url
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(item)
            onLongClick != null
        }
    }

    override fun getItemCount(): Int = items.size
}
