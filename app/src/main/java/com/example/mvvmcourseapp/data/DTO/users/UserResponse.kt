package com.example.mvvmcourseapp.data.DTO.users

import com.example.mvvmcourseapp.data.models.User

data class UserResponse(
    val id: Int,
    val login: String,
    val email: String
) {
    fun toUser(): User {
        return User(
            id = id,
            login = login,
            email = email,
            pass = "",
        )
    }

}