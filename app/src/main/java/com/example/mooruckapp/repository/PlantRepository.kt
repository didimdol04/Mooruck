package com.example.mooruckapp.repository

import com.example.mooruckapp.BuildConfig
import com.example.mooruckapp.network.RetrofitClient
import com.example.mooruckapp.network.dto.PlantDetail
import com.example.mooruckapp.network.dto.PlantSearchResult
import com.example.mooruckapp.network.parser.PlantDetailXmlParser
import com.example.mooruckapp.network.parser.PlantXmlParser

class PlantRepository {

    suspend fun searchPlants(
        keyword: String,
        pageNo: Int = 1,
        numOfRows: Int = 20,
    ): PlantSearchResult {

        val xml = RetrofitClient.plantApi.searchPlants(
            apiKey = BuildConfig.NONGSARO_API_KEY,
            searchType = "sCntntsSj",
            keyword = keyword,
            pageNo = pageNo,
            numOfRows = numOfRows,
        )

        return PlantXmlParser.parsePlantSearchResult(xml)
    }

    suspend fun getPlantDetail(
        contentNo: String,
        ): PlantDetail {

        // Retrofit으로 상세 API의 XML 문자열을 받아.
        val xml = RetrofitClient.plantApi.getPlantDetail(
            apiKey = BuildConfig.NONGSARO_API_KEY,
            contentNo = contentNo,
            )

        // XML 문자열을 PlantDetail 객체로 변환해서 반환해.
        return PlantDetailXmlParser.parse(xml)
    }
}