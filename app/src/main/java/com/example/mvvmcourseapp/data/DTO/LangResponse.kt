package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.Lang

data class LangResponse(
    val id: Int,
    val name: String
) {
    fun toLang() : Lang {
        return Lang(id, name)
    }
}
