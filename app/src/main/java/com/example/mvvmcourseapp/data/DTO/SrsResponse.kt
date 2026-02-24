package com.example.mvvmcourseapp.data.DTO

data class SrsResponse(
    val id: Int,
    val user: Int,
    val question: Int,
    val ef: Double,
    val repetition_count: Int,
    val interval: Int,
    val last_review_date: String?
)
