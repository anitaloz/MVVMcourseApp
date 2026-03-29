package com.example.mvvmcourseapp.data.DTO

import com.google.gson.annotations.SerializedName

data class UserCodeResponse(
    @SerializedName("id")
    val id: Int, // Обязательно! Используем для генерации

    @SerializedName("file")
    val fileUrl: String,

    // Поле tasks будет пустым списком [] при первой загрузке
    @SerializedName("tasks")
    val tasks: List<GeneratedTaskResponse> = emptyList()
)

data class GenerateActionResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("count")
    val count: Int,

    @SerializedName("message")
    val message: String? = null
)

data class GeneratedTaskResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("full_snippet")
    val fullSnippet: String, // Код с контекстом (5 строк)

    @SerializedName("broken_line")
    val brokenLine: String,  // Измененная строка

    @SerializedName("correct_answer")
    val correctAnswer: String // Исходная правильная строка
)