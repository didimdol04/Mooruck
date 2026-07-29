package com.example.mooruckapp.ui.home

data class HomePlantItem(
    val id: Long,
    val plantName: String,
    val nickname: String?,
    val profileImageUri: String?,
    val wateringMessage: String
)