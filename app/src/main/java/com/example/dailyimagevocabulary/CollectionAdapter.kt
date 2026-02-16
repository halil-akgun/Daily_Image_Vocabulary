package com.example.dailyimagevocabulary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CollectionAdapter(
    private val list: MutableList<Pair<CollectionEntity, Int>>,
    private val onClick: (CollectionEntity) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textCollectionName)
        val count: TextView = view.findViewById(R.id.textCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (collection, count) = list[position]

        holder.name.text = collection.name
        holder.count.text = "$count items"

        holder.itemView.setOnClickListener {
            onClick(collection)
        }
    }
}
