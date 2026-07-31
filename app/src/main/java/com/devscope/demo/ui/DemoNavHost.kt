package com.devscope.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devscope.DevScope

/** Route names in one place, so screens and navigation calls can't drift apart. */
object Routes {
    const val HOME = "home"
    const val USERS = "users"
    const val GREETING = "greeting/{name}"

    fun greeting(name: String) = "greeting/$name"
}

@Composable
fun DemoNavHost() {
    val navController = rememberNavController()

    // One line wires the Nav tab to this NavController.
    remember(navController) { DevScope.trackNavigation(navController); true }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.USERS) { UsersScreen() }
        composable(Routes.GREETING) { entry ->
            GreetingScreen(name = entry.arguments?.getString("name") ?: "?")
        }
    }
}
