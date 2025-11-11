package com.example.mvvmcourseapp.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.SettingsViewModel.SettingsEvent
import com.example.mvvmcourseapp.viewModels.SettingsViewModel.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsViewModel(
    private val userRepo: UserRepo,
    private val sessionManager: SessionManager,
    private val sharedViewModel: SharedViewModel,
    private val quizQuestionRepo: QuizQuestionRepo) : ViewModel()
{
    private val _uiState= MutableStateFlow<StatisticsUiState>(StatisticsUiState())
    val uiState : StateFlow<StatisticsUiState> = _uiState

    private val _events= Channel<StatisticsEvent>()
    val events=_events.receiveAsFlow()

    init{
        renderUiState()
    }

    private fun renderUiState()
    {
        viewModelScope.launch(Dispatchers.IO) {
            val langsList=quizQuestionRepo.getLangs().first()
            val langLvlViewList=mutableListOf<StatisticsView>()
            for (i: Lang in langsList)
            {
                langLvlViewList.add(quizQuestionRepo.getStatisticsView(i, 0, 30, sharedViewModel.user.value!!))

            }
            Log.d("LISTOFLANGLVLS22", langLvlViewList.toString())
            _uiState.value=_uiState.value.copy(langLvlViewList)
        }
    }

    fun onCategorySelected(selectedItem: String, statisticsView: StatisticsView) {
        Log.d("STATISTICS_VIEW_MODEL", "Selected: $selectedItem, current langId: ${statisticsView.langId}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val statisticsViewNew = if (statisticsView.selectedIndex == 0) {
                    quizQuestionRepo.getStatisticsView(
                        quizQuestionRepo.getLangByLangName(selectedItem),
                        0, 30, sharedViewModel.user.value!!
                    )
                } else {
                    quizQuestionRepo.getStatisticsView(
                        quizQuestionRepo.getCategoryByName(selectedItem),
                        0, 30, sharedViewModel.user.value!!
                    )
                }

                withContext(Dispatchers.Main) {
                    statisticsViewNew.selectedIndex = statisticsView.selectedIndex

                    // Обновляем состояние
                    _uiState.value = _uiState.value.copy(
                        listOfStaticsView = _uiState.value.listOfStaticsView.map { currentView ->
                            if (currentView.langId == statisticsView.langId) {
                                statisticsViewNew.copy(
                                    selectedIndex = statisticsViewNew.selectedIndex,
                                    langId = statisticsViewNew.langId
                                )
                            } else {
                                currentView
                            }
                        }
                    )

                    Log.d("STATISTICS_VIEW_MODEL", "Update completed for langId: ${statisticsView.langId}")
                }
            } catch (e: Exception) {
                Log.e("STATISTICS_VIEW_MODEL", "Error: ${e.message}", e)
                showError("Ошибка загрузки статистики")
            }
        }
    }
    private fun sendEvent(event: StatisticsEvent)
    {
        viewModelScope.launch{
            _events.send(event)
        }
    }
    fun navigateToMenu()
    {
        sendEvent(StatisticsEvent.NavigateToMenu)
    }

    fun showError(err:String)
    {
        sendEvent(StatisticsEvent.ShowError(err))
    }
    data class StatisticsUiState(
        val listOfStaticsView : List<StatisticsView> = emptyList()
    )

    sealed class StatisticsEvent{
        object NavigateToMenu: StatisticsEvent()
        data class ShowError(val error:String): StatisticsEvent()
    }

}