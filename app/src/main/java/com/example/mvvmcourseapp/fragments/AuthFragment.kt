package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.PassHash
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.databinding.FragmentAuthBinding
import com.example.mvvmcourseapp.databinding.FragmentMenuBinding
import com.example.mvvmcourseapp.viewModels.AuthViewModel
import com.example.mvvmcourseapp.viewModels.LoginViewModel
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthBinding.bind(view)

        setupViewModel()
        setupViews()
        setupObservers()
    }

    private fun setupViewModel() {
        val appContainer: AppContainer by lazy {
            val application = requireActivity().application as MVVMcourseApplication
            application.appContainer
        }
        authViewModel = ViewModelProvider(this, appContainer.getViewModelFactory())[AuthViewModel::class.java]
    }

    private fun setupViews() {
        binding.buttonReg.setOnClickListener {
            registerUser()
        }

        binding.linkToAuth.setOnClickListener {
            authViewModel.navigateToLogin()
        }
        binding.backToMenu.setOnClickListener {
            authViewModel.navigateToMenu()
        }
    }


    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за событиями
                authViewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }



    private fun handleEvent(event: AuthViewModel.AuthEvent) {
        when (event) {
            is AuthViewModel.AuthEvent.RegistrationSuccess -> {
                showToast(event.message)
                clearForm()
                findNavController().navigate(R.id.action_auth_to_login)
            }
            is AuthViewModel.AuthEvent.ShowError -> {
                showToast(event.message)
                clearPasswordField() // Очищаем пароль для безопасности
            }
            is AuthViewModel.AuthEvent.NavigateToLogin -> {
                findNavController().navigate(R.id.action_auth_to_login)
            }
            is AuthViewModel.AuthEvent.ShowValidationFeedbackError ->{
                showFeedbackWithAnimation(R.drawable.error_background_modern, event.state)
            }
            is AuthViewModel.AuthEvent.ShowValidationFeedbackSuccess ->{
               showFeedbackWithAnimation(R.drawable.success_background_modern, event.state)
            }
            is AuthViewModel.AuthEvent.NavigateToMenu -> {
                findNavController().navigate(R.id.action_auth_to_menu)
            }

        }
    }

    private fun registerUser() {
        val login = binding.userLogin.text.toString().trim()
        val email = binding.userEmail.text.toString().trim()
        val password = binding.userPassAuth.text.toString().trim()

        authViewModel.registerUser(login, email, password)
    }

    private fun clearForm() {
        binding.userLogin.text?.clear()
        binding.userEmail.text?.clear()
        binding.userPassAuth.text?.clear()
    }

    private fun showFeedbackWithAnimation(res:Int, text: String) {
        binding.authValidationFeedBack.text = text
        binding.authValidationFeedBack.setBackgroundResource(res)
        // Анимация появления с масштабированием
        binding.authValidationFeedBack.alpha = 0f
        binding.authValidationFeedBack.scaleX = 0.8f
        binding.authValidationFeedBack.scaleY = 0.8f
        binding.authValidationFeedBack.visibility = View.VISIBLE

        binding.authValidationFeedBack.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    private fun clearPasswordField() {
        binding.userPassAuth.text?.clear()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}