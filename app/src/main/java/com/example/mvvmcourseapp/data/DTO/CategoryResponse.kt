package com.example.mvvmcourseapp.data.DTO

import com.example.mvvmcourseapp.data.models.Category

data class CategoryResponse(
    val id: Int,
    val name: String,
    val lang: Int
) {
    fun toCategory() : Category{
        return Category(id, name, lang)
    }
}
