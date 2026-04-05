package com.example.mvvmcourseapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.example.mvvmcourseapp.adapters.LangButtonAdapter
import com.example.mvvmcourseapp.adapters.ProcessButtonAdapter
import com.example.mvvmcourseapp.databinding.FragmentMenuBinding
import com.example.mvvmcourseapp.databinding.FragmentPracticeMenuBinding
import com.example.mvvmcourseapp.databinding.FragmentPracticeQuizBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.PracticeMenuViewModel
import com.example.mvvmcourseapp.viewModels.PracticeQuizViewModel
import com.example.mvvmcourseapp.viewModels.StatisticsViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class PracticeQuizFragment : Fragment(R.layout.fragment_practice_quiz) {
    private var _binding: FragmentPracticeQuizBinding? = null
    private val binding get() = _binding!!

    private var lastLoadedSnippet: String? = null

    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }

    private val practiceQuizViewModel: PracticeQuizViewModel by activityViewModels {
        appContainer.getViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPracticeQuizBinding.bind(view)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.backToMenu.setOnClickListener {
            practiceQuizViewModel.navigateToPracticeMenu()
        }

        binding.btnVerify.setOnClickListener {
            val userCode = binding.codeEditText.text.toString()
            practiceQuizViewModel.onActionClicked(userCode)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                practiceQuizViewModel.uiState.collect { state ->
                    // Соответствие ID: correct_count и wrong_count
                    binding.correctCount.text = state.correctAns
                    binding.wrongCount.text = state.wrongAns

                    // Соответствие ID: question_count
                    binding.questionCount.text = "Задание ${state.questionNumber}/${state.totalQuestions}"

                    // Логика кнопки btnVerify
                    binding.btnVerify.text = if (state.isVerified) "Далее" else "Проверить ответ"

                    if (state.isVerified) {
                        binding.btnVerify.text = "Далее"

                        if (!state.isCorrect) {
                            // Показываем подсказку, если ответ неверный
                            binding.correctCodeTitle.visibility = View.VISIBLE
                            binding.correctCodeTextView.visibility = View.VISIBLE

                            val task = state.currentQuestion
                            if (task != null) {
                                // Генерируем правильный полный код (заменяя битую строку на верную)
                                // Обычно сервер присылает готовый вариант или мы собираем его сами
                                val correctFullCode = task.fullSnippet.replace(task.brokenLine, task.correctAnswer)
                                displayCorrectCode(correctFullCode, task.correctAnswer)
                            }
                        } else {
                            // Если ответ верный, скрываем подсказку (на случай, если она была на прошлом вопросе)
                            binding.correctCodeTitle.visibility = View.GONE
                            binding.correctCodeTextView.visibility = View.GONE
                        }

                        // Блокируем EditText и красим его
                        binding.codeEditText.isEnabled = false
                        binding.codeEditText.setBackgroundColor(
                            if (state.isCorrect) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")
                        )
                    } else {
                        // СБРОС СОСТОЯНИЯ для нового вопроса
                        binding.btnVerify.text = "Проверить ответ"
                        binding.correctCodeTitle.visibility = View.GONE
                        binding.correctCodeTextView.visibility = View.GONE
                        binding.codeEditText.isEnabled = true
                        binding.codeEditText.setBackgroundColor(Color.parseColor("#F5F5F5"))

                        if (state.currentQuestion?.fullSnippet != lastLoadedSnippet) {
                            binding.codeEditText.setText(state.currentQuestion?.fullSnippet)
                            lastLoadedSnippet = state.currentQuestion?.fullSnippet
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                practiceQuizViewModel.events.collect { event ->
                    when (event) {
                        is PracticeQuizViewModel.PracticeQuizEvent.NavigateToPracticeMenu ->
                            findNavController().navigate(R.id.action_practice_quiz_to_practice_menu)

                        is PracticeQuizViewModel.PracticeQuizEvent.NavigateToResults -> {
                            showToast("Поздравляем! Практика завершена.")
                            findNavController().navigate(R.id.action_practice_quiz_to_practice_results)
                        }

                        is PracticeQuizViewModel.PracticeQuizEvent.ShowToast ->
                            showToast(event.message)

                        else -> {}
                    }
                }
            }
        }
    }

    private fun displayCorrectCode(fullCode: String, highlightedPart: String) {
        val spannable = SpannableString(fullCode)
        val startIndex = fullCode.indexOf(highlightedPart)

        if (startIndex != -1) {
            val endIndex = startIndex + highlightedPart.length
            // Подсвечиваем только исправленную часть зеленым цветом
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#2E7D32")), // Темно-зеленый
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // Можно также сделать текст жирным
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.correctCodeTextView.text = spannable
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}