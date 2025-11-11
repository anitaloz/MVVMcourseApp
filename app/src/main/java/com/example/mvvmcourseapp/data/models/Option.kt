package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "options",
    foreignKeys = [
    androidx.room.ForeignKey(
        entity = QuizQuestion::class,
        parentColumns = ["id"],
        childColumns = ["quiz_question_id"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
@Parcelize
data class Option(
    @PrimaryKey val id: Int,
    @ColumnInfo(name="quiz_question_id") val quizQuestionId: Int,
    @ColumnInfo(name="quiz_question_option") val quizQuestionOption: String,
    val correct: Boolean//если данная опция верна -- 1, иначе 0
):Parcelable