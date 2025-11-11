package com.example.mvvmcourseapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.models.UserSettings
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.SettingsViewModel.SettingsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizResultViewModel (private val userRepo: UserRepo,
    private val quizQuestionRepo: QuizQuestionRepo,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {

    private val _uiState= MutableStateFlow(ResultUiState())
    val uiState:StateFlow<ResultUiState> = _uiState
    private val sendEvent= Channel<ResultEvent>()
    val events = sendEvent.receiveAsFlow()
    init {
        renderUiState()

        if(sharedViewModel.category.value?.categoryName=="Тест на определение уровня")
        {
            var langName=""
            viewModelScope.launch() {
                try {
                    val userSettings=userRepo.getUserSettingsByLang(sharedViewModel.user.value!!, sharedViewModel.category.value!!.langId)
                    if(sharedViewModel.correctAnswer.value!!<=6)
                    {
                        userSettings.langLvl=1
                    }
                    if(sharedViewModel.correctAnswer.value!!<=12 && sharedViewModel.correctAnswer.value!!>6)
                    {
                        userSettings.langLvl=2
                    }
                    if(sharedViewModel.correctAnswer.value!!<=15 && sharedViewModel.correctAnswer.value!!>12)
                    {
                        userSettings.langLvl=3
                    }
                    userRepo.updateUserSettings(userSettings)
                    withContext(Dispatchers.Main)
                    {
                        langName=quizQuestionRepo.getLangNameByLangId(sharedViewModel.category.value!!.langId)
                        val lvl=userSettings.langLvl
                        showLvlFeedback(lvl, langName)
                    }
                } catch(e:Exception)
                {
                    showError(e.message.toString())
                }
            }
        }
    }

    fun renderUiState()
    {
        _uiState.value=_uiState.value.copy(isLoading = true)
        val correctAnsCount=sharedViewModel.correctAnswer.value ?: 0
        val wrongAnsCount=sharedViewModel.wrongAnswer.value ?: 0
        val totalQuestions=sharedViewModel.questionList.value?.size ?: 0
        val resultInPercent=((correctAnsCount.toFloat()/(correctAnsCount+wrongAnsCount).toFloat())*100).toInt()
        _uiState.value=_uiState.value.copy(correctAnsCount=correctAnsCount, wrongAnsCount=wrongAnsCount, totalQuestions=totalQuestions, resultInPercent=resultInPercent, isLoading = false)

    }

    private fun sendEvent(resultEvent: ResultEvent) {
        viewModelScope.launch {
            sendEvent.send(resultEvent)
        }
    }

    fun navigateToMenuHandled() {
        sendEvent(ResultEvent.NavigateToMenu)
    }
    fun showError(err:String)
    {
        sendEvent(ResultEvent.ShowError(err))
    }

    fun showLvlFeedback(lvl:Int, langName: String)
    {
        sendEvent(ResultEvent.ShowLvlFeedBack(lvl, langName))
    }
    data class ResultUiState(
        val correctAnsCount:Int=0,
        val wrongAnsCount:Int=0,
        val totalQuestions:Int=0,
        val resultInPercent:Int=0,
        val isLoading: Boolean = true,
    )

    sealed class ResultEvent{
        object NavigateToMenu : ResultEvent()
        data class ShowError(val error:String): ResultEvent()
        data class ShowLvlFeedBack(val lvl:Int, val langName:String): ResultEvent()
    }
}

