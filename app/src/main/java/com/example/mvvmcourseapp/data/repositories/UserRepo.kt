package com.example.mvvmcourseapp.data.repositories

import android.util.Log
import com.example.mvvmcourseapp.PassHash
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.DTO.LoginRequest
import com.example.mvvmcourseapp.data.DTO.users.RefreshRequest
import com.example.mvvmcourseapp.data.DTO.users.RegisterRequest
import com.example.mvvmcourseapp.data.DTO.users.UpdateSettingsRequest
import com.example.mvvmcourseapp.data.DTO.users.UserResponse
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.dao.Dao
import com.example.mvvmcourseapp.data.models.UserSettings
import com.example.mvvmcourseapp.data.services.ApiService

class UserRepo(private val dao: Dao, private val api: ApiService, private val sessionManager: SessionManager)
{
    suspend fun login(login: String, password: String): Boolean {
        val response = api.login(LoginRequest(login, password))
        sessionManager.saveTokens(response.access, response.refresh)
        val response2 = api.getMe()
        if (response2.isSuccessful) {
            if (dao.getUserByLogin(login) == null) {
                dao.addUser(User(response2.body()!!.id, login, "", ""))
            }
        }
        return true
    }

    suspend fun getCurrentUser(): UserResponse {
        return api.getMe().body()!!
    }

    suspend fun refreshUserSettings() {
        val response = api.getUserSettings()

        if (response.isSuccessful) {
            Log.e("USERsettings", response.body().toString())
            val settingsFromServer = response.body() ?: emptyList()
            val listOfSettings = settingsFromServer.map {
                it.toUserSettings()
            }
            Log.d("USERSETTINGSENTITY", listOfSettings.toString())
            dao.clearUserSettings()
            dao.insertAllUserSettings(listOfSettings)
        }
    }

    suspend fun register(login: String, email: String, password: String): Boolean {
        val response = api.register(RegisterRequest(login, email, password))
        return response.isSuccessful
    }

    fun logout() {
        sessionManager.logout()
    }

    suspend fun refreshToken(): Boolean {
        val refresh = sessionManager.getRefreshToken() ?: return false

        return try {
            val response = api.refresh(RefreshRequest(refresh))
            sessionManager.saveTokens(response.access, refresh)
            true
        } catch (e: Exception) {
            false
        }
    }



//    suspend fun login(login:String, pass:String) :Boolean {
//        val userFromDb=dao.getUserByLogin(login)
//        if(userFromDb!=null)
//            return PassHash.verifyPassword(pass, userFromDb.pass)
//        return false
//    }

    suspend fun getUserByLogin(login:String):User?
    {
        return dao.getUserByLogin(login)
    }

    suspend fun getUserSettings(user: User): List<UserSettings>
    {
        return dao.getUserSettings(user.id)
    }

    suspend fun getUserSettingsByLang(user: User, lang:Int): UserSettings
    {
        return dao.getUserSettingsByLang(user.id, lang)
    }

    suspend fun getUserSettingsAndLangNames(user:User):List<LangLvlView>
    {
        return dao.getUserSettingsAndLangNames(user.id)
    }

    suspend fun updateUserSettingsOnServer(user: UpdateSettingsRequest)
    {
        api.updateUserSettings(user)
    }

    suspend fun updateUserSettingsOnDb(userSetting : UserSettings) {
        dao.updateUserSettings(userSetting)
    }

}