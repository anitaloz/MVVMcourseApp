package com.example.mvvmcourseapp.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.UIhelper.AnalyticalPieChart
import com.example.mvvmcourseapp.UIhelper.LangLvlView
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import kotlinx.coroutines.supervisorScope

class StatisticsAdapter(private val onCategorySelected: (String, StatisticsView) -> Unit):
    RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {

    var data: List<StatisticsView> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    class StatisticsViewHolder(
        statisticsView: View,
        private val onCategorySelected: (String, StatisticsView) -> Unit
    ) : RecyclerView.ViewHolder(statisticsView) {
        private val chart: AnalyticalPieChart = statisticsView.findViewById(R.id.analyticalPieChart_1)
        private val spinner: Spinner = statisticsView.findViewById(R.id.spinner)

        // Храним текущий StatisticsView чтобы избежать рекурсии
        private var currentStatisticsView: StatisticsView? = null
        private var isSpinnerProgrammaticChange = false

        fun bind(statisticsView: StatisticsView) {
            currentStatisticsView = statisticsView

            // Обновляем chart
            Log.d("ADAPTER", currentStatisticsView.toString())
            chart.setDataChart(
                listOf(
                    Pair(statisticsView.learnedQ, "Выученные вопросы"),
                    Pair(statisticsView.unlearnedQ, "Незнакомые вопросы"),
                    Pair(statisticsView.inProcessQ, "В процессе")
                )
            )
            chart.startAnimation()

            // Настройка адаптера
            val adapter = ArrayAdapter(
                itemView.context,
                R.layout.spinner_item_custom,
                statisticsView.arrayCategories
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom)
            spinner.adapter = adapter

            // ВРЕМЕННО удаляем слушатель
            spinner.onItemSelectedListener = null

            // Устанавливаем позицию ПЕРЕД добавлением слушателя
            val safePosition = statisticsView.selectedIndex.coerceIn(0, statisticsView.arrayCategories.size - 1)
            isSpinnerProgrammaticChange = true
            spinner.setSelection(safePosition, false)

            // Добавляем слушатель ПОСЛЕ установки позиции
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Игнорируем программные изменения
                    if (isSpinnerProgrammaticChange) {
                        isSpinnerProgrammaticChange = false
                        return
                    }

                    val currentView = currentStatisticsView ?: return

                    // Вызываем callback только если позиция изменилась
                    if (position != currentView.selectedIndex) {
                        currentView.selectedIndex = position
                        onCategorySelected(currentView.arrayCategories[position], currentView)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Обработка отсутствия выбора
                }
            }

            // Сбрасываем флаг после завершения bind
            isSpinnerProgrammaticChange = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.statistics_item, parent, false)
        return StatisticsViewHolder(view, onCategorySelected)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        holder.bind(data[position])
    }
}