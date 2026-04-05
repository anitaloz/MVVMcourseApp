package com.example.mvvmcourseapp.viewModels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.utils.NetworkUtils

object ViewModelFactory {
    fun createFactory(
        application: Application,
        quizQuestionRepo: QuizQuestionRepo,
        userRepo: UserRepo,
        sessionManager: SessionManager,
        sharedViewModel: SharedViewModel,
        networkUtils: NetworkUtils,
    ): ViewModelProvider.Factory = viewModelFactory {

        initializer {
            MenuViewModel(userRepo, quizQuestionRepo, sessionManager, sharedViewModel, networkUtils = networkUtils)
        }

        initializer {
            QuizViewModel(userRepo,quizQuestionRepo,  sharedViewModel)
        }

        initializer {
            AuthViewModel(userRepo)
        }

        initializer {
            LoginViewModel(userRepo, quizQuestionRepo, sharedViewModel)
        }

        initializer {
            QuizResultViewModel(userRepo, quizQuestionRepo, sharedViewModel)
        }

        initializer {
            SettingsViewModel(userRepo, sessionManager, sharedViewModel, networkUtils, quizQuestionRepo)
        }

        initializer {
            StatisticsViewModel(userRepo, sessionManager, sharedViewModel, quizQuestionRepo)
        }

        initializer {
            PracticeMenuViewModel(userRepo, sessionManager, sharedViewModel, quizQuestionRepo)
        }

        initializer {
            PracticeQuizViewModel(userRepo, sharedViewModel, quizQuestionRepo)
        }
    }
}
