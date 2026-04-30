package com.example.wisewaste

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wisewaste.RegisterScreen
import com.example.wisewaste.ui.screens.*
import com.example.wisewaste.viewmodel.AuthViewModel
import com.example.wisewaste.viewmodel.CampaignViewModel
import com.example.wisewaste.viewmodel.EducationViewModel
import com.example.wisewaste.viewmodel.ReportViewModel

@Composable
fun WiseWasteNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn = authViewModel.currentUser != null

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SubmitReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            SubmitReportScreen(
                viewModel = reportViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.MyReports.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            MyReportsScreen(
                viewModel = reportViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Education.route) {
            val educationViewModel: EducationViewModel = hiltViewModel()
            EducationScreen(
                viewModel = educationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Campaigns.route) {
            val campaignViewModel: CampaignViewModel = hiltViewModel()
            CampaignScreen(
                viewModel = campaignViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Leaderboard.route) {
            val campaignViewModel: CampaignViewModel = hiltViewModel()
            LeaderboardScreen(
                viewModel = campaignViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Rewards.route) {
            val campaignViewModel: CampaignViewModel = hiltViewModel()
            val authVm: AuthViewModel = hiltViewModel()
            RewardsScreen(
                viewModel = campaignViewModel,
                authViewModel = authVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminReports.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            AdminReportsScreen(
                viewModel = reportViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}