package com.offgridrescue.app.ui.navigation

sealed class AppScreen(val route: String) {
    data object Home : AppScreen("home")
    data object Emergency : AppScreen("emergency")
    data object Victim : AppScreen("victim")
    data object Rescuer : AppScreen("rescuer")
    data object ResponderDashboard : AppScreen("responder_dashboard")
    data object Settings : AppScreen("settings")
}
