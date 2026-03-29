package com.example.mvvmcourseapp.fragments

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
import com.example.mvvmcourseapp.adapters.LangButtonAdapter
import com.example.mvvmcourseapp.adapters.ProcessButtonAdapter
import com.example.mvvmcourseapp.databinding.FragmentMenuBinding
import com.example.mvvmcourseapp.databinding.FragmentPracticeMenuBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.PracticeMenuViewModel
import com.example.mvvmcourseapp.viewModels.StatisticsViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class PracticeMenuFragment : Fragment(R.layout.fragment_practice_menu) {
    private var _binding: FragmentPracticeMenuBinding? = null
    private val binding get() = _binding!!

    // Используем ленивую инициализацию адаптера
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }

    private val practiceMenuViewModel: PracticeMenuViewModel by activityViewModels {
        appContainer.getViewModelFactory()
    }

    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            practiceMenuViewModel.prepareFile(uri, requireContext().contentResolver)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPracticeMenuBinding.bind(view)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Инициализируем пустой адаптер
        spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLangName = spinnerAdapter.getItem(position)
                selectedLangName?.let {
                    // ПЕРЕДАЕМ В VIEWMODEL
                    practiceMenuViewModel.onLangSelected(it)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.buttonDownloadCode.setOnClickListener {
            // Теперь выбор файла просто запускает процесс,
            // а ViewModel сама возьмет язык и сложность из своего State
            selectFileLauncher.launch("text/*")
        }

        binding.backToMenu.setOnClickListener {
            practiceMenuViewModel.navigateToMenu()
        }

        binding.buttonBegin.setOnClickListener {
            practiceMenuViewModel.uploadAndGenerate()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                practiceMenuViewModel.uiState.collect { state ->
                    // 1. Получаем список названий языков из объектов Lang
                    val langNames = state.listOfLangs.map { it.langName } // Предположим, у Lang есть поле name

                    // 2. Обновляем адаптер
                    spinnerAdapter.clear()
                    spinnerAdapter.addAll(langNames)
                    spinnerAdapter.notifyDataSetChanged()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                practiceMenuViewModel.events.collect{event->
                    when(event){
                        PracticeMenuViewModel.PracticeMenuEvent.NavigateToMenu -> findNavController().navigate(R.id.action_practice_menu_to_menu)
                        is PracticeMenuViewModel.PracticeMenuEvent.ShowValidationFeedbackError -> TODO()
                        is PracticeMenuViewModel.PracticeMenuEvent.ShowToast -> showToast(message = event.message)
                        PracticeMenuViewModel.PracticeMenuEvent.NavigateToPracticeQuiz ->  findNavController().navigate(R.id.action_practice_menu_to_practice_quiz)
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
        _binding = null
    }
}