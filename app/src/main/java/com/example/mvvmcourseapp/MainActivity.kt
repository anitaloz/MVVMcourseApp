package com.example.mvvmcourseapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

import com.example.mvvmcourseapp.viewModels.SharedViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sessionManager=SessionManager(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigation()
    }

    private fun setupNavigation() {
        val isLoggedIn = sessionManager.isLoggedIn()

        // Получаем навигационный граф
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        if (isLoggedIn) {
            // Если авторизован - стартуем с MenuFragment
            navGraph.setStartDestination(R.id.menuFragment)
        } else {
            // Если не авторизован - стартуем с LoginFragment
            navGraph.setStartDestination(R.id.loginFragment)
        }

        // Устанавливаем граф
        navController.setGraph(navGraph, null)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}