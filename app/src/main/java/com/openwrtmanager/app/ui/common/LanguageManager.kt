package com.openwrtmanager.app.ui.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private const val PREFS = "app_preferences"
    private const val LANGUAGE = "language"

    fun isArabic(): Boolean = Locale.getDefault().language == "ar"

    fun applySavedLanguage(context: Context) {
        val language = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(LANGUAGE, "en") ?: "en"
        apply(context, language)
    }

    fun toggle(context: Context) {
        val language = if (isArabic()) "en" else "ar"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE, language)
            .apply()
        apply(context, language)
    }

    private fun apply(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
