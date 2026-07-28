package com.example.mooruckapp.network.dto

data class PlantSearchResult(
    val plants: List<PlantItem>,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int,
)