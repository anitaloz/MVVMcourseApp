package com.example.mvvmcourseapp.data.DTO

data class UpdateSettingsRequest(
    val new_questions: Int? = null,
    val max_rep_questions: Int? = null,
    val lang_lvl: Int? = null,
    val lang: Int? = null
)
