package com.example.mvvmcourseapp.data.DTO.users

import com.example.mvvmcourseapp.data.models.UserSettings

data class UserSettingsResponse(
    val id: Int,
    val user: Int,
    val lang: Int,
    val new_questions: Int,
    val max_rep_questions: Int,
    val lang_lvl: Int
) {
    fun toUserSettings(): UserSettings {
        return UserSettings(
            this.id,
            this.user,
            this.lang,
            this.lang_lvl,
            this.new_questions,
            this.max_rep_questions
        )
    }
}