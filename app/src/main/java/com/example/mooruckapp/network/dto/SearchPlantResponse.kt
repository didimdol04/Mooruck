package com.example.mooruckapp.network.dto

import com.google.gson.annotations.SerializedName

data class SearchPlantResponse(

    @SerializedName("response")
    val response: Response

)

data class Response(

    @SerializedName("body")
    val body: Body

)

data class Body(

    @SerializedName("items")
    val items: Items

)

data class Items(

    @SerializedName("item")
    val item: List<PlantItem>

)

data class PlantItem(

    @SerializedName("cntntsNo")
    val contentNo: String,

    @SerializedName("cntntsSj")
    val title: String

)