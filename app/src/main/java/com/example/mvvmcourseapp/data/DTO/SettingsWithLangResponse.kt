package com.example.mvvmcourseapp.data.DTO

data class SettingsWithLangResponse(
    val settings_id: Int,
    val lang_id: Int,
    val lang_name: String,
    val lang_lvl: Int
)
