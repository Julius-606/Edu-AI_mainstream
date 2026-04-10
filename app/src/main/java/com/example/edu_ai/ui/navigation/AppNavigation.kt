package com.example.edu_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.edu_ai.ui.screens.LoginScreen
import com.example.edu_ai.ui.screens.student.ModuleScreen
import com.example.edu_ai.ui.screens.student.StudentDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherDashboard

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "Teacher") {
                        navController.navigate("teacher_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("student_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("student_dashboard") {
            StudentDashboard(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("student_dashboard") { inclusive = true }
                    }
                },
                onLaunchModule = {
                    navController.navigate("module_screen")
                }
            )
        }

        composable("module_screen") {
            ModuleScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("teacher_dashboard") {
            TeacherDashboard(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("teacher_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
