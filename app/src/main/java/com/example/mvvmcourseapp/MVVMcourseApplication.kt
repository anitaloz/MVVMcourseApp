package com.example.mvvmcourseapp

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.viewModels.ViewModelFactory

class MVVMcourseApplication: Application() {
     val db: MainDb by lazy { MainDb.getDb(this) }
    val appContainer: AppContainer by lazy { AppContainer(this, db) }

}

class AppContainer(private val application: Application, private val db: MainDb) {


    val quizQuestionRepo: QuizQuestionRepo by lazy { QuizQuestionRepo(db.getQuizQuestionDao()) }
    val userRepo: UserRepo by lazy { UserRepo(db.getDao()) }
    val sessionManager: SessionManager by lazy { SessionManager(application) }

    val sharedViewModel: SharedViewModel by lazy {
        val factory = viewModelFactory {
            initializer { SharedViewModel(application) }
        }
        ViewModelProvider(ViewModelStore(), factory)[SharedViewModel::class.java]
    }

    fun getViewModelFactory(): ViewModelProvider.Factory {
        return ViewModelFactory.createFactory(
            application = application,
            quizQuestionRepo = quizQuestionRepo,
            userRepo = userRepo,
            sessionManager = sessionManager,
            sharedViewModel = sharedViewModel
        )
    }
}
