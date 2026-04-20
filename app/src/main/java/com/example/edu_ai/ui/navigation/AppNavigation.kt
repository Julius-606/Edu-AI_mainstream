package com.example.edu_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.edu_ai.ui.screens.parent.ParentDashboard // We will create this
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, userId ->
                    val destination = when (role) {
                        "Teacher" -> "teacher_dashboard"
                        "Parent" -> "parent_dashboard/$userId"
                        else -> "student_dashboard/$userId"
                    }
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "student_dashboard/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            StudentDashboard(
                userId = userId,
                onLogout = {
                    scope.launch {
                        repository.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
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
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
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
                    scope.launch {
                        repository.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = "parent_dashboard/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            ParentDashboard(
                studentId = studentId,
                repository = repository,
                onLogout = {
                    scope.launch {
                        repository.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
