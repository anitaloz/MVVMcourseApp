package com.example.mvvmcourseapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.models.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {
    @Transaction
    suspend fun runInTransaction(block: suspend () -> Unit) {
        block()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettings)

    @Delete
    suspend fun deleteUserSettings(settings: UserSettings)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addUser(user: User)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addUserSettings(userSettings: UserSettings)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users LIMIT 1")
    fun getMe(): User

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): User?

    @Query("SELECT * FROM user_settings WHERE user_id== :userId")
    suspend fun getUserSettings(userId:Int?): List<UserSettings>


    @Delete
    suspend fun deleteUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT l.id AS lang_id, l.lang_name, u.lang_lvl, u.id AS user_settings_id FROM lang l JOIN user_settings u ON l.id==u.lang_id WHERE user_id = :userId")
    suspend fun getUserSettingsAndLangNames(userId:Int?):List<LangLvlView>

    @Query("SELECT u.* FROM user_settings u WHERE u.lang_id= :langId AND user_id= :userId")
    suspend fun getUserSettingsByLang(userId: Int, langId:Int): UserSettings
    @Update
    suspend fun updateUserSettings(user: UserSettings)

    @Query("DELETE FROM user_settings")
    suspend fun clearUserSettings()

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUserSettings(settings: List<UserSettings>)

    @Delete
    suspend fun deleteAll(settings: List<UserSettings>)

    @Transaction
    suspend fun batchUpdate(
        toInsert: List<UserSettings>,
        toUpdate: List<UserSettings>,
        toDelete: List<UserSettings>
    ) {
        if (toInsert.isNotEmpty()) insertAllUserSettings(toInsert)
        if (toInsert.isNotEmpty()) insertAllUserSettings(toInsert)
        if (toDelete.isNotEmpty()) deleteAll(toDelete)
    }
}