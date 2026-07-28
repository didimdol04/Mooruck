package com.example.mooruckapp.network.dto

data class PlantDetail(
    // 농사로 콘텐츠 번호
    val contentNo: String = "",

    // 식물 이름
    val name: String = "",

    // 광도 정보
    val lightDemand: String = "",

    // 습도 정보
    val humidity: String = "",

    // 적정 온도 정보
    val temperature: String = "",

    // 봄철 물주기 코드
    val springWaterCode: String = "",
)