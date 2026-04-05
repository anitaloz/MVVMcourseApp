package com.example.mvvmcourseapp.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.mvvmcourseapp.data.DTO.CreateSrsRequest
import com.example.mvvmcourseapp.data.DTO.SrsResponse
import java.time.LocalDate
import java.util.Date
import kotlin.Int


//TODO insert когда пользователь изучает новый вопрос
@Entity(tableName = "srs_tools",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuizQuestion::class,
            parentColumns = ["id"],
            childColumns = ["qq_id"],
            onDelete = ForeignKey.CASCADE
        )
    ])
data class SRSTools (
    @PrimaryKey(autoGenerate = true) val id:Long?,
    val EF:Double = 2.5,//фактор легкости EF
    @ColumnInfo(name="repetition_count") val n:Int=1,//количество повторения n
    val interval:Int=1,//интервал I
    @ColumnInfo(name="last_review_date") val lastReviewDate: Long? = null,
    @ColumnInfo(name="qq_id") val qqId: Int,
    @ColumnInfo(name="user_id") val userId:Int,
    @ColumnInfo(name = "server_id") val serverId: Long? = null,
    @ColumnInfo(name="is_dirty") val isDirty: Boolean = false,
) {
    fun toCreateSrsRequest() : CreateSrsRequest {
        return CreateSrsRequest (
            questionId = qqId,
            ef = EF,
            repetitionCount = n,
            interval = interval,
            lastReviewDate = lastReviewDate,
        )
    }
}


