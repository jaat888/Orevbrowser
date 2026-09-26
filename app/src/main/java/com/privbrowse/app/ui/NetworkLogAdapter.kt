package com.privbrowse.app.ui

import android.content.res.ColorStateList
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R
import com.privbrowse.app.data.NetworkLogEntry

class NetworkLogAdapter(
    private val items: List<NetworkLogEntry>
) : RecyclerView.Adapter<NetworkLogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusDot: View = view.findViewById(R.id.statusDot)
        val host: TextView = view.findViewById(R.id.host)
        val detail: TextView = view.findViewById(R.id.detail)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_network_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.host.text = item.host

        val colorRes = when {
            item.blocked && item.category == "fingerprint" -> R.color.grade_f
            item.blocked -> R.color.grade_d
            else -> R.color.grade_a
        }
        holder.statusDot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        holder.detail.text = when {
            item.blocked && item.category == "fingerprint" -> context.getString(R.string.log_status_fingerprint)
            item.blocked -> context.getString(R.string.log_status_tracker)
            else -> context.getString(R.string.log_status_allowed, item.pageOrigin)
        }
        holder.timestamp.text = DateFormat.format("MMM d, HH:mm", item.timestamp)
    }

    override fun getItemCount(): Int = items.size
}
