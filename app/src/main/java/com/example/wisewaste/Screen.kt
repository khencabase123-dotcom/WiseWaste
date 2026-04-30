package com.example.wisewaste

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object SubmitReport : Screen("submit_report")
    object MyReports : Screen("my_reports")
    object Education : Screen("education")
    object Campaigns : Screen("campaigns")
    object Leaderboard : Screen("leaderboard")
    object Rewards : Screen("rewards")
    object Profile : Screen("profile")
    object AdminReports : Screen("admin_reports")
}