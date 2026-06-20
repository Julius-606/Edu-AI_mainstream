package com.example.edu_ai.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferenceManager {
    private const val PREF_NAME = "edu_ai_secure_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(context: Context, token: String) {
        getSharedPrefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun clearToken(context: Context) {
        getSharedPrefs(context).edit().remove(KEY_AUTH_TOKEN).apply()
    }
}
