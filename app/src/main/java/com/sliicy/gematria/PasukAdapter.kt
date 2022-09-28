package com.sliicy.gematria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class PasukAdapter(private val mList: List<PasukModel>) : RecyclerView.Adapter<PasukAdapter.ViewHolder>() {

    var onItemClick : ((PasukModel) -> Unit)? = null
    var onItemLongClick : ((PasukModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)
        return ViewHolder(view)
    }

    // Binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pasuk = mList[position]
        holder.textView.text = pasuk.text
        holder.textView.setOnClickListener {
            onItemClick?.invoke(pasuk)
        }
        holder.textView.setOnLongClickListener {
            onItemLongClick?.invoke(pasuk)
            return@setOnLongClickListener true
        }
    }

    // Return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        init {
            itemView.setOnClickListener {
            }
        }
    }
}
