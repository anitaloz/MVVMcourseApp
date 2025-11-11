package com.example.mvvmcourseapp.data.repositories

import androidx.room.Query
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.dao.QuizQuestionDao
import com.example.mvvmcourseapp.data.models.SRSTools
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.models.UserSettings
import kotlinx.coroutines.flow.Flow

class QuizQuestionRepo(private val dao: QuizQuestionDao)
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



    suspend fun getQuizQuestionByCategoryAndLang(category:Category, lang:Lang):List<QuizQuestion>
    {
        return dao.getQuizQuestionByCategoryAndLang(category.id, lang.id)
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
        val newsrs = if(q!=0) SRSTools(srsTools.id, EFnew, srsTools.n+1, interval, System.currentTimeMillis(), srsTools.qqId, srsTools.userId) else SRSTools(srsTools.id, EFnew, 1, 1, System.currentTimeMillis(), srsTools.qqId, srsTools.userId)
        dao.updateSrsTools(newsrs)
    }

    suspend fun insertSrsTools(srsTools: SRSTools) {
        dao.insertSrsTools(srsTools)
    }


}