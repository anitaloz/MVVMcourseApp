package com.example.mvvmcourseapp.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.animate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.UIhelper.OptionItem
import com.example.mvvmcourseapp.adapters.OptionItemAdapter
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.databinding.FragmentQuizQuestionBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.QuizViewModel
//import com.example.mvvmcourseapp.viewModels.QuizViewModel
//import com.example.mvvmcourseapp.viewModels.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//TODO: не забыть сделать предупреждение при переходе назад(в меню) результат аннулируется
class QuizQuestionFragment : Fragment(R.layout.fragment_quiz_question) {
    private var _binding: FragmentQuizQuestionBinding?=null

    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val quizViewModel: QuizViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[QuizViewModel::class.java]
    }

    private val binding get() = _binding!!
    private lateinit var optionsAdapter: OptionItemAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizQuestionBinding.bind(view)

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        // Настройка адаптера
        optionsAdapter = OptionItemAdapter { optionItem, position ->
            quizViewModel.onOptionSelected(position, optionItem, binding.timerView.getRemainingTime())
        }
        binding.optionItemRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.optionItemRecycler.adapter = optionsAdapter

        // Таймер
        binding.timerView.setCustomFont(R.font.montserrat_ace_medium)
        binding.timerView.onTimerFinished = {
            quizViewModel.onTimerFinished()
        }

        binding.backToMenu.setOnClickListener {
            quizViewModel.navigateToMenuHandled()
        }
    }

    private fun setupObservers() {
        // Наблюдаем за состоянием UI
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quizViewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quizViewModel.events.collect { event ->
                    when (event) {
                        is QuizViewModel.QuizEvent.NavigateToResults -> {
                            findNavController().navigate(R.id.action_quiz_to_result)
                        }
                        is QuizViewModel.QuizEvent.NavigateToMenu ->
                            findNavController().navigate(R.id.action_quiz_to_menu)
                        is QuizViewModel.QuizEvent.showError -> {
                            Toast.makeText(requireContext(), event.error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderUiState(state: QuizViewModel.QuizUiState) {
        // Показываем вопрос
        state.currentQuestion?.let { question ->
            binding.questionTextView.text = question.questionText
            binding.questionCount.text = getString(
                R.string.question_order,
                state.questionNumber,
                state.totalQuestions
            )
            binding.correctCount.text=state.correctAns
            binding.wrongCount.text=state.wrongAns
        }

        // Обновляем варианты ответов
        optionsAdapter.setData(state.options)

        // Показываем выбранный вариант
        optionsAdapter.setSelectedPosition(state.selectedOptionIndex, state.showFeedback)
        // Запускаем таймер для нового вопроса
        binding.timerView.startTimer(state.timerSeconds, state.currentQuestion, state.isDelayActive)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}