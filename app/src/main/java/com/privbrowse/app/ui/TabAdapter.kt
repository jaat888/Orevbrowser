package com.privbrowse.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R

class TabAdapter(
    private val tabs: MutableList<BrowserTab>,
    private val onTabSelected: (Int) -> Unit,
    private val onTabClosed: (Int) -> Unit,
    private val isVaultUnlocked: () -> Boolean = { true }
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
        val vertical = (parent as? RecyclerView)?.layoutManager is androidx.recyclerview.widget.LinearLayoutManager &&
            ((parent as? RecyclerView)?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.orientation == RecyclerView.VERTICAL
        view.setTag(R.id.tab_vertical_layout, vertical)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        val hiddenVault = tab.isVault && !isVaultUnlocked()
        if (hiddenVault) {
            holder.title.text = ""
            holder.close.visibility = View.GONE
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
                width = 0
                height = 0
            }
            holder.itemView.isClickable = false
            return
        }
        holder.itemView.visibility = View.VISIBLE
        val vertical = holder.itemView.getTag(R.id.tab_vertical_layout) == true
        holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
            if (vertical) {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = dp(parent = holder.itemView, value = 52)
            } else {
                width = dp(parent = holder.itemView, value = 150)
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        holder.close.visibility = View.VISIBLE
        holder.title.text = if (tab.isVault) "🔒 ${tab.title}" else if (tab.isIncognito) "Private · ${tab.title}" else tab.title
        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setBackgroundResource(R.drawable.tab_item_background)
        if (tab.groupColor != 0) holder.itemView.setBackgroundColor(tab.groupColor)
        holder.itemView.setOnClickListener { onTabSelected(holder.bindingAdapterPosition) }
        holder.close.setOnClickListener { onTabClosed(holder.bindingAdapterPosition) }
    }

    private fun dp(parent: View, value: Int) = (value * parent.resources.displayMetrics.density).toInt()

    override fun getItemCount(): Int = tabs.size
}
