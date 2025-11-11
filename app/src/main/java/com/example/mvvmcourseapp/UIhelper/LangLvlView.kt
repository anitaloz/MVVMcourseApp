package com.example.mvvmcourseapp.UIhelper

import androidx.room.ColumnInfo
import com.example.mvvmcourseapp.data.models.Lang

data class LangLvlView(
    @ColumnInfo(name = "lang_id")val id:Int,
    @ColumnInfo(name = "lang_name")val langName: String,
    @ColumnInfo(name = "lang_lvl")val lvl:Int,
    @ColumnInfo(name = "user_settings_id")val uid:Int
){
}