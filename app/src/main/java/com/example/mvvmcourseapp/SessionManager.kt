package com.example.mvvmcourseapp

import android.content.Context

import java.util.UUID

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}
