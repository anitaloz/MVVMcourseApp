package com.example.mvvmcourseapp.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.UserSettings
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.QuizViewModel.QuizEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepo: UserRepo,
    private val sessionManager: SessionManager,
    private val sharedViewModel: SharedViewModel,
    private val quizQuestionRepo: QuizQuestionRepo
): ViewModel()  {
    private val _uiState= MutableStateFlow<SettingsUiState>(SettingsUiState())
    val uiState : StateFlow<SettingsUiState> = _uiState

    private val _events= Channel<SettingsEvent>()
    val events=_events.receiveAsFlow()

    init{

        renderUiState()
    }
    private fun renderUiState()
    {
        viewModelScope.launch(Dispatchers.IO) {
            val userSettingsList=userRepo.getUserSettings(sharedViewModel.user.value!!)
            val langLvlViewList=userRepo.getUserSettingsAndLangNames(sharedViewModel.user.value!!)
            Log.d("LISTOFLANGLVLS22", langLvlViewList.toString())
            _uiState.value=_uiState.value.copy(newQuestionsInQuiz = userSettingsList.firstOrNull()?.newQ
                ?: -1)
            _uiState.value=_uiState.value.copy(repeatableQuestionsInQuiz = userSettingsList.firstOrNull()?.maxRepQuestions
                ?: -1)
            _uiState.value=_uiState.value.copy(listOfLanglLvls = langLvlViewList)
        }
    }

    //для обработки входных квизов
    fun loadNewSettings(newQ:Int, repQ:Int, langLvlList:List<LangLvlView>)
    {

        if(repQ>100)
            showValidationFeedbackError("Максимальное количество повторяемых вопросов не должно превышать 100")
        else {
            if (repQ < 0)
                showValidationFeedbackError("Максимальное количество повторяемых вопросов должно быть больше 0")
            else {
                if (newQ > 50)
                    showValidationFeedbackError("Количество новых вопросов не должно превышать 50")
                else {
                    if (newQ < 0)
                    showValidationFeedbackError("Количество новых вопросов должно быть болше 0")
                else {
                        Log.d("SAVE", "$newQ")
                        viewModelScope.launch() {
                            try {
//                                val userSettings =
//                                    userRepo.getUserSettings(sharedViewModel.user.value!!)
//                                val langs = quizQuestionRepo.getLangs().first()

                                for (u: LangLvlView in langLvlList) {
                                    userRepo.updateUserSettings(
                                        UserSettings(
                                            u.uid,
                                            sharedViewModel.user.value?.id!!,
                                            u.id,
                                            newQ,
                                            repQ,
                                            langLvl = u.lvl
                                        )
                                    )

                                }
                            } catch (e: Exception) {
                                showError(e.message.toString())
                            }
                        }
                        showValidationFeedbackSuccess("Данные сохранены")
                        renderUiState()
                    }
                }
            }
        }
    }
    private fun sendEvent(event: SettingsEvent)
    {
        viewModelScope.launch{
            _events.send(event)
        }
    }

    fun logout() {
        sessionManager.logout()
        sharedViewModel.setUser(null)
        Log.d("USER_SETTINGS", sharedViewModel.user.value.toString())
        navigateToMenu()
    }
    fun navigateToMenu()
    {
        sendEvent(SettingsEvent.NavigateToMenu)
    }

    fun showError(err:String)
    {
        sendEvent(SettingsEvent.ShowError(err))
    }
    fun showValidationFeedbackSuccess(state:String)
    {
        sendEvent(SettingsEvent.ShowValidationFeedbackSuccess(state))
    }

    fun showValidationFeedbackError(state:String)
    {
        sendEvent(SettingsEvent.ShowValidationFeedbackError(state))
    }

    data class SettingsUiState(
        val newQuestionsInQuiz:Int = 10,
        val repeatableQuestionsInQuiz:Int=10,
        val listOfLanglLvls : List<LangLvlView> = emptyList()
    )

    sealed class SettingsEvent{
        object NavigateToMenu: SettingsEvent()
        data class ShowError(val error:String): SettingsEvent()
        data class ShowValidationFeedbackSuccess(val state:String):SettingsEvent()
        data class ShowValidationFeedbackError(val state:String):SettingsEvent()
    }
}