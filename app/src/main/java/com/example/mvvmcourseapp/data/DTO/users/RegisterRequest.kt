package com.example.mvvmcourseapp.data.DTO.users

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)