package com.example.mvvmcourseapp.viewModels

import android.app.Application
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedViewModel(application: Application): ViewModel() {

//    private val _loadingState = MutableStateFlow(false)//если после удаления приложения данные в базу данных долго подгружаются поменять на true(загрузка)
//    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

    private val _user=MutableLiveData<User?>()
    val user:LiveData<User?> = _user

//    private val _lang=MutableLiveData<Lang>()
//    val lang:LiveData<Lang> = _lang
//
    private val _category=MutableLiveData<Category>()
    val category:LiveData<Category> = _category

    private val _questionList= MutableLiveData<List<QuizQuestion>?>()
    val questionList: LiveData<List<QuizQuestion>?> = _questionList

//    private var _index=MutableLiveData<Int?>()
//    val index:LiveData<Int?> = _index

    private var _correctAnswer=MutableLiveData<Int>()
    val correctAnswer:LiveData<Int> = _correctAnswer

    private var _wrongAnswer=MutableLiveData<Int>()
    val wrongAnswer:LiveData<Int> = _wrongAnswer

    fun setQuestionList(qq:List<QuizQuestion>?)
    {
        _questionList.value=qq
    }

//    fun addQuestiontoQuiestionList(qq: QuizQuestion)
//    {
//        val currentList = _questionList.value ?: emptyList()
//        _questionList.value = currentList + qq
//    }
//
////    fun setIndex(index:Int?)
////    {
////        _index.value=index
////    }

    fun setCorrectAnswer(correctAnswer:Int)
    {
        _correctAnswer.value=correctAnswer
    }

    fun setWrongAnswer(wrongAnswer:Int)
    {
        _wrongAnswer.value=wrongAnswer
    }

    fun setCategory(category:Category) {
        _category.value = category
        Log.wtf("SharedViewModel", "Category set: ${category.categoryName}")
    }

    fun setUser(user: User?) {
        _user.value = user
        Log.wtf("SharedViewModel", "User set: ${user?.login}")
    }

}