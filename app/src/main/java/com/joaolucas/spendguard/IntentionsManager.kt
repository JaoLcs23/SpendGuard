package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IntentionsManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val PREFS_NAME = "spendguard_intentions_secure"
        private const val TAG = "IntentionsManager"
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (e: Exception) {
            Log.w(TAG, "Falha no EncryptedSharedPreferences, recriando.", e)
            try {
                context.deleteSharedPreferences(PREFS_NAME)
                buildEncryptedPrefs(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Falha critica no armazenamento seguro de Intentions", e2)
                context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _intention = MutableStateFlow(getIntention())
    val intention: StateFlow<String> = _intention

    fun getIntention(): String = prefs.getString("current_intention", "") ?: ""

    fun setIntention(text: String) {
        prefs.edit().putString("current_intention", text.take(300)).apply()
        _intention.value = text.take(300)
    }

    fun clear() {
        prefs.edit().remove("current_intention").apply()
        _intention.value = ""
    }
}
