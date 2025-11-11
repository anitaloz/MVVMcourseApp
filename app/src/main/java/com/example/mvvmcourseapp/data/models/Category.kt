package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "categories",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Lang::class,
            parentColumns = ["id"],
            childColumns = ["lang_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ])
@Parcelize
data class Category (@PrimaryKey val id:Int,
                     @ColumnInfo(name="category_name")val categoryName: String,
                     @ColumnInfo(name="lang_id") val langId: Int ):Parcelable
