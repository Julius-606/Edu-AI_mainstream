package com.example.edu_ai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.BuildConfig
import com.example.edu_ai.data.remote.RetrofitClient
import com.example.edu_ai.schemas.LoginRequest
import com.example.edu_ai.utils.PreferenceManager
import com.example.edu_ai.EduAIApplication
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val repository = app.repository
    
    var isDeveloperMode by remember { mutableStateOf(PreferenceManager.isDeveloperMode(context)) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Trace Learning Portal",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sign in to access your workspace",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Login Button
        Button(
            onClick = { 
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            // Attempt Online Login
                            val response = RetrofitClient.instance.login(LoginRequest(email, password))
                            PreferenceManager.saveToken(context, response.accessToken)
                            PreferenceManager.saveLastUserId(context, response.userId)
                            
                            // Sync user details to local DB for offline access
                            repository.syncUserToLocal(response.userId, email, password)
                            
                            onLoginSuccess(response.role, response.userId)
                        } catch (e: IOException) {
                            // Offline or network error - attempt Offline Login
                            val offlineUser = repository.offlineLogin(email, password)
                            if (offlineUser != null) {
                                PreferenceManager.saveLastUserId(context, offlineUser.id)
                                // We don't have a fresh token, but we might have an old one 
                                // or the app can function in a limited way.
                                onLoginSuccess(offlineUser.role, offlineUser.id)
                            } else {
                                errorMessage = "Offline login failed. No cached credentials found."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Login failed: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { 
            val baseUrl = if (isDeveloperMode) "https://untropic-rozanne-noncomprehendingly.ngrok-free.dev/" else BuildConfig.BACKEND_BASE_URL
            val signupUrl = baseUrl + "signup"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signupUrl))
            context.startActivity(intent)
        }) {
            Text("Don't have an account? Sign Up in Browser")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Developer Mode (Local Backend)", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDeveloperMode,
                onCheckedChange = { 
                    isDeveloperMode = it
                    PreferenceManager.saveDeveloperMode(context, it)
                }
            )
        }
    }
}
