package com.anonym239.flohmarkt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anonym239.flohmarkt.ui.screens.detail.DetailScreen
import com.anonym239.flohmarkt.ui.screens.favorites.FavoritesScreen
import com.anonym239.flohmarkt.ui.screens.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Detail : Screen("detail/{entryId}") {
        fun createRoute(entryId: String) = "detail/$entryId"
    }
}

@Composable
fun FlohmarktNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { entryId ->
                    navController.navigate(Screen.Detail.createRoute(entryId))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { entryId ->
                    navController.navigate(Screen.Detail.createRoute(entryId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            DetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
