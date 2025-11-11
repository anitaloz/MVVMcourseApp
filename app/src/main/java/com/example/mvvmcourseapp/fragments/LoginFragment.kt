package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.viewModels.SharedViewModel
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.databinding.FragmentAuthBinding
import com.example.mvvmcourseapp.databinding.FragmentLoginBinding
import com.example.mvvmcourseapp.viewModels.AuthViewModel
import com.example.mvvmcourseapp.viewModels.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log
import kotlin.text.clear

class LoginFragment : Fragment (R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val loginViewModel: LoginViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[LoginViewModel::class.java]
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.linkToReg.setOnClickListener{ loginViewModel.navigateToAuth()}
        binding.buttonAuth.setOnClickListener {
            authButtonClick()
        }
        binding.backToMenu.setOnClickListener {
            loginViewModel.navigateToMenu()
        }
    }

    private fun authButtonClick()
    {
        val login = binding.userLoginAuth.text.toString().trim()
        val pass = binding.userPassAuth.text.toString().trim()
        loginViewModel.authentication(login, pass)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за событиями
                loginViewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: LoginViewModel.LoginEvent) {
        when (event) {
            is LoginViewModel.LoginEvent.showToast -> {
                showToast(event.message)
            }
            is LoginViewModel.LoginEvent.NavigateToMenu -> {
                navigateToMenu()
            }
            is LoginViewModel.LoginEvent.NavigateToAuth -> {
                navigateToAuth()
            }
            is LoginViewModel.LoginEvent.ShowValidationFeedbackError -> {
                showFeedbackWithAnimation(event.state)
            }
        }
    }
    private fun showFeedbackWithAnimation(text: String) {
        binding.loginValidationFeedBack.text = text

        // Анимация появления с масштабированием
        binding.loginValidationFeedBack.alpha = 0f
        binding.loginValidationFeedBack.scaleX = 0.8f
        binding.loginValidationFeedBack.scaleY = 0.8f
        binding.loginValidationFeedBack.visibility = View.VISIBLE

        binding.loginValidationFeedBack.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    private fun navigateToMenu() {
        binding.apply {
            userPassAuth.text?.clear()
            userLoginAuth.text?.clear()
        }
        findNavController().navigate(R.id.action_login_to_menu)

    }

    private fun navigateToAuth() {
        findNavController().navigate(R.id.action_login_to_auth)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}