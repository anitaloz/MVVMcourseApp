package com.example.mvvmcourseapp.UIhelper

import com.example.mvvmcourseapp.data.models.Option

data class OptionItem(
    val option: Option,
    var optionImage : Int,
    val optionText : String=option.quizQuestionOption
)