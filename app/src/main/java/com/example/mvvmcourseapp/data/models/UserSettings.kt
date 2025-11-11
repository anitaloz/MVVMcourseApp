package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "user_settings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
    androidx.room.ForeignKey(
        entity = Lang::class,
        parentColumns = ["id"],
        childColumns = ["lang_id"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )]
)
data class UserSettings(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    @ColumnInfo(name = "user_id") var userId: Int,
    @ColumnInfo(name = "lang_id") var langId:Int,
    @ColumnInfo(name = "new_questions") var newQ: Int = 10,
    @ColumnInfo(name = "max_rep_questions") var maxRepQuestions: Int = 10,
    @ColumnInfo(name = "lang_lvl") var langLvl:Int = 0,
) : Parcelable
