package com.example.mvvmcourseapp

import android.content.Context

import java.util.UUID


class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_USER_LOGIN="user_login"
    }

    fun getUserLogin():String?{
        return sharedPreferences.getString(KEY_USER_LOGIN, null)
    }

    // Сохраняем статус входа
    fun saveAuthToken(userLogin: String, generateNewToken: Boolean = false) {
        val token = if (generateNewToken || fetchAuthToken() == null) {
            generateSecureToken()
        } else {
            fetchAuthToken()!!
        }

        sharedPreferences.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_LOGIN, userLogin)
            apply()
        }
    }

    private fun generateSecureToken(): String {

        return UUID.randomUUID().toString()

    }

    // Получаем токен
    fun fetchAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    // Проверяем, вошел ли пользователь
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Выход (очищаем данные)
    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
}