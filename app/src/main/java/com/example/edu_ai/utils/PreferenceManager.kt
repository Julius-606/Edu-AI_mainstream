package com.example.edu_ai.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferenceManager {
    private const val PREF_NAME = "edu_ai_secure_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_DEVELOPER_MODE = "developer_mode"
    private const val KEY_LAST_USER_ID = "last_user_id"

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

    fun saveDeveloperMode(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
    }

    fun isDeveloperMode(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_DEVELOPER_MODE, false)
    }

    fun saveLastUserId(context: Context, userId: String) {
        getSharedPrefs(context).edit().putString(KEY_LAST_USER_ID, userId).apply()
    }

    fun getLastUserId(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_LAST_USER_ID, null)
    }

    fun clearLastUserId(context: Context) {
        getSharedPrefs(context).edit().remove(KEY_LAST_USER_ID).apply()
    }
}
