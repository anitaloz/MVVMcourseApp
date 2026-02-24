package com.example.mvvmcourseapp.data.DTO

data class UserSettingsResponse(
    val id: Int,
    val user: Int,
    val lang: Int,
    val new_questions: Int,
    val max_rep_questions: Int,
    val lang_lvl: Int
)
