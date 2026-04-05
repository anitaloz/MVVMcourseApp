package com.example.mvvmcourseapp.viewModels

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.StatisticsViewModel.StatisticsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeMenuViewModel(
    private val userRepo: UserRepo,
    private val sessionManager: SessionManager,
    private val sharedViewModel: SharedViewModel,
    private val quizQuestionRepo: QuizQuestionRepo,
) : ViewModel() {
    private val _uiState= MutableStateFlow<PracticeMenuUiState>(PracticeMenuUiState())
    val uiState : StateFlow<PracticeMenuUiState> = _uiState

    private val _events = Channel<PracticeMenuEvent>()
    val events = _events.receiveAsFlow()

    init{
        renderUiState()
    }

    private fun renderUiState() {
        viewModelScope.launch(Dispatchers.IO) {
            // .first() возьмет текущее состояние списка из БД/Репозитория
            val langsList = quizQuestionRepo.getLangs().first()

            // Используем именованный параметр для ясности
            _uiState.update { currentState ->
                currentState.copy(listOfLangs = langsList)
            }
        }
    }

    // Добавьте методы для обновления этих полей из Фрагмента
    fun onLangSelected(lang: String) {
        _uiState.update { it.copy(selectedLang = lang) }
    }

    fun uploadAndGenerate() {
        val state = uiState.value
        val bytes = state.selectedFileBytes

        if (bytes == null){
            sharedViewModel.setCurrentFileId(-1)
            navigateToPracticeQuiz()
            return // Если файл не выбран, выходим
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _events.send(PracticeMenuEvent.ShowToast("Загрузка и генерация..."))

                // 1. Сначала загружаем файл
                val uploadResponse = quizQuestionRepo.uploadFile(
                    bytes = bytes,
                    fileName = state.selectedFileName ?: "file.txt",
                    language = state.selectedLang
                )

                if (uploadResponse.isSuccessful) {
                    val fileId = uploadResponse.body()?.id ?: return@launch
                    val lang = quizQuestionRepo.getLangByLangName(state.selectedLang)
                    val userSettings = userRepo.getUserSettingsByLang(user = userRepo.getCurrentUser(), lang.id)

                    // 2. Сразу запускаем генерацию для этого ID
                    val genResponse = quizQuestionRepo.generateTasks(
                        fileId = fileId,
                        count = state.taskCount,
                        difficulty = userSettings.langLvl,
                    )

                    if (genResponse.isSuccessful) {
                        sharedViewModel.setCurrentFileId(fileId)
                        _events.send(PracticeMenuEvent.ShowToast("Все готово!"))
                        _events.send(PracticeMenuEvent.NavigateToPracticeQuiz)
                    }
                } else {
                    _events.send(PracticeMenuEvent.ShowToast("Ошибка при загрузке на сервер"))
                }
            } catch (e: Exception) {
                _events.send(PracticeMenuEvent.ShowToast("Ошибка сети: ${e.message}"))
            }
        }
    }

    fun showToast(message : String){
        sendEvent(PracticeMenuEvent.ShowToast(message))
    }

    fun navigateToPracticeQuiz(){
        sendEvent(PracticeMenuEvent.NavigateToPracticeQuiz)
    }

    fun navigateToMenu(){
        sendEvent(PracticeMenuEvent.NavigateToMenu)
    }

    private fun sendEvent(event: PracticeMenuEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    fun prepareFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch

                // Получаем имя файла из URI (опционально)
                val fileName = "code_file.txt"

                _uiState.update { it.copy(
                    selectedFileBytes = bytes,
                    selectedFileName = fileName,
                    isFileSelected = true
                )}
                _events.send(PracticeMenuEvent.ShowToast("Файл выбран: $fileName"))
            } catch (e: Exception) {
                _events.send(PracticeMenuEvent.ShowToast("Ошибка чтения файла"))
            }
        }
    }

    data class PracticeMenuUiState(
        val selectedFileBytes: ByteArray? = null,
        val selectedFileName: String? = null,
        val listOfLangs: List<Lang> = emptyList(),
        val selectedLang: String = "",
        val taskCount: Int = 10,
        val currentFileId: Int? = null, // Сюда сохраним ID после загрузки
        val isFileSelected: Boolean = false
    )


    sealed class PracticeMenuEvent {
        data class ShowToast(val message: String) : PracticeMenuEvent()
        object NavigateToMenu : PracticeMenuEvent()
        object NavigateToPracticeQuiz : PracticeMenuEvent()
        data class ShowValidationFeedbackError(val state:String):PracticeMenuEvent()
    }
}

