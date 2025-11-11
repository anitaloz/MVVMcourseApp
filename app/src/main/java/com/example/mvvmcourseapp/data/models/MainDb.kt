package com.example.mvvmcourseapp.data.models

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mvvmcourseapp.data.dao.Dao
import com.example.mvvmcourseapp.data.dao.QuizQuestionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database (entities = [User::class, QuizQuestion::class, Lang::class, Category::class, Option::class, SRSTools::class, UserSettings::class], version=1)//перечисляем все таблицы(entity)
abstract class MainDb : RoomDatabase() {
    abstract fun getDao(): Dao
    abstract fun getQuizQuestionDao(): QuizQuestionDao

    companion object {

        fun getDb(context: Context): MainDb {
            return Room.databaseBuilder(
                context.applicationContext,
                MainDb::class.java,
                "CyberDb.databases"
            ).build()

        }
    }
}
