package com.example.data.local

import android.content.Context

class KeyManager(context: Context) {
    private val prefs = context.getSharedPreferences("openrouter_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String? {
        val key = prefs.getString("api_key", null)
        return if (key.isNullOrBlank()) null else key
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("api_key", key.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove("api_key").apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString("selected_model_id", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString("selected_model_id", modelId).apply()
    }

    fun isInstagramPromptEnabled(): Boolean {
        return prefs.getBoolean("is_instagram_prompt_enabled", true)
    }

    fun setInstagramPromptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_instagram_prompt_enabled", enabled).apply()
    }
}
