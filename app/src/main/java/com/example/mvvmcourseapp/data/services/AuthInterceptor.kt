package com.example.mvvmcourseapp.data.services

import android.util.Log
import com.example.mvvmcourseapp.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        // Список эндпоинтов, которые не требуют авторизации
        private val PUBLIC_ENDPOINTS = listOf(
            "/login/",
            "/register/",
            "/refresh/"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Проверяем, требует ли запрос авторизации
        val requiresAuth = PUBLIC_ENDPOINTS.none { url.contains(it) }

        val modifiedRequest = if (requiresAuth) {
            val token = sessionManager.getAccessToken()

            if (token != null) {
                Log.d("AUTH", "Adding token to request: $url")
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                Log.w("AUTH", "No token available for protected endpoint: $url")
                request
            }
        } else {
            Log.d("AUTH", "Public endpoint, no token added: $url")
            request
        }

        return chain.proceed(modifiedRequest)
    }
}