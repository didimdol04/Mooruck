package com.example.mooruckapp.network.api

import retrofit2.http.GET
import retrofit2.http.Query

interface PlantApiService {

    // 식물 검색
    @GET("service/garden/gardenList")
    suspend fun searchPlants(
        @Query("apiKey") apiKey: String,
        @Query("sType") searchType: String,
        @Query("sText") keyword: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 20
    ): String

    // 식물 상세 조회
    @GET("service/garden/gardenDtl")
    suspend fun getPlantDetail(
        @Query("apiKey") apiKey: String,
        @Query("cntntsNo") contentNo: String
    ): String
}