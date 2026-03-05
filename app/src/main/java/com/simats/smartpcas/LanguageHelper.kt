package com.simats.smartpcas

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageHelper {

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
            return context
        }
    }

    fun applyLanguage(context: Context) {
        val sessionManager = SessionManager(context)
        val languageCode = sessionManager.getLanguage()
        setLocale(context, languageCode)
    }

    fun getSortedLanguages(): List<Locale> {
        val availableLocales = Locale.getAvailableLocales()
        val distinctLanguages = mutableMapOf<String, Locale>()

        for (locale in availableLocales) {
            val language = locale.language
            if (language.isNotEmpty() && !distinctLanguages.containsKey(language)) {
                if (locale.getDisplayLanguage(Locale.ENGLISH).isNotEmpty()) {
                    distinctLanguages[language] = locale
                }
            }
        }
        
        return distinctLanguages.values.sortedBy { it.getDisplayLanguage(Locale.ENGLISH) }
    }
}
