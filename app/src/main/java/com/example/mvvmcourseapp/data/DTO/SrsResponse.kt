package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.SRSTools

data class SrsResponse(
    val id: Int,
    val user: Int,
    val question: Int,
    val ef: Double,
    val repetition_count: Int,
    val interval: Int,
    val last_review_date: Long?
) {
    fun toSrs(): SRSTools {
        return SRSTools(
            id,
            ef,
            repetition_count,
            interval,
            last_review_date,
            question,
            user
        )
    }
}
