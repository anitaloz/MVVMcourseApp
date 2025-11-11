package com.example.mvvmcourseapp.UIhelper

import androidx.room.ColumnInfo

data class StatisticsView (
//    @ColumnInfo("total_questions") val totalQ : Int,
    @ColumnInfo("learned_question") var learnedQ:Int,
    @ColumnInfo("unlearned_question") val unlearnedQ:Int,
    @ColumnInfo("inProcess") var inProcessQ:Int,
    var arrayCategories: Array<String>,
    var langId: Int,
    var selectedIndex: Int = 0,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatisticsView

        if (learnedQ != other.learnedQ) return false
        if (unlearnedQ != other.unlearnedQ) return false
        if (inProcessQ != other.inProcessQ) return false
        if (!arrayCategories.contentEquals(other.arrayCategories)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = learnedQ
        result = 31 * result + unlearnedQ
        result = 31 * result + inProcessQ
        result = 31 * result + arrayCategories.contentHashCode()
        return result
    }
}