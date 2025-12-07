package com.example.mvvmcourseapp.viewModels

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.OptionItem
import com.example.mvvmcourseapp.adapters.OptionItemAdapter
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.models.SRSTools
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel(
    private val userRepo: UserRepo,
    private val quizQuestionRepo: QuizQuestionRepo,
    private val sharedViewModel: SharedViewModel
): ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState

    private val _events= Channel<QuizEvent>()
    val events = _events.receiveAsFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = 0

    init {
        loadQuestions(currentQuestionIndex)
    }

    fun loadQuestions(index:Int) {
        questions = sharedViewModel.questionList.value ?: emptyList()
        //currentQuestionIndex = sharedViewModel.index.value ?: 0

        if (questions.isNotEmpty()) {
            loadQuestion(currentQuestionIndex)
        }
    }

    private fun loadQuestion(index: Int) {
        if (index >= questions.size) {
            navigateToResultsHandled()
            return
        }

        currentQuestionIndex = index
        //sharedViewModel.setIndex(index)

        val currentQuestion = questions[currentQuestionIndex]

        viewModelScope.launch {
            try {
                val options = quizQuestionRepo.getOptionsByQuizQuestion(currentQuestion)
                    .map { OptionItem(it, R.drawable.empty_option) }
                Log.d("OPTIONS", questions.toString())
                _uiState.value = _uiState.value.copy(
                    currentQuestion = currentQuestion,
                    options = options,
                    questionNumber = index + 1,
                    totalQuestions = questions.size,
                    isDelayActive = false, // Сбрасываем задержку
                    showFeedback = false   // Сбрасываем фидбек
                )
            } catch (e: Exception) {
                sendEvent(QuizEvent.showError("Error loading options: ${e.message}"))
            }
        }
    }

    fun onOptionSelected(optionIndex: Int, optionItem: OptionItem, time:Int, question: QuizQuestion=questions[currentQuestionIndex]) {
        if (_uiState.value.isDelayActive) return

        // Блокируем дальнейшие клики
        _uiState.value = _uiState.value.copy(
            isDelayActive = true,
            selectedOptionIndex = optionIndex,
            showFeedback = true
        )

        // Обновляем счет
        //ЛОГИКА SPACED REPETITION если вопрос из новых неповторяемых, то новая запись в SRSTools иначе update по формуле
        if(sharedViewModel.category.value?.categoryName!="Тест на определение уровня") {
            var timer: Int = 0
            if (optionItem.option.correct) {
                var corrAns = _uiState.value.correctAns.toInt()
                if (time >= 20)
                    timer = 3
                else {
                    if (time >= 10)
                        timer = 2
                    else timer = 1
                }
                //Log.d("TTTTTTTTTTTTTTTT", "${_uiState.value.repetitions}")
                if (_uiState.value.questionNumber <= _uiState.value.totalQuestions - _uiState.value.repetitions) {
                    sharedViewModel.setCorrectAnswer(corrAns + 1)
                    corrAns += 1
                    viewModelScope.launch {
                        val flqr = quizQuestionRepo.questionIsRepeatable(
                            question,
                            sharedViewModel.user.value!!
                        )
                        if (flqr) {
                            quizQuestionRepo.updateSrsTools(
                                quizQuestionRepo.getQuestionRepeatable(
                                    question,
                                    sharedViewModel.user.value!!
                                ), timer
                            )
                        } else {
                            quizQuestionRepo.insertSrsTools(
                                SRSTools(
                                    null,
                                    2.5,
                                    1,
                                    1,
                                    System.currentTimeMillis(),
                                    question.id,
                                    sharedViewModel.user.value?.id!!
                                )
                            )
                        }
                    }
                }
                //_uiState.value=_uiState.value.copy(correctAns = sharedViewModel.correctAnswer.value!!)
                _uiState.value =  _uiState.value.copy(correctAns = stringScoreDisplay(corrAns))
            } else {
                //sharedViewModel.addQuestiontoQuiestionList(_uiState.value.currentQuestion!!)
                questions = questions + _uiState.value.currentQuestion!!
                var wrongAns = _uiState.value.wrongAns.toInt()
                if (_uiState.value.questionNumber <= _uiState.value.totalQuestions - _uiState.value.repetitions) {
                    sharedViewModel.setWrongAnswer(wrongAns + 1)
                    wrongAns += 1
                    timer = 0
                    viewModelScope.launch {
                        val flqr = quizQuestionRepo.questionIsRepeatable(
                            question,
                            sharedViewModel.user.value!!
                        )
                        if (flqr) {
                            quizQuestionRepo.updateSrsTools(
                                quizQuestionRepo.getQuestionRepeatable(
                                    question,
                                    sharedViewModel.user.value!!
                                ), timer
                            )
                        } else {
                            quizQuestionRepo.insertSrsTools(
                                SRSTools(
                                    null,
                                    2.5,
                                    1,
                                    1,
                                    System.currentTimeMillis(),
                                    question.id,
                                    sharedViewModel.user.value?.id!!
                                )
                            )
                        }
                    }
                }
                val rep = _uiState.value.repetitions + 1
                //_uiState.value=_uiState.value.copy(wrongAns = sharedViewModel.wrongAnswer.value!!, repetitions = rep)
                _uiState.value =
                    _uiState.value.copy(wrongAns = stringScoreDisplay(wrongAns), repetitions = rep)

            }
        }
        else {
            if (optionItem.option.correct) {
                var corrAns = _uiState.value.correctAns.toInt()
                sharedViewModel.setCorrectAnswer(corrAns + 1)
                corrAns += 1
                _uiState.value = _uiState.value.copy(correctAns = stringScoreDisplay(corrAns))
            }
            else {
                var wrongAns = _uiState.value.wrongAns.toInt()
                sharedViewModel.setWrongAnswer(wrongAns + 1)
                wrongAns += 1
                _uiState.value =
                    _uiState.value.copy(wrongAns = stringScoreDisplay(wrongAns))
            }

        }
        viewModelScope.launch {
            delay(1000)

            val nextIndex = currentQuestionIndex + 1
            if (nextIndex < questions.size) {
                loadQuestion(nextIndex) // Загружаем следующий вопрос
            } else {
                navigateToResultsHandled()
            }

            // Разблокируем клики после навигации
            _uiState.value = _uiState.value.copy(isDelayActive = false)
        }
    }

    fun onTimerFinished(question: QuizQuestion=questions[currentQuestionIndex]) {
        val nextIndex = currentQuestionIndex + 1
        //sharedViewModel.addQuestiontoQuiestionList(_uiState.value.currentQuestion!!)
        if(sharedViewModel.category.value?.categoryName!="Тест на определение уровня") {
            questions += _uiState.value.currentQuestion!!
            //questions=sharedViewModel.questionList.value!!
            var wrongAns = _uiState.value.wrongAns.toInt()
            if (_uiState.value.questionNumber <= _uiState.value.totalQuestions - _uiState.value.repetitions) {
                sharedViewModel.setWrongAnswer(wrongAns + 1)
                wrongAns += 1
                viewModelScope.launch {
                    val flqr = quizQuestionRepo.questionIsRepeatable(
                        question,
                        sharedViewModel.user.value!!
                    )
                    if (flqr) {
                        quizQuestionRepo.updateSrsTools(
                            quizQuestionRepo.getQuestionRepeatable(
                                question,
                                sharedViewModel.user.value!!
                            ), 0
                        )
                    } else {
                        quizQuestionRepo.insertSrsTools(
                            SRSTools(
                                null,
                                2.5,
                                1,
                                0,
                                System.currentTimeMillis(),
                                question.id,
                                sharedViewModel.user.value?.id!!
                            )
                        )
                    }
                }
            }
            val rep = _uiState.value.repetitions + 1
            //sharedViewModel.addWrongAnswer()
            //_uiState.value=_uiState.value.copy(wrongAns = sharedViewModel.wrongAnswer.value!!)
            _uiState.value =
                _uiState.value.copy(wrongAns = stringScoreDisplay(wrongAns), repetitions = rep)
        }
        else {
            var wrongAns = _uiState.value.wrongAns.toInt()
            sharedViewModel.setWrongAnswer(wrongAns + 1)
            wrongAns += 1
            _uiState.value =
                _uiState.value.copy(wrongAns = stringScoreDisplay(wrongAns))
        }
        if (nextIndex < questions.size) {
            loadQuestion(nextIndex)
        } else {
            navigateToResultsHandled()
        }
    }

    fun stringScoreDisplay(score: Int) : String {
        if(score==0)
            return "0"
        if (score < 10) {
            return "0${score}"
        }
        return score.toString()
    }
    private fun sendEvent(QuizEvent: QuizEvent) {
        viewModelScope.launch {
            _events.send(QuizEvent)
        }
    }
    fun navigateToResultsHandled() {
        sendEvent(QuizEvent.NavigateToResults)
    }

    fun navigateToMenuHandled() {
        sendEvent(QuizEvent.NavigateToMenu)
    }

    data class QuizUiState(
        val currentQuestion: QuizQuestion? = null,
        val options: List<OptionItem> = emptyList(),
        val questionNumber: Int = 0,
        val totalQuestions: Int = 0,
        val selectedOptionIndex: Int = -1,
        val showFeedback: Boolean = false,
        val isDelayActive: Boolean = false,
        val correctAns:String = "0",
        val wrongAns:String="0",
        val repetitions: Int = 0,
        val timerSeconds:Int = 30
    )

    sealed class QuizEvent{
        object NavigateToResults: QuizEvent()
        object NavigateToMenu : QuizEvent()
        data class showError(val error:String): QuizEvent()
    }
}