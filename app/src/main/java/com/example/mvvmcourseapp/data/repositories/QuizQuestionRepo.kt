package com.example.mvvmcourseapp.data.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Query
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import com.example.mvvmcourseapp.data.DTO.CreateSrsRequest
import com.example.mvvmcourseapp.data.DTO.GenerateActionResponse
import com.example.mvvmcourseapp.data.DTO.GeneratedTaskResponse
import com.example.mvvmcourseapp.data.DTO.SrsResponse
import com.example.mvvmcourseapp.data.DTO.UpdateSrsRequest
import com.example.mvvmcourseapp.data.DTO.UserCodeResponse
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.dao.QuizQuestionDao
import com.example.mvvmcourseapp.data.models.SRSTools
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.models.UserSettings
import com.example.mvvmcourseapp.data.services.ApiService
import com.example.mvvmcourseapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class QuizQuestionRepo(
    private val dao: QuizQuestionDao,
    private val api: ApiService,
    private val networkUtils: NetworkUtils,
)
{
    fun filter (s:String): Flow<List<QuizQuestionDao.CategoryFilter>>
    {
        lateinit var categoryList : Flow<List<QuizQuestionDao.CategoryFilter>>

        if(s == "")
        {
            categoryList=dao.categoryFilter()
        }
        else
        {
            val new = s.trim()
            if(new.contains(":"))
            {
                val (langPart, categoryPart) = new.split(":").map { it.trim() }
                categoryList=dao.categoryFilter(langPart, categoryPart)
            }
            else categoryList=dao.categoryFilter(new)
        }
        return categoryList
    }

    fun accurateFilter(s:String) : Flow<List<QuizQuestionDao.CategoryFilter>>
    {
        return dao.categoryFilterAccurate(s.trim())
    }

    fun getLangsName():Flow<List<String>>
    {
        return dao.getLangsName()
    }

    fun getLangs():Flow<List<Lang>>
    {
        return dao.getLangs()
    }

    fun getLangByLangName(langName:String):Lang
    {
        return dao.getLangByLangName(langName)
    }

    fun getCategoryByNameAndLang(categoryName:String, lang:Lang):Category{
        return dao.getCategoryByNameAndLang(categoryName, lang.id)
    }



    suspend fun getStatisticsView(category: Category, SRSintervalDown:Int, SRSintervalUp: Int, user: User) : StatisticsView
    {
        return StatisticsView(dao.getLearnedQuestionsByCategoryAndSRSinterval(category.id, SRSintervalUp, user.id!!),
            dao.getUnlearnedQuestionsByCategoryAndSRSinterval(category.id, user.id!!),
            dao.getInProcessQuestionsByCategoryAndSRSinterval(category.id, SRSintervalDown, SRSintervalUp, user.id!!),
            dao.getCategoriesByLangId(category.langId).apply { add(0, dao.getLangNameByLangId(category.langId)) }.toTypedArray(),
            category.langId)
    }

    suspend fun getStatisticsView(lang:Lang, SRSintervalDown:Int, SRSintervalUp: Int, user: User) : StatisticsView
    {
        var l : Array<String> =dao.getCategoriesByLangId(lang.id).apply { add(0, lang.langName) }.toTypedArray()
        return StatisticsView(dao.getLearnedQuestionsByLangAndSRSinterval(lang.id, SRSintervalUp, user.id!!),
            dao.getUnlearnedQuestionsByLangAndSRSinterval(lang.id, user.id!!),
            dao.getInProcessQuestionsByLangAndSRSinterval(lang.id, SRSintervalDown, SRSintervalUp, user.id!!),
            l, lang.id)
    }

    fun getCategoryByName(categoryName:String):Category
    {
        return dao.getCategoryByName(categoryName)
    }
    suspend fun getLangIdByLangName(langName:String):Int
    {
        return dao.getLangIdByLangName(langName)
    }



    suspend fun getQuizQuestionByCategoryAndLang(category:Category):List<QuizQuestion>
    {
        return dao.getQuizQuestionByCategoryAndLang(category.id)
    }

    suspend fun questionIsRepeatable(quizQuestion: QuizQuestion, user: User) : Boolean {
        return dao.questionIsRepeatable(quizQuestion.id, user.id!!)
    }
    suspend fun getQuizQuestionByCategoryAndLangAndCurDate(category:Category, lang:Lang, user: UserSettings):List<QuizQuestion>
    {
        if(user.langId==lang.id && user.langLvl==0)
            return dao.getQuizQuestionByCategoryAndLangAndCurDate(category.id, lang.id, user.userId, user.maxRepQuestions)
        return dao.getQuizQuestionByCategoryAndLangAndCurDateAndSettings(category.id, lang.id, user.userId, user.maxRepQuestions)
    }

    suspend fun getQuestionRepeatable(qqId: QuizQuestion, userID:User) : SRSTools
    {
        return dao.getQuestionRepeatable(qqId.id, userID.id!!)
    }

    suspend fun getQuizQuestionByCategoryAndLangAndNew(category: Category, lang: Lang, u: UserSettings):List<QuizQuestion>
    {
        if(u.langId==lang.id && u.langLvl==0)
            return dao.getQuizQuestionByCategoryAndLangAndNew(category.id, lang.id, u.newQ, u.userId)
        return dao.getQuizQuestionByCategoryAndLangAndSettingsAndNew(category.id, lang.id, u.newQ, u.userId)
    }

    suspend fun getOptionsByQuizQuestion(quizQuestion: QuizQuestion):List<Option>
    {
        return dao.getOptionsByQuizQuestionId(quizQuestion.id)
    }

    suspend fun isNotEmptyQuestions(): Boolean
    {
        return dao.isNotEmptyQuestions()
    }

    suspend fun isNotEmptyLang(): Boolean
    {
        return dao.isNotEmptyLang()
    }

    suspend fun isNotEmptyOptions(): Boolean
    {
        return dao.isNotEmptyOptions()
    }

    suspend fun isNotEmptyCategories(): Boolean
    {
        return dao.isNotEmptyCategories()
    }

    suspend fun isNotEmptyLangs(): Boolean{
        return dao.isNotEmptyLang()
    }

    suspend fun insertAllCategories(categories: List<Category>)
    {
        dao.insertAllCategories(categories)
    }


    suspend fun insertAllLang(lang: List<Lang>)
    {
        dao.insertAllLang(lang)
    }


    suspend fun insertAllQuizQuestions(qq: List<QuizQuestion>)
    {
        dao.insertAllQuizQuestions(qq)
    }


    suspend fun insertAllOptions(option: List<Option>)
    {
        dao.insertAllOptions(option)
    }


    suspend fun insertLang(lang: Lang)
    {
        dao.insertLang(lang)
    }

    suspend fun  getLangNameByLangId(id:Int): String
    {
        return dao.getLangNameByLangId(id)
    }


    suspend fun insertCategories(category: Category)
    {
        dao.insertCategories(category)
    }


    suspend fun insertOption(option: Option)
    {
        dao.insertOption(option)
    }


    suspend fun updateQuizQuestion(qq: QuizQuestion)
    {
        dao.updateQuizQuestion(qq)
    }


    suspend fun deleteQuizQuestion(qq: QuizQuestion)
    {
        dao.deleteQuizQuestion(qq)
    }

    suspend fun updateSrsTools(srsTools: SRSTools, q:Int)
    {
        var interval=srsTools.interval
        val EF=srsTools.EF
        if(srsTools.n==1)
            interval=1
        if(srsTools.n==2)
            interval=6
        else interval=(interval*EF).toInt()
        var EFnew=EF+(0.1-(3-q)*(0.08+(3-q)*0.02))
        if(EFnew<1.3) EFnew=1.3
        val newsrs = if(q!=0) SRSTools(srsTools.id, EFnew, srsTools.n+1,
            interval, System.currentTimeMillis(), srsTools.qqId, srsTools.userId, isDirty = true)
        else SRSTools(srsTools.id, EFnew, 1, 1, System.currentTimeMillis(),
            srsTools.qqId, srsTools.userId)

        if (networkUtils.isNetworkAvailable()) {
            try {
                val response = api.updateSrs(newsrs.id!!, UpdateSrsRequest(
                    newsrs.EF,
                    newsrs.n,
                    newsrs.interval,
                    newsrs.lastReviewDate
                ))

                if (response.isSuccessful) {
                    dao.updateSrsTools(newsrs.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.d("ERROR SERVER", e.message.toString())
            }
        }
        else {
            dao.updateSrsTools(newsrs.copy(isDirty = true))
        }
    }

    suspend fun insertSrsTools(ef: Double, n: Int, interval: Int,lastReviewDate: Long, qqId: Int, userId: Int) {
        if (networkUtils.isNetworkAvailable()) {
            val response = api.createSrsItem(CreateSrsRequest(
                qqId,
                ef,
                n,
                interval,
                lastReviewDate
            ))

            if (response.isSuccessful && response.body() != null) {
                val serverData = response.body()!!

                dao.insertSrsTools(
                    SRSTools(
                        null,
                        ef,
                        n,
                        interval,
                        lastReviewDate,
                        qqId,
                        userId,
                        serverId = serverData.id,
                    )
                )
            }
        } else {
            dao.insertSrsTools(
                SRSTools(
                    null,
                    ef,
                    n,
                    interval,
                    lastReviewDate,
                    qqId,
                    userId,
                    isDirty = true,
                )
            )
        }
    }

    suspend fun refreshCategories() {
        val response = api.getCategories()

        if (response.isSuccessful) {
            val categoriesFromServer = response.body() ?: emptyList()
            val listOfCategories = categoriesFromServer.map {
                it.toCategory()
            }
            try {
                dao.clearAllCategories()
                dao.insertAllCategories(listOfCategories)
            }
            catch (e:Exception)
            {
                Log.d("ERROR DB",e.message.toString())
            }
        }
    }

    suspend fun refreshLangs() {
        val response = api.getLanguages()

        if (response.isSuccessful) {
            val languagesFromServer = response.body() ?: emptyList()
            val listOfLangs = languagesFromServer.map {
                it.toLang()
            }
            dao.clearAllLangs()
            dao.insertAllLang(listOfLangs)
        }
    }

    suspend fun refreshQuizQuestions() {
        val response = api.getQuestions()

        if (response.isSuccessful) {
            val quizQuestionsFromServer = response.body() ?: emptyList()
            val listOfQuizQuestions = quizQuestionsFromServer.map {
                it.toQuizQuestion()
            }
            dao.clearAllQuizQuestions()
            dao.insertAllQuizQuestions(listOfQuizQuestions)
        }
    }

    suspend fun refreshOptions() {
        val response = api.getOptions()

        if (response.isSuccessful) {
            val optionsFromServer = response.body() ?: emptyList()
            val listOfOptions = optionsFromServer.map {
                it.toOption()
            }
            dao.clearAllOptions()
            dao.insertAllOptions(listOfOptions)
        }
    }

    suspend fun refreshSrsToolsWhenUserChanged() {
        if (!networkUtils.isNetworkAvailable()) return

        try {
            val response = api.getSrs()

            if (response.isSuccessful) {
                val srsToolsFromServer = response.body() ?: emptyList()
                val listOfSrs = srsToolsFromServer.map {
                    it.toSrs()
                }
                dao.clearAllSrsTools()
                dao.insertAllSrsTools(listOfSrs)
            }
        }
        catch (e: Exception) {
            Log.e("REFRESHING SRS ERROR", e.message.toString())
        }
    }

    suspend fun deleteSrsTools() {
        dao.clearAllSrsTools()
    }

    // Добавьте withContext(Dispatchers.IO) в начало метода
    suspend fun synchroniseSrsTools() = withContext(Dispatchers.IO) {
        if (!networkUtils.isNetworkAvailable()) return@withContext

        try {
            val response = api.getSrs()
            if (!response.isSuccessful) return@withContext

            val serverTools = response.body() ?: emptyList()
            val localTools = dao.getAllSrsTools()

            val localToolsMap = localTools.associateBy { it.serverId }
            val serverToolsMap = serverTools.associateBy { it.id }

            // Синхронизация dirty записей (локальные изменения -> сервер)
            localTools.filter { it.isDirty }.forEach { localTool ->
                try {
                    val updateResponse = api.updateSrs(
                        localTool.id!!,
                        UpdateSrsRequest(
                            localTool.EF,
                            localTool.n,
                            localTool.interval,
                            localTool.lastReviewDate
                        )
                    )

                    if (updateResponse.isSuccessful) {
                        dao.updateSrsTools(localTool.copy(isDirty = false))
                        Log.d("SYNC", "Successfully synced SRS tool ${localTool.id}")
                    } else {
                        Log.e("SYNC", "Failed to update tool ${localTool.id}: ${updateResponse.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("SYNC", "Network error while updating tool ${localTool.id}", e)
                    // Не снимаем dirty флаг при ошибке сети
                }
            }

            // Подготовка к обновлению локальной БД из сервера
            val toolsToUpdate = mutableListOf<SRSTools>()
            val toolsToInsert = mutableListOf<SRSTools>()

            serverTools.forEach { serverTool ->
                val localTool = localToolsMap[serverTool.id]

                when {
                    localTool == null -> {
                        // Новый инструмент на сервере - добавляем
                        toolsToInsert.add(
                            SRSTools(
                                serverId = serverTool.id,
                                EF = serverTool.ef,
                                n = serverTool.repetition_count,
                                interval = serverTool.interval,
                                lastReviewDate = serverTool.last_review_date,
                                isDirty = false,
                                id = null,
                                qqId = serverTool.question,
                                userId = serverTool.user,
                            )
                        )
                    }
                    !localTool.isDirty -> {
                        // Локальный инструмент не изменялся - обновляем из сервера
                        toolsToUpdate.add(
                            localTool.copy(
                                EF = serverTool.ef,
                                n = serverTool.repetition_count,
                                interval = serverTool.interval,
                                lastReviewDate = serverTool.last_review_date,
                                isDirty = false
                            )
                        )
                    }
                    // Если localTool.isDirty == true, оставляем локальную версию (конфликт в пользу локальных данных)
                }
            }

            // Удаляем инструменты, которых нет на сервере (только если они не dirty)
            val toolsToDelete = localTools.filter {
                it.serverId != null && !serverToolsMap.containsKey(it.serverId) && !it.isDirty
            }

            // Применяем все изменения в одной транзакции
            if (toolsToInsert.isNotEmpty() || toolsToUpdate.isNotEmpty() || toolsToDelete.isNotEmpty()) {
                dao.runInTransaction {
                    toolsToInsert.forEach { dao.insertSrsTools(it) }
                    toolsToUpdate.forEach { dao.updateSrsTools(it) }
                    toolsToDelete.forEach { dao.deleteSrsTool(it) }
                }

                Log.d("SYNC", "SRS Sync completed: inserted=${toolsToInsert.size}, " +
                        "updated=${toolsToUpdate.size}, deleted=${toolsToDelete.size}")
            } else {
                Log.d("SYNC", "No SRS changes needed")
            }

        } catch (e: Exception) {
            Log.e("SYNC", "SRS Synchronization failed", e)
        }
    }
    suspend fun uploadFile(bytes: ByteArray, fileName: String, language: String): Response<UserCodeResponse> {
        val requestFile = RequestBody.create("text/plain".toMediaTypeOrNull(), bytes)
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
        val langPart = language.toRequestBody("text/plain".toMediaTypeOrNull())

        return api.uploadFile(filePart, langPart)
    }

    suspend fun generateTasks(fileId: Int, count: Int, difficulty: Int): Response<GenerateActionResponse> {
        return api.generateTasks(fileId, count, difficulty)
    }

    suspend fun getTasksWithoutFile(fileId: Int, count: Int, difficulty: Int): Response<GenerateActionResponse> {
        return api.generateTasks(fileId, count, difficulty)
    }

    suspend fun getPracticeTasks(fileId: Int) : Response<UserCodeResponse> {
       return api.getFileDetails(fileId)
    }

    suspend fun getPracticeTasks(difficulty: Int, count: Int, langId: Int) : Response<UserCodeResponse> {
        return api.getTasksWithoutFile(difficulty = difficulty, count = count, langId = langId)
    }
}























