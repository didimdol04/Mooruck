package com.example.mooruckapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mooruckapp.databinding.ActivityHomeBinding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.snackbar.Snackbar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlantList()
        setupButtons()
    }

    private fun setupPlantList() {
        binding.recyclerViewPlants.layoutManager = GridLayoutManager(this, 2)

        // DB 연결 전 화면과 검색 기능 확인용 임시 데이터
        val samplePlants = listOf(
            HomePlantItem(
                id = 1L,
                plantName = "몬스테라",
                nickname = "몬몬이",
                profileImageUri = null,
                wateringMessage = "오늘 물을 주세요"
            ),
            HomePlantItem(
                id = 2L,
                plantName = "스투키",
                nickname = "쑥쑥이",
                profileImageUri = null,
                wateringMessage = "3일 뒤 물주기"
            ),
            HomePlantItem(
                id = 3L,
                plantName = "산세베리아",
                nickname = null,
                profileImageUri = null,
                wateringMessage = "물 준 지 2일째"
            )
        )

        val plantAdapter = HomePlantAdapter(
            plants = samplePlants,
            onPlantClick = { plant ->
                // TODO: 식물 상세 Activity 연결
            },
            onWaterClick = { plant ->
                val displayName = plant.nickname
                    ?.takeIf { it.isNotBlank() }
                    ?: plant.plantName

                Snackbar.make(
                    binding.root,
                    "${displayName}의 물주기 날짜를 오늘로 재설정합니다.",
                    Snackbar.LENGTH_SHORT
                ).show()

                // TODO: WateringRecord DB 저장 연결
            }
        )

        binding.recyclerViewPlants.adapter = plantAdapter

        binding.editTextSearch.addTextChangedListener { text ->
            val keyword = text?.toString()?.trim().orEmpty()

            val filteredPlants = if (keyword.isBlank()) {
                samplePlants
            } else {
                samplePlants.filter { plant ->
                    plant.plantName.contains(
                        other = keyword,
                        ignoreCase = true
                    ) ||
                            plant.nickname?.contains(
                                other = keyword,
                                ignoreCase = true
                            ) == true
                }
            }

            plantAdapter.updatePlants(filteredPlants)
        }
    }

    private fun setupButtons() {
        binding.buttonAddPlant.setOnClickListener {
            // TODO: 식물 등록 Activity가 만들어진 뒤 연결
        }

        binding.buttonDiary.setOnClickListener {
            // TODO: 성장 일지 Activity가 만들어진 뒤 연결
        }

        binding.buttonMyPage.setOnClickListener {
            // TODO: 마이페이지 Activity가 만들어진 뒤 연결
        }
    }
}