package com.example.mvvmcourseapp.viewModels

import android.util.Log
import androidx.compose.runtime.State
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.PassHash
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.data.DTO.RegisterRequest
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.User
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


class AuthViewModel(
    private val userRepo: UserRepo,
) : ViewModel()
{

    private val _events = Channel<AuthEvent>()
    val events = _events.receiveAsFlow()

    fun registerUser(login: String, email: String, password: String) {
        if (login.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showValidationFeedbackError("Заполните все поля")
            return
        }
        if (password.length < 6) {
            showValidationFeedbackError("Пароль должен содержать минимум 6 символов")
            return
        }
        if(!isValidEmail(email))
        {
            showValidationFeedbackError("Почта введена некорректно")
            return
        }
        viewModelScope.launch {
            try {
                val success = userRepo.register(login, email, password)

                if (!success) {
                    showValidationFeedbackError("Пользователь уже существует")
                } else {
                    showError("Пользователь успешно зарегистрирован")
                    navigateToLogin()
                }

            } catch (e: Exception) {
                sendEvent(AuthEvent.ShowError("Ошибка регистрации: ${e.message}"))
            }
        }

    }
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
        return email.matches(emailRegex.toRegex())
    }
    fun navigateToLogin() {
        sendEvent(AuthEvent.NavigateToLogin)
    }

    fun showValidationFeedbackSuccess(state:String)
    {
        sendEvent(AuthEvent.ShowValidationFeedbackSuccess(state))
    }

    fun showValidationFeedbackError(state:String)
    {
        sendEvent(AuthEvent.ShowValidationFeedbackError(state))
    }

    private fun sendEvent(event: AuthEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
    fun navigateToMenu()
    {
        sendEvent(AuthEvent.NavigateToMenu)
    }
    fun showError(err:String)
    {
        sendEvent(AuthEvent.ShowError(err))
    }

    sealed class AuthEvent {
        data class RegistrationSuccess(val message: String) : AuthEvent()
        data class ShowError(val message: String) : AuthEvent()
        object NavigateToLogin : AuthEvent()
        object NavigateToMenu: AuthEvent()
        data class ShowValidationFeedbackSuccess(val state:String):AuthEvent()
        data class ShowValidationFeedbackError(val state:String):AuthEvent()
    }
}