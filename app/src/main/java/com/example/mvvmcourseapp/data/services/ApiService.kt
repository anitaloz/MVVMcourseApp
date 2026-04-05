package com.example.mvvmcourseapp.data.services
import com.example.mvvmcourseapp.data.DTO.CategoryResponse
import com.example.mvvmcourseapp.data.DTO.CreateSrsRequest
import com.example.mvvmcourseapp.data.DTO.GenerateActionResponse
import com.example.mvvmcourseapp.data.DTO.LangResponse
import com.example.mvvmcourseapp.data.DTO.LoginRequest
import com.example.mvvmcourseapp.data.DTO.OptionResponse
import com.example.mvvmcourseapp.data.DTO.QuizQuestionResponse
import com.example.mvvmcourseapp.data.DTO.users.RefreshRequest
import com.example.mvvmcourseapp.data.DTO.users.RegisterRequest
import com.example.mvvmcourseapp.data.DTO.users.SettingsWithLangResponse
import com.example.mvvmcourseapp.data.DTO.SrsResponse
import com.example.mvvmcourseapp.data.DTO.TokenResponse
import com.example.mvvmcourseapp.data.DTO.users.UpdateSettingsRequest
import com.example.mvvmcourseapp.data.DTO.UpdateSrsRequest
import com.example.mvvmcourseapp.data.DTO.UserCodeResponse
import com.example.mvvmcourseapp.data.DTO.users.UserResponse
import com.example.mvvmcourseapp.data.DTO.users.UserSettingsResponse
import com.example.mvvmcourseapp.data.models.UserSettings
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------- USER ----------
    @POST("users/register/")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>

    @POST("users/login/")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("users/me/")
    suspend fun getMe(): Response<UserResponse>

    @POST("users/token/refresh/")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse



    // ---------- SETTINGS ----------
    @GET("user_settings/")
    suspend fun getUserSettings(): Response<List<UserSettingsResponse>>

    @PATCH("user_settings/update/")
    suspend fun updateUserSettings(@Body body: UpdateSettingsRequest): Response<UserSettingsResponse>

    @GET("settings/full/")
    suspend fun getSettingsWithLang(): SettingsWithLangResponse

    @GET("settings/")
    suspend fun getSettingsByLang(@Query("lang_id") langId: Int): UserSettingsResponse


    // ---------- LANG ----------
    @GET("langs/")
    suspend fun getLanguages(): Response<List<LangResponse>>


    // ---------- CATEGORY ----------
    @GET("categories/")
    suspend fun getCategories(): Response<List<CategoryResponse>>


    // ---------- QUIZ ----------
    @GET("quiz/")
    suspend fun getQuestions(): Response<List<QuizQuestionResponse>>

    @GET("quiz/options/")
    suspend fun getOptions(): Response<List<OptionResponse>>


    // ---------- SRS ----------
    @GET("srs/")
    suspend fun getSrs(): Response<List<SrsResponse>>

    @PATCH("srs/{id}/")
    suspend fun updateSrs(
        @Path("id") id: Long,
        @Body body: UpdateSrsRequest
    ): Response<SrsResponse>

    @POST("srs/")
    suspend fun createSrsItem(
        @Body body: CreateSrsRequest
    ): Response<SrsResponse>

    @Multipart
    @POST("mistakes/files/")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): Response<UserCodeResponse>

    // Шаг 2: Генерация (count передается как ?count=5)
    @POST("mistakes/files/{id}/generate/")
    suspend fun generateTasks(
        @Path("id") fileId: Int,
        @Query("count") count: Int,
        @Query("difficulty") difficulty: Int,
    ): Response<GenerateActionResponse>

    // Шаг 3: Получение списка задач (если нужно обновить список)
    @GET("mistakes/files/{id}/")
    suspend fun getFileDetails(
        @Path("id") fileId: Int
    ): Response<UserCodeResponse>

    @GET("mistakes/tasks")
    suspend fun getTasksWithoutFile(
        @Query("count") count: Int,
        @Query("difficulty") difficulty: Int,
        @Query("lang_id") langId: Int,
    ): Response<UserCodeResponse>
}
