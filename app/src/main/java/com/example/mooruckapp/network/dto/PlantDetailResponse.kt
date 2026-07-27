package com.example.mooruckapp.network.dto

import com.google.gson.annotations.SerializedName

data class PlantDetailResponse(

    @SerializedName("response")
    val response: DetailResponse

)

data class DetailResponse(

    @SerializedName("body")
    val body: DetailBody

)

data class DetailBody(

    @SerializedName("item")
    val item: PlantDetail

)

data class PlantDetail(

    @SerializedName("distbNm")
    val plantName: String = "",

    @SerializedName("lighttdemanddoCodeNm")
    val light: String = "",

    @SerializedName("hdCodeNm")
    val humidity: String = "",

    @SerializedName("grwhTpCodeNm")
    val temperature: String = "",

    @SerializedName("watercycleSprngCode")
    val springWaterCode: String = "",

    @SerializedName("watercycleSummerCode")
    val summerWaterCode: String = "",

    @SerializedName("watercycleAutumnCode")
    val autumnWaterCode: String = "",

    @SerializedName("watercycleWinterCode")
    val winterWaterCode: String = ""
)