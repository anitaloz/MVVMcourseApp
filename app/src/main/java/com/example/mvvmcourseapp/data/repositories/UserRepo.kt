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
import com.example.mvvmcourseapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.filter

class UserRepo(private val dao: Dao, private val api: ApiService, private val sessionManager: SessionManager, private val networkUtils : NetworkUtils)
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

    suspend fun getCurrentUser(): User {
        return if (networkUtils.isNetworkAvailable()) {
            api.getMe().body()!!.toUser()
        } else {
            withContext(Dispatchers.IO) {
                dao.getMe()
            }
        }
    }

    suspend fun synchronizeUserSettings(userId: Int) {
        if (!networkUtils.isNetworkAvailable()) return

        try {
            // Все операции с БД выполняем на IO потоке
            val response = withContext(Dispatchers.IO) {
                api.getUserSettings() // API вызов
            }

            if (!response.isSuccessful) {
                Log.e("SYNC", "Failed to get settings from server: ${response.code()}")
                return
            }

            val settingsFromServer = response.body() ?: emptyList()

            // Операции с БД на IO потоке
            val localSettings = withContext(Dispatchers.IO) {
                dao.getUserSettings(userId)
            }

            // Обработка dirty настроек
            val dirtySettings = localSettings.filter { it.isDirty }

            if (dirtySettings.isNotEmpty()) {
                dirtySettings.forEach { setting ->
                    try {
                        val updateResponse = withContext(Dispatchers.IO) {
                            api.updateUserSettings(
                                UpdateSettingsRequest(
                                    setting.newQ,
                                    setting.maxRepQuestions,
                                    setting.langLvl,
                                    setting.langId
                                )
                            )
                        }

                        if (updateResponse.isSuccessful) {
                            withContext(Dispatchers.IO) {
                                dao.updateUserSettings(setting.copy(isDirty = false))
                            }
                            Log.d("SYNC", "Successfully synced setting ${setting.id}")
                        } else {
                            Log.e("SYNC", "Failed to sync setting ${setting.id}: ${updateResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("SYNC", "Error syncing setting ${setting.id}", e)
                    }
                }
            }

            // Обновляем локальные данные
            val settingsToInsert = settingsFromServer.map { serverSetting ->
                UserSettings(
                    id = null,
                    newQ = serverSetting.new_questions,
                    maxRepQuestions = serverSetting.max_rep_questions,
                    langLvl = serverSetting.lang_lvl,
                    langId = serverSetting.lang,
                    isDirty = false,
                    userId = serverSetting.user,
                )
            }

            // Применяем изменения в транзакции на IO потоке
            if (settingsToInsert.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    dao.runInTransaction {
                        settingsToInsert.forEach { dao.insertUserSettings(it) }
                    }
                }
                Log.d("SYNC", "Sync completed: inserted=${settingsToInsert.size}")
            } else {
                Log.d("SYNC", "No changes needed")
            }

        } catch (e: Exception) {
            Log.e("SYNC", "Failed to synchronize user settings", e)
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

    suspend fun updateUserSettings(userSettings: UserSettings)
    {
        if (networkUtils.isNetworkAvailable()) {
            api.updateUserSettings(
                UpdateSettingsRequest(
                    userSettings.newQ,
                    userSettings.maxRepQuestions,
                    userSettings.langLvl,
                    userSettings.langId
                )
            )
            dao.updateUserSettings(userSettings.copy(isDirty = false))
        } else {
            dao.updateUserSettings(userSettings.copy(isDirty = true))
        }
    }

    suspend fun deleteUserDataFromRoom() {
        dao.clearUserSettings()
        dao.clearUsers()
    }
}