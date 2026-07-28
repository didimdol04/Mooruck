package com.example.mooruckapp.network

import com.example.mooruckapp.network.api.PlantApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://api.nongsaro.go.kr/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // 농사로 API 기본 주소
            .client(okHttpClient) // 로그를 포함한 OkHttpClient
            .addConverterFactory(ScalarsConverterFactory.create()) // 응답을 String으로 받음
            .build()
    }

    val plantApi: PlantApiService by lazy {
        retrofit.create(PlantApiService::class.java)
    }
}