package com.privbrowse.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R

class TabAdapter(
    private val tabs: MutableList<BrowserTab>,
    private val onTabSelected: (Int) -> Unit,
    private val onTabClosed: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    var selectedPosition: Int = 0
        set(value) {
            val old = field
            field = value
            if (old in tabs.indices) notifyItemChanged(old)
            if (value in tabs.indices) notifyItemChanged(value)
        }

    class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(R.id.tabTitle)
        val close: android.widget.ImageButton = view.findViewById(R.id.tabClose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = if (tab.isIncognito) "\uD83D\uDD76 ${tab.title}" else tab.title
        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setOnClickListener { onTabSelected(holder.bindingAdapterPosition) }
        holder.close.setOnClickListener { onTabClosed(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = tabs.size
}
