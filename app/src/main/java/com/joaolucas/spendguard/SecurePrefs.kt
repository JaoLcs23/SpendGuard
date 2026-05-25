package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    fun create(context: Context, name: String): SharedPreferences {
        return try {
            build(context, name)
        } catch (e: Exception) {
            Log.w("SecurePrefs", "Failed to create EncryptedSharedPreferences for $name, recreating.", e)
            try {
                context.deleteSharedPreferences(name)
                build(context, name)
            } catch (e2: Exception) {
                Log.e("SecurePrefs", "Critical failure in secure storage for $name", e2)
                context.getSharedPreferences("${name}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun build(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
