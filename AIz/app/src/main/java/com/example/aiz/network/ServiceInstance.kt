package com.example.aiz.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://10.129.77.222:5001"
    private const val TIMEOUT_MIN: Long = 5

    private val moshi = Moshi
        .Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_MIN, TimeUnit.MINUTES)
        .readTimeout(TIMEOUT_MIN, TimeUnit.MINUTES)
        .writeTimeout(TIMEOUT_MIN, TimeUnit.MINUTES)
        .callTimeout(TIMEOUT_MIN, TimeUnit.MINUTES)
        .build()


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
