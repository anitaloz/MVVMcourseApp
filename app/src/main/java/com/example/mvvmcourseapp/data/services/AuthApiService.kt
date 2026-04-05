package com.example.mvvmcourseapp.data.services

import com.example.mvvmcourseapp.data.DTO.TokenResponse
import com.example.mvvmcourseapp.data.DTO.users.RefreshRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("users/token/refresh/")
    fun refresh(@Body request: RefreshRequest): retrofit2.Call<TokenResponse>
}