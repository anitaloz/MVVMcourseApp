package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.data.models.MainDb
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.ProcessButton
import com.example.mvvmcourseapp.adapters.LangButtonAdapter
import com.example.mvvmcourseapp.adapters.ProcessButtonAdapter
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.databinding.FragmentMenuBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val menuViewModel: MenuViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[MenuViewModel::class.java]
    }

    private lateinit var langAdapter: LangButtonAdapter
    private lateinit var processAdapter: ProcessButtonAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMenuBinding.bind(view)

        setupUI()
        setupObservers()
        setupSearchListener()

    }


    private fun setupUI() {
        binding.langRecyclerViewCategories.layoutManager =
            GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false)

        binding.processRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)

        langAdapter = LangButtonAdapter { langButton ->
            menuViewModel.searchAccurate(langButton.name)
        }

        processAdapter = ProcessButtonAdapter { processButton ->
            menuViewModel.onProcessSelected(processButton)
        }

        binding.langRecyclerViewCategories.adapter = langAdapter
        binding.processRecyclerView.adapter = processAdapter

        binding.imbSettings.setOnClickListener {
            val currentUser = menuViewModel.user.value
            if (currentUser != null) {
                //menuViewModel.logout()
                menuViewModel.navigateToSettings()
                //updateLoginUI(null)
            } else {
                menuViewModel.navigateToLogin()
            }
        }

        binding.imbStatistics.setOnClickListener {
            val currentUser = menuViewModel.user.value
            if (currentUser != null) {
                //menuViewModel.logout()
                menuViewModel.navigateToStatistics()
                //updateLoginUI(null)
            } else {
                menuViewModel.navigateToLogin()
            }
        }

        binding.tvLogin.setOnClickListener {
            menuViewModel.navigateToLogin()
        }

    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                menuViewModel.uiState.collect { state ->
                    binding.content.visibility = if (state.loading) View.GONE else View.VISIBLE
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    if (state.userLoading) {
                        binding.tvLogin.text = "Загрузка..."
                    }
                    langAdapter.data = state.langButtons
                    langAdapter.notifyDataSetChanged()
                    processAdapter.data = state.processButtons
                    processAdapter.notifyDataSetChanged()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                menuViewModel.events.collect{event->
                    when(event){
                        is MenuViewModel.MenuEvent.NavigateToQuiz -> findNavController().navigate(R.id.action_menu_to_quiz)
                        is MenuViewModel.MenuEvent.showError -> showToast(event.error)
                        is MenuViewModel.MenuEvent.NavigateToLogin->findNavController().navigate(R.id.action_menu_to_login)
                        is MenuViewModel.MenuEvent.NavigateToSettings->findNavController().navigate(R.id.action_menu_to_settings)
                        is MenuViewModel.MenuEvent.NavigateToStatistics -> findNavController().navigate(R.id.action_menu_to_statistics)
                    }
                }
            }
        }

        // Самая важная подписка - на пользователя!
        menuViewModel.user.observe(viewLifecycleOwner) { user ->
            updateLoginUI(user?.login)
        }

    }

    private fun setupSearchListener() {
        val searchHandler = Handler(Looper.getMainLooper())
        var searchRunnable: Runnable? = null
        val SEARCH_DELAY = 500L

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    menuViewModel.search(s?.toString() ?: "")
                }
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateLoginUI(username: String?) {
        updateLoginUI(username, shouldShowLogoutIcon(username))
    }

    private fun updateLoginUI(username: String?, showLogoutIcon: Boolean) {
        val displayText = if (username.isNullOrEmpty() || username == "Имя пользователя") {
            "Войти"
        } else {
            username
        }

        binding.tvLogin.text = displayText

        if (showLogoutIcon) {
            //binding.imbSettings.setBackgroundResource(R.drawable.settings)
            binding.imbSettings.visibility=View.VISIBLE
            binding.imbStatistics.visibility=View.VISIBLE
        } else {
            //binding.imbSettings.background = null
            binding.imbSettings.visibility=View.GONE
            binding.imbStatistics.visibility=View.GONE
        }
    }

    private fun shouldShowLogoutIcon(username: String?): Boolean {
        return !username.isNullOrEmpty() && username != "Имя пользователя"
    }


    private fun showToast(message: String?) {
        Toast.makeText(requireContext(), message ?: "Неизвестная ошибка", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

