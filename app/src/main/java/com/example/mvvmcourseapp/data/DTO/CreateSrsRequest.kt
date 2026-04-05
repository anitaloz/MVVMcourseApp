package com.example.mvvmcourseapp.data.DTO

import com.google.gson.annotations.SerializedName

data class CreateSrsRequest(
    @SerializedName("question")
    val questionId: Int,

    @SerializedName("ef")
    val ef: Double = 2.5,

    @SerializedName("repetition_count")
    val repetitionCount: Int = 0,

    @SerializedName("interval")
    val interval: Int = 0,

    @SerializedName("last_review_date")
    val lastReviewDate: Long? = null
)