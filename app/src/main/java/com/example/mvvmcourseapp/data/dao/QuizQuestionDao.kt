package com.example.mvvmcourseapp.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.models.SRSTools
import kotlinx.coroutines.flow.Flow
import java.sql.Time
import kotlin.time.TimeSource

@Dao
interface QuizQuestionDao {
    @Query("SELECT * FROM quiz_questions")
    fun getAllQuestions(): Flow<List<QuizQuestion>>

    @Query("SELECT EXISTS(SELECT * FROM quiz_questions LIMIT 1)")
    suspend fun isNotEmptyQuestions(): Boolean

    @Query("SELECT EXISTS(SELECT * FROM lang LIMIT 1)")
    suspend fun isNotEmptyLang(): Boolean

    @Query("SELECT EXISTS(SELECT * FROM options LIMIT 1)")
    suspend fun isNotEmptyOptions(): Boolean

    @Query("SELECT lang_name FROM lang")
    fun getLangsName():Flow<List<String>>

    @Query("SELECT lang_name FROM lang WHERE id=:id")
    suspend fun  getLangNameByLangId(id:Int): String

    @Query("SELECT * FROM lang")
    fun getLangs():Flow<List<Lang>>

    @Query("SELECT * FROM lang WHERE lang_name= :langName LIMIT 1")
    fun getLangByLangName(langName:String):Lang

    @Query("SELECT category_name FROM categories WHERE lang_id = :langId AND category_name != 'Тест на определение уровня'")
    suspend fun getCategoriesByLangId(langId: Int): MutableList<String>

    @Query("SELECT*FROM categories WHERE category_name= :categoryName AND lang_id= :langId LIMIT 1")
    fun getCategoryByNameAndLang(categoryName:String, langId:Int):Category

    @Query("SELECT*FROM categories WHERE category_name= :categoryName LIMIT 1")
    fun getCategoryByName(categoryName:String):Category
    @Query("SELECT EXISTS(SELECT * FROM categories LIMIT 1)")
    suspend fun isNotEmptyCategories(): Boolean

    @Query("""
        SELECT 
            c.category_name,
            l.lang_name,
            COUNT(q.id) AS cardsCount
        FROM categories c 
        JOIN lang l ON c.lang_id = l.id
        LEFT JOIN quiz_questions q ON q.category_id = c.id AND q.language_id = l.id
        GROUP BY c.id, l.id
""")
    fun categoryFilter(): Flow<List<CategoryFilter>>

    @Query("SELECT id FROM lang WHERE lang_name= :langName LIMIT 1")
    suspend fun getLangIdByLangName(langName:String):Int
    @Query("SELECT * FROM options WHERE quiz_question_id= :quizQuestionId")
    suspend fun getOptionsByQuizQuestionId(quizQuestionId:Int):List<Option>

    @Query("SELECT COUNT(*) as total_questions FROM quiz_questions qq WHERE category_id= :categoryId")
    suspend fun getTotalQuestionsByCategoryAndSRSinterval(categoryId:Int) : Int

    @Query("SELECT COUNT(*) as learned_questions FROM quiz_questions qq join srs_tools srs ON qq.id=srs.qq_id AND srs.interval>= :SRSinterval AND srs.user_id = :userID WHERE category_id= :categoryId")
    suspend fun getLearnedQuestionsByCategoryAndSRSinterval(categoryId:Int, SRSinterval:Int, userID: Int) : Int

    @Query("""
SELECT COUNT(*) as unlearned_questions 
FROM quiz_questions qq 
JOIN categories c ON c.id = category_id  
WHERE category_id = :categoryId 
  AND category_name != "Тест на определение уровня"
  AND NOT EXISTS (
    SELECT 1 FROM srs_tools srs 
    WHERE srs.qq_id = qq.id AND srs.user_id = :userID
  )
""")
    suspend fun getUnlearnedQuestionsByCategoryAndSRSinterval(categoryId:Int, userID: Int) : Int

    @Query("SELECT COUNT(*) as total_questions FROM quiz_questions qq join srs_tools srs ON qq.id=srs.qq_id AND srs.interval>= :SRSintervalDown AND srs.interval< :SRSintervalUp AND srs.user_id = :userID WHERE category_id= :categoryId")
    suspend fun getInProcessQuestionsByCategoryAndSRSinterval(categoryId:Int, SRSintervalDown:Int, SRSintervalUp: Int, userID: Int) : Int

    @Query("SELECT COUNT(*) as learned_questions FROM quiz_questions qq join srs_tools srs ON qq.id=srs.qq_id AND srs.interval>= :SRSinterval AND srs.user_id = :userID WHERE language_id= :langId")
    suspend fun getLearnedQuestionsByLangAndSRSinterval(langId:Int, SRSinterval:Int, userID: Int) : Int

    @Query("""
SELECT COUNT(*) as unlearned_questions 
FROM quiz_questions qq 
JOIN categories c ON c.id = category_id  
WHERE lang_id = :langId
  AND category_name != "Тест на определение уровня"
  AND NOT EXISTS (
    SELECT 1 FROM srs_tools srs 
    WHERE srs.qq_id = qq.id AND srs.user_id = :userID
  )
""")
    suspend fun getUnlearnedQuestionsByLangAndSRSinterval(langId:Int, userID: Int) : Int

    @Query("SELECT COUNT(*) as total_questions FROM quiz_questions qq join srs_tools srs ON qq.id=srs.qq_id AND srs.interval>= :SRSintervalDown AND srs.interval< :SRSintervalUp AND srs.user_id= :userID WHERE language_id= :langId")
    suspend fun getInProcessQuestionsByLangAndSRSinterval(langId:Int, SRSintervalDown:Int, SRSintervalUp: Int, userID: Int) : Int



    @Query("""
SELECT 
    c.category_name,
    l.lang_name,
    COUNT(q.id) AS cardsCount
FROM categories c 
JOIN lang l ON c.lang_id = l.id
LEFT JOIN quiz_questions q ON q.category_id = c.id AND q.language_id = l.id
WHERE l.lang_name LIKE '%' || :searchTerm || '%' COLLATE NOCASE
   OR c.category_name LIKE '%' || :searchTerm || '%' COLLATE NOCASE
GROUP BY c.id, l.id
""")
    fun categoryFilter(searchTerm: String, ): Flow<List<CategoryFilter>>

    @Query("""
SELECT 
    c.category_name,
    l.lang_name,
    COUNT(q.id) AS cardsCount
FROM categories c 
JOIN lang l ON c.lang_id = l.id
LEFT JOIN quiz_questions q ON q.category_id = c.id AND q.language_id = l.id
WHERE l.lang_name LIKE '%' || :searchLangTerm || '%' COLLATE NOCASE
   AND c.category_name LIKE '%' || :searchCategoryTerm || '%' COLLATE NOCASE
GROUP BY c.id, l.id
""")
    fun categoryFilter(searchLangTerm: String, searchCategoryTerm: String): Flow<List<CategoryFilter>>


    @Query("""
SELECT
    c.category_name,
    l.lang_name,
    COUNT(q.id) AS cardsCount
FROM categories c
JOIN lang l ON c.lang_id = l.id
LEFT JOIN quiz_questions q ON q.category_id = c.id AND q.language_id = l.id
WHERE l.lang_name == :searchTerm
GROUP BY c.id, l.id
""")
    fun categoryFilterAccurate(searchTerm: String): Flow<List<CategoryFilter>>

    data class CategoryFilter(
        @ColumnInfo(name = "category_name") val category: String,
        @ColumnInfo(name = "lang_name") val langName: String,
        @ColumnInfo(name = "cardsCount") val cardsCount : Int)
    @Insert
    suspend fun insertQuizQuestion(qq: QuizQuestion)


    @Query("SELECT * FROM quiz_questions WHERE category_id= :categoryID AND language_id= :langID")
    suspend fun getQuizQuestionByCategoryAndLang(categoryID:Int, langID:Int):List<QuizQuestion>

    @Query("""
    SELECT q.* FROM quiz_questions q 
    LEFT JOIN srs_tools srs ON q.id = srs.qq_id AND srs.user_id = :userID 
    JOIN user_settings us ON us.user_id = :userID AND us.lang_id = q.language_id
    WHERE q.category_id = :categoryID 
    AND q.language_id = :langID  
    AND q.difficulty <= us.lang_lvl
    AND srs.qq_id IS NULL 
    LIMIT :newQ
""")
    suspend fun getQuizQuestionByCategoryAndLangAndSettingsAndNew(categoryID:Int, langID:Int, newQ:Int, userID: Int):List<QuizQuestion>

    @Query("""
    SELECT q.* FROM quiz_questions q 
    LEFT JOIN srs_tools srs ON q.id = srs.qq_id AND srs.user_id = :userID
    WHERE q.category_id = :categoryID 
    AND q.language_id = :langID
    AND srs.qq_id IS NULL 
    LIMIT :newQ
""")
    suspend fun getQuizQuestionByCategoryAndLangAndNew(categoryID:Int, langID:Int, newQ:Int, userID: Int):List<QuizQuestion>


    @Query("""
    SELECT q.* FROM quiz_questions q 
    JOIN srs_tools srs ON (q.id = srs.qq_id AND srs.user_id = :userID)
    JOIN user_settings us ON (us.user_id = :userID AND us.lang_id = q.language_id)
    WHERE q.category_id = :categoryID 
    AND q.language_id = :langID 
    AND (srs.last_review_date + (srs.interval * 86400000)) <= :currentTime
    AND q.difficulty <= us.lang_lvl
    ORDER BY srs.last_review_date ASC
    LIMIT :maxRepQ
""")
    suspend fun getQuizQuestionByCategoryAndLangAndCurDateAndSettings(categoryID:Int, langID:Int, userID:Int, maxRepQ: Int, currentTime: Long=System.currentTimeMillis()):List<QuizQuestion>

    @Query("""
    SELECT * FROM quiz_questions q 
    JOIN categories c ON q.category_id=c.id
""")
    suspend fun getQuiz():List<QuizQuestion>

    @Query("""
    SELECT q.* FROM quiz_questions q 
    JOIN srs_tools srs ON (q.id = srs.qq_id AND srs.user_id = :userID)
    WHERE q.category_id = :categoryID 
    AND q.language_id = :langID 
    AND (srs.last_review_date + (srs.interval * 86400000)) <= :currentTime
    ORDER BY srs.last_review_date ASC
    LIMIT :maxRepQ
""")
    suspend fun getQuizQuestionByCategoryAndLangAndCurDate(categoryID:Int, langID:Int, userID:Int, maxRepQ: Int, currentTime: Long=System.currentTimeMillis()):List<QuizQuestion>

    @Query("SELECT EXISTS (SELECT * FROM srs_tools WHERE qq_id= :qqId AND user_id= :userID)")
    suspend fun questionIsRepeatable(qqId:Int, userID: Int) : Boolean

    @Query("SELECT * FROM srs_tools WHERE qq_id= :qqId AND user_id= :userID")
    suspend fun getQuestionRepeatable(qqId:Int, userID: Int) : SRSTools
    @Insert
    suspend fun insertAllCategories(categories: List<Category>)

    @Update
    suspend fun updateSrsTools(srsTools: SRSTools)

    @Insert
    suspend fun insertAllLang(lang: List<Lang>)

    @Insert
    suspend fun insertAllQuizQuestions(qq: List<QuizQuestion>)

    @Insert
    suspend fun insertAllOptions(option: List<Option>)

    @Insert
    suspend fun insertLang(lang: Lang)

    @Insert
    suspend fun insertCategories(category: Category)

    @Insert
    suspend fun insertSrsTools(srsTools: SRSTools)

    @Insert
    suspend fun insertOption(option: Option)

    @Update
    suspend fun updateQuizQuestion(qq: QuizQuestion)

    @Delete
    suspend fun deleteQuizQuestion(qq: QuizQuestion)
}