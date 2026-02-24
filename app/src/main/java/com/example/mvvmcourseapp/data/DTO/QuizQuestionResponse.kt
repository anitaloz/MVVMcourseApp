package com.example.mvvmcourseapp.data.DTO

data class QuizQuestionResponse(
    val id: Int,
    val lang: Int,
    val category: Int,
    val question_text: String,
    val explanation: String,
    val difficulty: Int
)
