
package com.example.edu_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.edu_ai.ui.screens.student.LibraryScreen
import com.example.edu_ai.ui.screens.student.ModuleScreen
import com.example.edu_ai.ui.screens.student.StudentDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherDashboard
import com.example.edu_ai.ui.screens.teacher.TeacherViewModel
import com.example.edu_ai.ui.screens.teacher.TeacherViewModelFactory
import com.example.edu_ai.ui.screens.parent.ParentDashboard
import com.example.edu_ai.ui.screens.admin.AdminDashboardScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val repository = app.repository
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val user = dao.getUser().firstOrNull()
        if (user != null) {
            val destination = when (user.role) {
                "Teacher" -> "teacher_dashboard"
                "Parent" -> "parent_dashboard/${user.id}"
                "Admin" -> "admin_dashboard"
                else -> "student_dashboard/${user.id}"
            }
            navController.navigate(destination) {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, userId ->
                    val destination = when (role) {
                        "Teacher" -> "teacher_dashboard"
                        "Parent" -> "parent_dashboard/$userId"
                        "Admin" -> "admin_dashboard"
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
                        repository.logout(context)
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onLaunchModule = {
                    navController.navigate("module_screen/$userId")
                },
                onOpenLibrary = {
                    navController.navigate("library_screen/$userId")
                },
                onLaunchUnit = { unitId ->
                    navController.navigate("unit_outline/$userId/$unitId")
                },
                onOpenBookmarks = {
                    navController.navigate("bookmarks_screen/$userId")
                }
            )
        }

        composable(
            route = "unit_outline/{userId}/{unitId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("unitId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val unitId = backStackEntry.arguments?.getLong("unitId") ?: 0L
            com.example.edu_ai.ui.screens.student.UnitOutlineScreen(
                userId = userId,
                unitId = unitId,
                onBack = { navController.popBackStack() },
                onNavigateToLearn = { subtopicId ->
                    navController.navigate("learn_screen/$userId/$subtopicId")
                }
            )
        }

        composable(
            route = "learn_screen/{userId}/{subtopicId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("subtopicId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val subtopicId = backStackEntry.arguments?.getLong("subtopicId") ?: 0L
            com.example.edu_ai.ui.screens.student.LearnScreen(
                userId = userId,
                subtopicId = subtopicId,
                onBack = { navController.popBackStack() },
                onNavigateToBrowser = { url ->
                    navController.navigate("browser_screen/$userId?url=$url")
                }
            )
        }

        composable(
            route = "bookmarks_screen/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.example.edu_ai.ui.screens.student.BookmarksScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onNavigateToLearn = { subtopicId ->
                    navController.navigate("learn_screen/$userId/$subtopicId")
                },
                onNavigateToBrowser = { url ->
                    navController.navigate("browser_screen/$userId?url=$url")
                }
            )
        }

        composable(
            route = "browser_screen/{userId}?url={url}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val url = backStackEntry.arguments?.getString("url") ?: "https://statpearls.com/articles/hemolytic_anemia"
            com.example.edu_ai.ui.screens.student.BrowserScreen(
                userId = userId,
                url = url,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "library_screen/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            LibraryScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onUnitAdded = { 
                    navController.popBackStack()
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
                        repository.logout(context)
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
                        repository.logout(context)
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(
                onLogout = {
                    scope.launch {
                        repository.logout(context)
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}


 