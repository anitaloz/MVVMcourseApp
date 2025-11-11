package com.example.mvvmcourseapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.AuthViewModel.AuthEvent
import com.example.mvvmcourseapp.viewModels.SettingsViewModel.SettingsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val userRepo: UserRepo,
    private val sessionManager: SessionManager,
    private val sharedViewModel: SharedViewModel
) : ViewModel()
{
    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun authentication(login:String, pass:String){
        if (login.isEmpty() || pass.isEmpty()) {
            showValidationFeedbackError("Заполните все поля")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginSuccess=userRepo.login(login, pass)
                val userLogged=userRepo.getUserByLogin(login)
                withContext(Dispatchers.Main) {
                    if (loginSuccess) {
                        sendEvent(LoginEvent.showToast("Добро пожаловать, $login!"))
                        navigateToMenu()
                        if(userLogged!=null) {
                            sharedViewModel.setUser(userLogged)
                            sessionManager.saveAuthToken(userLogged.login)
                        }
                    } else {
                        showValidationFeedbackError("Неверное имя пользователя или пароль")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendEvent(LoginEvent.showToast("Ошибка: ${e.message}"))
                }
            }
        }
    }
    fun showValidationFeedbackError(state:String)
    {
        sendEvent(LoginEvent.ShowValidationFeedbackError(state))
    }
    fun navigateToAuth(){
        sendEvent(LoginEvent.NavigateToAuth)
    }
    fun navigateToMenu(){
        sendEvent(LoginEvent.NavigateToMenu)
    }
    private fun sendEvent(event: LoginEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    sealed class LoginEvent {
        data class showToast(val message: String) : LoginEvent()
        object NavigateToAuth : LoginEvent()
        object NavigateToMenu : LoginEvent()
        data class ShowValidationFeedbackError(val state:String):LoginEvent()
    }

}