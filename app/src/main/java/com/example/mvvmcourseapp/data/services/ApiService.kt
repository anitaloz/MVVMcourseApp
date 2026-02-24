package com.example.mvvmcourseapp.data.services
import com.example.mvvmcourseapp.data.DTO.CategoryResponse
import com.example.mvvmcourseapp.data.DTO.LangResponse
import com.example.mvvmcourseapp.data.DTO.LoginRequest
import com.example.mvvmcourseapp.data.DTO.OptionResponse
import com.example.mvvmcourseapp.data.DTO.QuizQuestionResponse
import com.example.mvvmcourseapp.data.DTO.RefreshRequest
import com.example.mvvmcourseapp.data.DTO.RegisterRequest
import com.example.mvvmcourseapp.data.DTO.SettingsWithLangResponse
import com.example.mvvmcourseapp.data.DTO.SrsResponse
import com.example.mvvmcourseapp.data.DTO.TokenResponse
import com.example.mvvmcourseapp.data.DTO.UpdateSettingsRequest
import com.example.mvvmcourseapp.data.DTO.UpdateSrsRequest
import com.example.mvvmcourseapp.data.DTO.UserResponse
import com.example.mvvmcourseapp.data.DTO.UserSettingsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------- USER ----------
    @POST("users/register/")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>

    @POST("users/login/")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("users/me/")
    suspend fun getMe(): UserResponse

    @POST("users/token/refresh/")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse



    // ---------- SETTINGS ----------
    @GET("settings/")
    suspend fun getUserSettings(): UserSettingsResponse

    @PATCH("settings/")
    suspend fun updateUserSettings(@Body body: UpdateSettingsRequest): UserSettingsResponse

    @GET("settings/full/")
    suspend fun getSettingsWithLang(): SettingsWithLangResponse

    @GET("settings/")
    suspend fun getSettingsByLang(@Query("lang_id") langId: Int): UserSettingsResponse


    // ---------- LANG ----------
    @GET("lang/")
    suspend fun getLanguages(): List<LangResponse>


    // ---------- CATEGORY ----------
    @GET("categories/")
    suspend fun getCategories(): List<CategoryResponse>


    // ---------- QUIZ ----------
    @GET("quiz/")
    suspend fun getQuestions(): List<QuizQuestionResponse>

    @GET("quiz/options/")
    suspend fun getOptions(): List<OptionResponse>


    // ---------- SRS ----------
    @GET("srs/")
    suspend fun getSrs(): List<SrsResponse>

    @PATCH("srs/{id}/")
    suspend fun updateSrs(
        @Path("id") id: Int,
        @Body body: UpdateSrsRequest
    ): SrsResponse
}
