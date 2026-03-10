package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.Option

data class OptionResponse(
    val id: Int,
    val question: Int,
    val text: String,
    val correct: Boolean
) {
    fun toOption() : Option {
        return Option(id, question, text, correct)
    }
}
