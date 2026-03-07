package com.example.mvvmcourseapp.data.repositories

import com.example.mvvmcourseapp.PassHash
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.DTO.LoginRequest
import com.example.mvvmcourseapp.data.DTO.users.RefreshRequest
import com.example.mvvmcourseapp.data.DTO.users.RegisterRequest
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
        return true
    }

    suspend fun getCurrentUser(): UserResponse {
        return api.getMe()
    }


    suspend fun register(login: String, email: String, password: String): Boolean {
        val response = api.register(RegisterRequest(login, email, password))
        if (response.isSuccessful) {
            dao.addUser(User(response.body()!!.id, login, email, password))
            val listOfSettings = api.getUserSettings()
            for (item in listOfSettings) {
                dao.addUserSettings(UserSettings(item.id, item.user, item.lang, item.new_questions, item.max_rep_questions))
            }
            dao.addUserSettings(UserSettings(null, response.body()!!.id, 1))
        }
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
        return dao.getUserSettingsByLang(user.id!!, lang)
    }

    suspend fun getUserSettingsAndLangNames(user:User):List<LangLvlView>
    {
        return dao.getUserSettingsAndLangNames(user.id)
    }
    suspend fun updateUserSettings(user: UserSettings)
    {
        dao.updateUserSettings(user)
    }

}