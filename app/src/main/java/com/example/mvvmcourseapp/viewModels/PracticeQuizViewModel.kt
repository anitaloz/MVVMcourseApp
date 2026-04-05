package com.example.mvvmcourseapp.viewModels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.OptionItem
import com.example.mvvmcourseapp.data.DTO.GeneratedTaskResponse
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeQuizViewModel(
    private val userRepo: UserRepo,
    private val sharedViewModel: SharedViewModel,
    private val quizQuestionRepo: QuizQuestionRepo,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeQuizUiState())
    val uiState: StateFlow<PracticeQuizUiState> = _uiState

    private val _events = Channel<PracticeQuizEvent>()
    val events = _events.receiveAsFlow()

    // Внутренние переменные для навигации по списку
    private var tasks: List<GeneratedTaskResponse> = emptyList()
    private var currentTaskIndex = 0

    init {
        // Подписываемся на ID файла из SharedViewModel
        viewModelScope.launch {
            sharedViewModel.currentFileId.collect { id ->
                id?.let { loadQuestionsFromApi(it) }
            }
        }
    }


    private fun verifyAnswer(userEditedCode: String) {
        val currentTask = _uiState.value.currentQuestion ?: return

        // Простая логика проверки
        val isCorrect = userEditedCode.contains(currentTask.correctAnswer.trim())

        val newCorrect = if (isCorrect) (_uiState.value.correctAns.toInt() + 1) else _uiState.value.correctAns.toInt()
        val newWrong = if (!isCorrect) (_uiState.value.wrongAns.toInt() + 1) else _uiState.value.wrongAns.toInt()

        // ✅ ПРАВИЛЬНО: Переключаемся на Main поток для обновления SharedViewModel
        viewModelScope.launch(Dispatchers.Main) {
            sharedViewModel.setCorrectAnswer(newCorrect)
            sharedViewModel.setWrongAnswer(newWrong)
        }

        _uiState.update { currentState ->
            currentState.copy(
                isVerified = true,
                showFeedback = true,
                isCorrect = isCorrect,
                correctAns = stringScoreDisplay(newCorrect),
                wrongAns = stringScoreDisplay(newWrong)
            )
        }
    }

    private fun loadQuestionsFromApi(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = quizQuestionRepo.getPracticeTasks(id)
                if (response.isSuccessful) {
                    tasks = response.body()?.tasks ?: emptyList()

                    withContext(Dispatchers.Main) {
                        sharedViewModel.setQuestionList(emptyList())
                        sharedViewModel.setWrongAnswer(0)
                        sharedViewModel.setCorrectAnswer(0)
                        sharedViewModel.setTotalSize(tasks.size)
                        sharedViewModel.setCurrentFileId(-1)
                    }

                    if (tasks.isNotEmpty()) {
                        setupTask(0)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendEvent(PracticeQuizEvent.ShowToast("Ошибка загрузки: ${e.message}"))
                }
            }
        }
    }

    // Метод для подготовки конкретного задания по индексу
    private fun setupTask(index: Int) {
        if (index >= tasks.size) {
            navigateToPracticeResult()
            return
        }

        currentTaskIndex = index
        val task = tasks[index]

        _uiState.update { currentState ->
            currentState.copy(
                currentQuestion = task,
                questionNumber = index + 1,
                totalQuestions = tasks.size,
                isVerified = false, // Сбрасываем флаг проверки для нового вопроса
                showFeedback = false
            )
        }
    }

    // Логика кнопки "Проверить" / "Далее"
    fun onActionClicked(userEditedCode: String) {
        val state = _uiState.value

        if (!state.isVerified) {
            // Режим ПРОВЕРКИ
            verifyAnswer(userEditedCode)
        } else {
            // Режим ПЕРЕХОДА к следующему
            setupTask(currentTaskIndex + 1)
        }
    }

    private fun stringScoreDisplay(score: Int): String = if (score < 10) "0$score" else score.toString()

    private fun sendEvent(event: PracticeQuizEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    fun navigateToPracticeMenu() {
        sendEvent(PracticeQuizEvent.NavigateToPracticeMenu)
    }

    fun navigateToPracticeResult() {
        sendEvent(PracticeQuizEvent.NavigateToResults)
    }

    data class PracticeQuizUiState(
        val currentQuestion: GeneratedTaskResponse? = null,
        val questionNumber: Int = 0,
        val totalQuestions: Int = 0,
        val isVerified: Boolean = false, // Проверен ли текущий ответ
        val isCorrect: Boolean = false,  // Верно ли исправлено
        val showFeedback: Boolean = false,
        val correctAns: String = "00",
        val wrongAns: String = "00"
    )

    sealed class PracticeQuizEvent {
        data class ShowToast(val message: String) : PracticeQuizEvent()
        object NavigateToPracticeMenu : PracticeQuizEvent()
        object NavigateToResults : PracticeQuizEvent() // Добавьте это событие
        data class ShowValidationFeedbackError(val state: String) : PracticeQuizEvent()
    }
}