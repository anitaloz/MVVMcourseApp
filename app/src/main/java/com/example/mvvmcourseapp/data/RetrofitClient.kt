//package com.example.mvvmcourseapp.data
//
//import com.example.mvvmcourseapp.data.services.ApiService
//import com.example.mvvmcourseapp.data.services.AuthInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//
//object RetrofitClient {
//
//    private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"
//
//    private val logging = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
//    private val client = OkHttpClient.Builder()
//        .addInterceptor(AuthInterceptor(sessionManager))
//        .addInterceptor(logging)
//        .build()
//
//
//    val api: ApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }
//}
