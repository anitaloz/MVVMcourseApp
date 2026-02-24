package com.example.mvvmcourseapp

import android.content.Context

import java.util.UUID

//
//class SessionManager(context: Context) {
//    private val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
//
//    companion object {
//        const val KEY_IS_LOGGED_IN = "is_logged_in"
//        const val KEY_AUTH_TOKEN = "auth_token"
//        const val KEY_USER_LOGIN="user_login"
//    }
//
//    fun getUserLogin():String?{
//        return sharedPreferences.getString(KEY_USER_LOGIN, null)
//    }
//
//    fun saveAuthToken(userLogin: String, generateNewToken: Boolean = false) {
//        val token = if (generateNewToken || fetchAuthToken() == null) {
//            generateSecureToken()
//        } else {
//            fetchAuthToken()!!
//        }
//
//        sharedPreferences.edit().apply {
//            putString(KEY_AUTH_TOKEN, token)
//            putBoolean(KEY_IS_LOGGED_IN, true)
//            putString(KEY_USER_LOGIN, userLogin)
//            apply()
//        }
//    }
//
//    private fun generateSecureToken(): String {
//
//        return UUID.randomUUID().toString()
//
//    }
//
//    fun fetchAuthToken(): String? {
//        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
//    }
//
//    fun isLoggedIn(): Boolean {
//        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
//    }
//
//    fun logout() {
//        sharedPreferences.edit().clear().apply()
//    }
//}

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
