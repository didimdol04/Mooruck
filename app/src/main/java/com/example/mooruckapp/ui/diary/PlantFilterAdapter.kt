package com.example.mooruckapp.ui.diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.entity.UserPlant

class PlantFilterAdapter(
    private var plants: List<UserPlant>,
    private val onPlantClick: (UserPlant) -> Unit
) : RecyclerView.Adapter<PlantFilterAdapter.PlantViewHolder>() {

    // 현재 선택된 식물 위치
    private var selectedPosition = 0

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumb: ImageView = itemView.findViewById(R.id.ivPlantThumb)
        val tvLabel: TextView = itemView.findViewById(R.id.tvPlantLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_filter, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        // 이름은 별명 우선, 없으면 식물명
        holder.tvLabel.text = plant.nickname ?: plant.plantName

        // 사진
        if (!plant.profileImageUri.isNullOrEmpty()) {
            holder.ivThumb.setImageURI(android.net.Uri.parse(plant.profileImageUri))
        } else {
            holder.ivThumb.setImageURI(null)
        }

        // 선택된 식물은 이름을 초록색·굵게 강조
        if (position == selectedPosition) {
            holder.tvLabel.setTextColor(0xFF2E6B4A.toInt())
        } else {
            holder.tvLabel.setTextColor(0xFF757575.toInt())
        }

        // 클릭하면 선택 변경
        holder.itemView.setOnClickListener {
            val current = holder.adapterPosition
            if (current == RecyclerView.NO_POSITION) return@setOnClickListener
            val prev = selectedPosition
            selectedPosition = current
            notifyItemChanged(prev)
            notifyItemChanged(current)
            onPlantClick(plants[current])
        }
    }

    override fun getItemCount(): Int = plants.size

    // 목록 갱신
    fun updateList(newPlants: List<UserPlant>) {
        plants = newPlants
        notifyDataSetChanged()
    }
}