package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    @ColumnInfo(name = "login") var login: String,
    @ColumnInfo(name = "email") var email: String,
    @ColumnInfo(name = "pass") var pass: String,
) : Parcelable
