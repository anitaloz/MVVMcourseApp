package com.example.mvvmcourseapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
        entity = Lang::class,
        parentColumns = ["id"],
        childColumns = ["language_id"],
        onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Parcelize
data class QuizQuestion(
    @PrimaryKey val id: Int,

    @ColumnInfo(name="language_id") val languageId: Int,
    @ColumnInfo(name="category_id")val categoryId: Int,

    @ColumnInfo(name="question_text")val questionText: String,
    val explanation: String,
    val difficulty: Int
): Parcelable