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
import com.example.edu_ai.utils.PreferenceManager
import com.example.edu_ai.ui.screens.student.LearnScreen
import com.example.edu_ai.ui.screens.student.LibraryScreen
import com.example.edu_ai.ui.screens.student.LearningRepositoryScreen
import com.example.edu_ai.ui.screens.student.ModuleScreen
import com.example.edu_ai.ui.screens.student.StudentDashboard
import com.example.edu_ai.ui.screens.student.UnitOutlineScreen
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
        val lastUserId = PreferenceManager.getLastUserId(context)
        val token = PreferenceManager.getToken(context)
        
        if (token != null && lastUserId != null) {
            navController.navigate("student_dashboard/$lastUserId") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, userId ->
                    navController.navigate("student_dashboard/$userId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }


// ... (inside NavHost)

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
                onLaunchModule = { subtopicId ->
                    if (subtopicId != null) {
                        navController.navigate("learn_screen/$userId/$subtopicId")
                    } else {
                        navController.navigate("module_screen/$userId")
                    }
                },
                onOpenLibrary = {
                    navController.navigate("library_screen/$userId")
                },
                onViewUnitOutline = { unitId ->
                    navController.navigate("unit_outline/$unitId")
                },
                onOpenConsultations = {
                    navController.navigate("module_screen/$userId")
                }
            )
        }

        composable(
            route = "unit_outline/{unitId}",
            arguments = listOf(navArgument("unitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getLong("unitId") ?: 0L
            UnitOutlineScreen(
                unitId = unitId,
                onBack = { navController.popBackStack() },
                onViewSubtopicRepository = { subtopicId, name ->
                    navController.navigate("learning_repository/$subtopicId/$name")
                }
            )
        }

        composable(
            route = "learning_repository/{subtopicId}/{subtopicName}",
            arguments = listOf(
                navArgument("subtopicId") { type = NavType.LongType },
                navArgument("subtopicName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subtopicId = backStackEntry.arguments?.getLong("subtopicId") ?: 0L
            val subtopicName = backStackEntry.arguments?.getString("subtopicName") ?: ""
            LearningRepositoryScreen(
                subtopicId = subtopicId,
                subtopicName = subtopicName,
                onBack = { navController.popBackStack() },
                repository = repository
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
                },
                onViewUnitOutline = { unitId -> navController.navigate("unit_outline/$unitId") }
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

        composable(
            route = "learn_screen/{userId}/{subtopicId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("subtopicId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val subtopicId = backStackEntry.arguments?.getInt("subtopicId") ?: 0
            LearnScreen(
                subtopicId = subtopicId,
                userId = userId,
                onBack = { navController.popBackStack() },
                onTriggerQuiz = {
                    if (!navController.popBackStack()) {
                        navController.navigate("student_dashboard/$userId")
                    }
                }
            )
        }
    }
}
