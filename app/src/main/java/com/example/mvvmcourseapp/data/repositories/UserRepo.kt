package com.example.mvvmcourseapp.data.repositories

import android.util.Log
import com.example.mvvmcourseapp.PassHash
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.dao.Dao
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.UserSettings
import kotlinx.coroutines.flow.Flow

class UserRepo(private val dao:Dao)
{
    suspend fun authentication(login:String, user:User, langList:List<Lang>):Boolean
    {
        val fl=dao.getUserByLogin(login)
        if(fl==null) {
            val userId = dao.addUser(user)
            for (l:Lang in langList){
                Log.d("TAG", l.id.toString())
                dao.addUserSettings(UserSettings(null, userId.toInt(), l.id))
            }
        }
        return fl!=null
    }

    suspend fun login(login:String, pass:String) :Boolean {
        val userFromDb=dao.getUserByLogin(login)
        if(userFromDb!=null)
            return PassHash.verifyPassword(pass, userFromDb.pass)
        return false
    }

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