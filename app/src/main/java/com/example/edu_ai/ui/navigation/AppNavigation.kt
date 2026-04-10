// #file app/src/main/java/com/example/edu_ai/ui/navigation/AppNavigation.kt
// #version 1.0.2
// #The traffic cop routing users to either the Admin or Worker dashboard.

package com.example.edu_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.edu_ai.ui.screens.LoginScreen
import com.example.edu_ai.ui.screens.student.StudentDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherDashboard

@Composable
fun AppNavigation() {
    // This is our GPS for the app
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        
        // 🛑 Route 1: The Front Door
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "Teacher") {
                        // Send Admin to the penthouse
                        navController.navigate("teacher_dashboard") {
                            popUpTo("login") { inclusive = true } // Don't let them hit back to login
                        }
                    } else {
                        // Send Student to the trenches
                        navController.navigate("student_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // 📈 Route 2: The Worker (Student) View
        composable("student_dashboard") {
            StudentDashboard(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("student_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 👨‍🏫 Route 3: The Admin (Teacher) View
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