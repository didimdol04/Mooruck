package com.example.mooruckapp.repository

import com.example.mooruckapp.network.RetrofitClient

class PlantRepository {

    // 식물 검색
    suspend fun searchPlants(
        apiKey: String,
        keyword: String
    ) = RetrofitClient.plantApi.searchPlants(
        apiKey = apiKey,
        keyword = keyword
    )

    // 식물 상세 조회
    suspend fun getPlantDetail(
        apiKey: String,
        contentNo: String
    ) = RetrofitClient.plantApi.getPlantDetail(
        apiKey = apiKey,
        contentNo = contentNo
    )
}