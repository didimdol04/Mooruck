package com.example.mooruckapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.databinding.ItemHomePlantBinding

class HomePlantAdapter(
    private var plants: List<HomePlantItem>,
    private val onPlantClick: (HomePlantItem) -> Unit,
    private val onWaterClick: (HomePlantItem) -> Unit
) : RecyclerView.Adapter<HomePlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(
        private val binding: ItemHomePlantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plant: HomePlantItem) {

            // 별명이 있으면 큰 글씨에 별명 표시,
            // 없으면 품종명을 표시
            binding.textPlantName.text =
                plant.nickname?.takeIf { it.isNotBlank() }
                    ?: plant.plantName

            binding.textWateringStatus.text = plant.wateringMessage

            // 별명이 있을 때만 아래에 품종명을 표시
            if (plant.nickname.isNullOrBlank()) {
                binding.textNickname.visibility = View.GONE
            } else {
                binding.textNickname.visibility = View.VISIBLE
                binding.textNickname.text = plant.plantName
            }

            if (plant.profileImageUri.isNullOrBlank()) {
                binding.imagePlant.setImageResource(
                    R.drawable.ic_plant_placeholder
                )
            } else {
                runCatching {
                    binding.imagePlant.setImageURI(
                        Uri.parse(plant.profileImageUri)
                    )
                }.onFailure {
                    binding.imagePlant.setImageResource(
                        R.drawable.ic_plant_placeholder
                    )
                }
            }

            binding.cardPlant.setOnClickListener {
                onPlantClick(plant)
            }

            binding.buttonWater.setOnClickListener {
                onWaterClick(plant)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlantViewHolder {
        val binding = ItemHomePlantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PlantViewHolder,
        position: Int
    ) {
        holder.bind(plants[position])
    }

    override fun getItemCount(): Int = plants.size

    fun updatePlants(newPlants: List<HomePlantItem>) {
        plants = newPlants
        notifyDataSetChanged()
    }
}