package com.example.mvvmcourseapp.data.DTO

data class UpdateSrsRequest(
    val ef: Double? = null,
    val repetition_count: Int? = null,
    val interval: Int? = null,
    val last_review_date: String? = null
)
