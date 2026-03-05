package com.simats.smartpcas

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val PREF_NAME = "SmartPacsSession"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_USER_EMAIL = "userEmail"
        const val KEY_USER_NAME = "userName"
        const val KEY_USER_PASSWORD = "userPassword"
        const val KEY_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"
        const val KEY_LANGUAGE = "language"
    }

    fun saveLoginState(isLoggedIn: Boolean) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserDetails(userId: Int, name: String, email: String) {
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_EMAIL, email)
        editor.apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    fun saveCredentials(email: String, password: String) {
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_PASSWORD, password)
        editor.apply()
    }

    fun getCredentials(): Pair<String?, String?> {
        val email = prefs.getString(KEY_USER_EMAIL, "")
        val password = prefs.getString(KEY_USER_PASSWORD, "")
        return Pair(email, password)
    }

    fun getUserDetails(): HashMap<String, String?> {
        val user = HashMap<String, String?>()
        user[KEY_USER_NAME] = prefs.getString(KEY_USER_NAME, null)
        user[KEY_USER_EMAIL] = prefs.getString(KEY_USER_EMAIL, null)
        return user
    }

    fun setHasSeenOnboarding(hasSeen: Boolean) {
        editor.putBoolean(KEY_HAS_SEEN_ONBOARDING, hasSeen)
        editor.apply()
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun saveLanguage(languageCode: String) {
        editor.putString(KEY_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun logout() {
        editor.clear()
        editor.apply()
    }
}
