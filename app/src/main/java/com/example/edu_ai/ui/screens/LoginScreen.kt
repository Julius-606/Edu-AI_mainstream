
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var backendMode by remember { mutableStateOf(PreferenceManager.getBackendMode(context)) }
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
                            val response = RetrofitClient.instance.login(LoginRequest(email, password))
                            PreferenceManager.saveToken(context, response.accessToken)
                            onLoginSuccess(response.role, response.userId)
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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                onLoginSuccess("Admin", "admin_root")
            },
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text("⚡ Fast-Track: Enter as Admin Console", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { 
            val baseUrl = when (backendMode) {
                "ngrok" -> "https://untropic-rozanne-noncomprehendingly.ngrok-free.dev/"
                "container" -> "http://10.0.2.2:8001/"
                else -> BuildConfig.BACKEND_BASE_URL
            }
            val signupUrl = baseUrl + "signup"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signupUrl))
            context.startActivity(intent)
        }) {
            Text("Don't have an account? Sign Up in Browser")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Backend Gateway Target",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val modes = listOf("cloud" to "Cloud", "ngrok" to "Ngrok", "container" to "Container")
            modes.forEach { (modeKey, modeName) ->
                val isSelected = backendMode == modeKey
                OutlinedButton(
                    onClick = {
                        backendMode = modeKey
                        PreferenceManager.saveBackendMode(context, modeKey)
                        PreferenceManager.saveDeveloperMode(context, modeKey != "cloud")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(40.dp)
                ) {
                    Text(modeName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


 