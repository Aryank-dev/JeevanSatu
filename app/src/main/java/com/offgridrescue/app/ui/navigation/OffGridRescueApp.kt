package com.offgridrescue.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.offgridrescue.app.ui.emergency.EmergencyModeRoute
import com.offgridrescue.app.ui.emergency.EmergencyViewModel
import com.offgridrescue.app.ui.home.HomeRoute
import com.offgridrescue.app.ui.rescuer.RescuerModeRoute
import com.offgridrescue.app.ui.rescuer.RescuerViewModel
import com.offgridrescue.app.ui.rescuer.ResponderDashboardRoute
import com.offgridrescue.app.ui.rescuer.ResponderDashboardViewModel
import com.offgridrescue.app.ui.settings.SettingsScreen
import com.offgridrescue.app.ui.victim.VictimModeRoute

@Composable
fun JeevanSetuApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val emergencyViewModel: EmergencyViewModel = viewModel()
    val rescuerViewModel: RescuerViewModel = viewModel()
    val responderViewModel: ResponderDashboardViewModel = viewModel()
    val emergencyUiState by emergencyViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route,
        modifier = modifier
    ) {
        composable(AppScreen.Home.route) {
            HomeRoute(
                emergencyUiState = emergencyUiState,
                onEmergencyClick = { navController.navigate(AppScreen.Emergency.route) },
                onVictimClick = { navController.navigate(AppScreen.Victim.route) },
                onRescuerClick = { navController.navigate(AppScreen.Rescuer.route) },
                onResponderDashboardClick = { navController.navigate(AppScreen.ResponderDashboard.route) },
                onSettingsClick = { navController.navigate(AppScreen.Settings.route) }
            )
        }
        composable(AppScreen.Emergency.route) {
            EmergencyModeRoute(
                viewModel = emergencyViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppScreen.Victim.route) {
            VictimModeRoute(
                viewModel = emergencyViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppScreen.Rescuer.route) {
            RescuerModeRoute(
                viewModel = rescuerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppScreen.ResponderDashboard.route) {
            ResponderDashboardRoute(
                viewModel = responderViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppScreen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
