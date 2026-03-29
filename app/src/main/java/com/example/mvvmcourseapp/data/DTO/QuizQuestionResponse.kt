package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.QuizQuestion

data class QuizQuestionResponse(
    val id: Int,
    val category: Int,
    val question_text: String,
    val difficulty: Int
) {
    fun toQuizQuestion(): QuizQuestion {
        return QuizQuestion(id, category, question_text, difficulty)
    }
}
