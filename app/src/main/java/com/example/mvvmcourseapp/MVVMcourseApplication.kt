package com.example.mvvmcourseapp

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.data.services.ApiService
import com.example.mvvmcourseapp.data.services.AuthInterceptor
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.viewModels.ViewModelFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MVVMcourseApplication: Application() {
     val db: MainDb by lazy { MainDb.getDb(this) }
    val appContainer: AppContainer by lazy { AppContainer(this, db) }

}

class AppContainer(private val application: Application, private val db: MainDb) {


    val quizQuestionRepo: QuizQuestionRepo by lazy { QuizQuestionRepo(db.getQuizQuestionDao()) }
    val sessionManager: SessionManager by lazy { SessionManager(application) }

    val userRepo: UserRepo by lazy { UserRepo(db.getDao(), api, sessionManager) }

    val sharedViewModel: SharedViewModel by lazy {
        val factory = viewModelFactory {
            initializer { SharedViewModel(application) }
        }
        ViewModelProvider(ViewModelStore(), factory)[SharedViewModel::class.java]
    }

    private val BASE_URL = "http://10.0.2.2:8000/api/v1/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(logging)
        .build()


    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
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
