package com.example.edu_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.ui.screens.LoginScreen
import com.example.edu_ai.ui.screens.student.ModuleScreen
import com.example.edu_ai.ui.screens.student.StudentDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherViewModel
import com.example.edu_ai.ui.screens.teacher.TeacherViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val repository = app.repository

    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, userId ->
                    if (role == "Teacher") {
                        navController.navigate("teacher_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("student_dashboard/$userId") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = "student_dashboard/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "STUDENT_001"
            StudentDashboard(
                userId = userId,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("student_dashboard/$userId") { inclusive = true }
                    }
                },
                onLaunchModule = {
                    navController.navigate("module_screen/$userId")
                }
            )
        }

        composable(
            route = "module_screen/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "STUDENT_001"
            ModuleScreen(
                userId = userId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("teacher_dashboard") {
            val teacherViewModel: TeacherViewModel = viewModel(
                factory = TeacherViewModelFactory(repository)
            )
            TeacherDashboard(
                viewModel = teacherViewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("teacher_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
