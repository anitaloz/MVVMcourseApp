package com.example.mvvmcourseapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mvvmcourseapp.AppContainer
import com.example.mvvmcourseapp.MVVMcourseApplication
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.UIhelper.StatisticsView
import com.example.mvvmcourseapp.adapters.LangLvlAdapter
import com.example.mvvmcourseapp.adapters.StatisticsAdapter
import com.example.mvvmcourseapp.databinding.FragmentStatisticsBinding
import com.example.mvvmcourseapp.viewModels.MenuViewModel
import com.example.mvvmcourseapp.viewModels.SettingsViewModel
import com.example.mvvmcourseapp.viewModels.StatisticsViewModel
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {
    private var _binding: FragmentStatisticsBinding? = null
    val binding get() = _binding!!
    private lateinit var statisticsAdapter: StatisticsAdapter
    private val appContainer: AppContainer by lazy {
        val application = requireActivity().application as MVVMcourseApplication
        application.appContainer
    }
    private val statisticsViewModel: StatisticsViewModel by lazy {
        ViewModelProvider(this, appContainer.getViewModelFactory())[StatisticsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding= FragmentStatisticsBinding.bind(view)

        setupUI()
        setuoObservers()
    }

    fun setupUI()
    {
        binding.backToMenu.setOnClickListener {
            statisticsViewModel.navigateToMenu()
        }
        binding.statRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
        statisticsAdapter = StatisticsAdapter { selectedItem, statisticsView ->
            statisticsViewModel.onCategorySelected(selectedItem, statisticsView)
        }
        binding.statRecyclerView.adapter=statisticsAdapter
    }

    fun setuoObservers()
    {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                statisticsViewModel.uiState.collect { state ->
                    statisticsAdapter.data=state.listOfStaticsView

                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                statisticsViewModel.events.collect{event->
                    when(event){
                        is StatisticsViewModel.StatisticsEvent.NavigateToMenu -> findNavController().navigate(R.id.action_statistics_to_menu)
                        is StatisticsViewModel.StatisticsEvent.ShowError -> showToast(event.error)
                    }
                }
            }
        }
    }

    fun showToast(s:String)
    {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }

}