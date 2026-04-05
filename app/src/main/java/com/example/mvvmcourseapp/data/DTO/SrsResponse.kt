package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.SRSTools

data class SrsResponse(
    val id: Long,
    val user: Int,
    val question: Int,
    val ef: Double,
    val repetition_count: Int,
    val interval: Int,
    val last_review_date: Long?
) {
    fun toSrs(): SRSTools {
        return SRSTools(
            id = null,
            EF = ef,
            n = repetition_count,
            interval = interval,
            lastReviewDate = last_review_date,
            qqId = question,
            userId = user,
            isDirty = false,
            serverId = id,
        )
    }
}
