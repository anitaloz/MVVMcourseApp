package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.QuizQuestion

data class QuizQuestionResponse(
    val id: Int,
    val lang: Int,
    val category: Int,
    val question_text: String,
    val explanation: String,
    val difficulty: Int
) {
    fun toQuizQuestion(): QuizQuestion {
        return QuizQuestion(id, lang, category, question_text, explanation, difficulty)
    }
}
