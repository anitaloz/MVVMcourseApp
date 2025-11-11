package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.databinding.FragmentQuizQuestionBinding
import com.example.mvvmcourseapp.databinding.FragmentQuizResultBinding
import com.example.mvvmcourseapp.viewModels.LoginViewModel
import com.example.mvvmcourseapp.viewModels.QuizResultViewModel
import com.example.mvvmcourseapp.viewModels.QuizViewModel
import kotlinx.coroutines.launch

class QuizResultFragment:Fragment(R.layout.fragment_quiz_result) {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val quizResultViewModel: QuizResultViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[QuizResultViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizResultBinding.bind(view)

        setupViews()
        setupObservers()

    }

    private fun setupViews()
    {
        binding.buttonToMenu.setOnClickListener{
            quizResultViewModel.navigateToMenuHandled()
        }
    }

    private fun formatScore(score: Int): String {
        return if (score < 10) getString(R.string.ansCount, score) else score.toString()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quizResultViewModel.uiState.collect { state ->
                    binding.textResultCorrect.text = formatScore(state.correctAnsCount)
                    binding.textResultWrong.text = formatScore(state.wrongAnsCount)
                    binding.textResultTotalQuestions.text = formatScore(state.totalQuestions)
                    binding.textResultInPercent.text =
                        getString(R.string.inPercent, state.resultInPercent)
                    binding.bubbleResultPercent.text =
                        getString(R.string.inPercent, state.resultInPercent)
                }

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quizResultViewModel.events.collect { event ->
                    when(event)
                    {
                        is QuizResultViewModel.ResultEvent.NavigateToMenu -> {
                            findNavController().navigate(R.id.action_result_to_menu)
                        }
                        is QuizResultViewModel.ResultEvent.ShowError -> {
                            showToast(event.error)
                        }
                        is QuizResultViewModel.ResultEvent.ShowLvlFeedBack -> {
                            binding.lvlFeedBack.visibility=View.VISIBLE
                            binding.lvlFeedBack.text="Уровень языка ${event.langName} изменен на ${event.lvl}"
                        }
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}