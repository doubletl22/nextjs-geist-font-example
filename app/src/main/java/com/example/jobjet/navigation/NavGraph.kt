package com.example.jobjet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.jobjet.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object JobDetail : Screen("job_detail/{jobId}") {
        const val ARG_JOB_ID = "jobId"
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object JobPost : Screen("job_post")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(
                navArgument(Screen.JobDetail.ARG_JOB_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString(Screen.JobDetail.ARG_JOB_ID)
                ?: return@composable
            JobDetailScreen(jobId = jobId, navController = navController)
        }
        
        composable(Screen.Chat.route) {
            ChatScreen(navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        
        composable(Screen.JobPost.route) {
            JobPostScreen(navController)
        }
    }
}
