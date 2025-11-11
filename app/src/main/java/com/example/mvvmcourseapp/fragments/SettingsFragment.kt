package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.adapters.LangButtonAdapter
import com.example.mvvmcourseapp.adapters.LangLvlAdapter
import com.example.mvvmcourseapp.databinding.FragmentSettingsBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.SettingsViewModel
import com.example.mvvmcourseapp.viewModels.StatisticsViewModel
import kotlinx.coroutines.launch

class SettingsFragment: Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var langLvlAdapter: LangLvlAdapter
    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[SettingsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding= FragmentSettingsBinding.bind(view)

        setupUI()
        setuoObservers()

    }

    private fun setupUI()
    {
        binding.logoutButton.setOnClickListener {
            settingsViewModel.logout()
        }
        binding.langLvlRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        langLvlAdapter = LangLvlAdapter()
        binding.saveButton.setOnClickListener {
            settingsViewModel.loadNewSettings(binding.editTextNewQuestions.text.toString().toInt(), binding.editTextRepeatableQuestions.text.toString().toInt(), langLvlAdapter.data)
        }
        binding.langLvlRecyclerView.adapter = langLvlAdapter
        binding.backToMenu.setOnClickListener {
            settingsViewModel.navigateToMenu()
        }
    }

    private fun showToast(text:String)
    {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
    private fun setuoObservers()
    {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                settingsViewModel.events.collect { event ->
                    when (event)
                    {
                        is SettingsViewModel.SettingsEvent.NavigateToMenu -> findNavController().navigate(R.id.action_settings_to_menu)
                        is SettingsViewModel.SettingsEvent.ShowError -> showToast(event.error)
                        is SettingsViewModel.SettingsEvent.ShowValidationFeedbackSuccess -> {
                            showFeedbackWithAnimation(R.drawable.success_background_modern, event.state)
                        }

                        is SettingsViewModel.SettingsEvent.ShowValidationFeedbackError -> {
                            showFeedbackWithAnimation(R.drawable.error_background_modern, event.state)
                        }
                    }

                }

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.STARTED)){
                settingsViewModel.uiState.collect { state ->
                    binding.editTextNewQuestions.setText(state.newQuestionsInQuiz.toString())
                    binding.editTextRepeatableQuestions.setText(state.repeatableQuestionsInQuiz.toString())
                    langLvlAdapter.data=state.listOfLanglLvls
                    Log.d("LISTOFLANGLVLS11", state.listOfLanglLvls.toString())
                }
            }
        }
    }

    private fun showFeedbackWithAnimation(backgroundRes: Int, text: String) {
        binding.validationFeedBack.setBackgroundResource(backgroundRes)
        binding.validationFeedBack.text = text

        // Анимация появления с масштабированием
        binding.validationFeedBack.alpha = 0f
        binding.validationFeedBack.scaleX = 0.8f
        binding.validationFeedBack.scaleY = 0.8f
        binding.validationFeedBack.visibility = View.VISIBLE

        binding.validationFeedBack.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Автоматическое скрытие через 3 секунды
        binding.validationFeedBack.postDelayed({
            hideFeedbackWithAnimation()
        }, 3000)
    }

    private fun hideFeedbackWithAnimation() {
        binding.validationFeedBack.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.validationFeedBack.visibility = View.GONE
                // Сброс трансформаций
                binding.validationFeedBack.scaleX = 1f
                binding.validationFeedBack.scaleY = 1f
            }
            .start()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}