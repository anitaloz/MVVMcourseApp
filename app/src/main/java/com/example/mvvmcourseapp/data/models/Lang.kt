package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "lang")
@Parcelize
data class Lang(@PrimaryKey val id:Int,
                     @ColumnInfo(name="lang_name") val langName: String) : Parcelable
