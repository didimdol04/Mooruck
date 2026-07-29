package com.example.mooruckapp.ui.plant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.databinding.ItemPlantSearchBinding
import com.example.mooruckapp.network.dto.PlantItem

class PlantSearchAdapter(
    private val onClick: (PlantItem) -> Unit
) : RecyclerView.Adapter<PlantSearchAdapter.ViewHolder>() {

    private val items = mutableListOf<PlantItem>()

    inner class ViewHolder(
        private val binding: ItemPlantSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlantItem) {
            binding.tvPlantName.text = item.title

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemPlantSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun submitList(list: List<PlantItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}