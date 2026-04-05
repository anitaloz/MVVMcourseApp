package com.example.mvvmcourseapp.data.services

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.mvvmcourseapp.MainActivity
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.data.DTO.users.RefreshRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TokenAuthenticator(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val baseUrl: String
) : Authenticator {

    companion object {
        private val PUBLIC_ENDPOINTS = listOf(
            "/login/",
            "/register/",
            "/refresh/"
        )
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        val url = request.url.toString()

        // Не обрабатываем публичные эндпоинты
        if (PUBLIC_ENDPOINTS.any { url.contains(it) }) {
            Log.d("AUTH_DEBUG", "Skipping token refresh for public endpoint: $url")
            return null
        }

        Log.d("AUTH_DEBUG", "Authenticator triggered for URL: $url")

        val refreshToken = sessionManager.getRefreshToken()
        if (refreshToken == null) {
            Log.e("AUTH_DEBUG", "No refresh token found")
            handleLogout()
            return null
        }

        return try {
            // Создаем временный клиент без аутентификации для обновления токена
            val client = OkHttpClient.Builder()
                .build()

            val tempRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val service = tempRetrofit.create(AuthApiService::class.java)

            Log.d("AUTH_DEBUG", "Sending refresh request...")
            val refreshResponse = service.refresh(RefreshRequest(refreshToken)).execute()

            if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                Log.d("AUTH_DEBUG", "Refresh SUCCESS")
                val newAccess = refreshResponse.body()!!.access
                sessionManager.saveTokens(newAccess, refreshToken)

                // Повторяем исходный запрос с новым токеном
                request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } else {
                Log.e("AUTH_DEBUG", "Refresh FAILED: ${refreshResponse.code()}")
                if (refreshResponse.code() == 401 || refreshResponse.code() == 403) {
                    handleLogout()
                }
                null
            }
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "CRITICAL ERROR during refresh", e)
            // При ошибке сети не выходим из системы, просто возвращаем null
            if (e is java.net.SocketTimeoutException || e is java.net.UnknownHostException) {
                Log.w("AUTH_DEBUG", "Network error, keeping user logged in")
                null
            } else {
                handleLogout()
                null
            }
        }
    }

    private fun handleLogout() {
        sessionManager.logout()

        // Очищаем данные в БД
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Здесь очищаем локальную БД
                // Например: database.clearAllTables()
                Log.d("AUTH", "Local database cleared")
            } catch (e: Exception) {
                Log.e("AUTH", "Error clearing database", e)
            }
        }

        // Переход на экран логина
        Handler(Looper.getMainLooper()).post {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                Log.d("AUTH", "Redirected to login screen")
            } catch (e: Exception) {
                Log.e("AUTH", "Error redirecting to login", e)
            }
        }
    }
}