package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class IntentionsManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)
    private val json = Json { ignoreUnknownKeys = true }

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

    private val _intentions = MutableStateFlow(loadIntentions())
    val intentions: StateFlow<List<FinancialIntention>> = _intentions

    private fun loadIntentions(): List<FinancialIntention> {
        val raw = prefs.getString("current_intention", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            if (raw.startsWith("[")) {
                json.decodeFromString<List<FinancialIntention>>(raw)
            } else {
                // Migração de texto simples para lista
                listOf(FinancialIntention(id = UUID.randomUUID().toString(), text = raw, status = "CURRENT"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao carregar intentions", e)
            emptyList()
        }
    }

    private fun saveIntentions(list: List<FinancialIntention>) {
        val raw = json.encodeToString(list)
        prefs.edit().putString("current_intention", raw).apply()
        _intentions.value = list
    }

    fun addIntention(text: String, targetAmount: Double?) {
        val list = _intentions.value.toMutableList()
        val hasCurrent = list.any { it.status == "CURRENT" }
        val status = if (hasCurrent) "FUTURE" else "CURRENT"
        var carryOver = 0.0
        var currentSince: Long? = null
        if (status == "CURRENT") {
            // Pick up any pending surplus from previously completed intentions
            val pending = prefs.getFloat("pending_surplus", 0f).toDouble()
            if (pending > 0.0) {
                carryOver = pending
                prefs.edit().remove("pending_surplus").apply()
            }
            val lastCompleted = list.maxOfOrNull { it.completedAt ?: 0L } ?: 0L
            currentSince = if (lastCompleted > 0L) lastCompleted else null
        }
        list.add(FinancialIntention(
            id = UUID.randomUUID().toString(), 
            text = text.take(300), 
            targetAmount = targetAmount, 
            status = status,
            currentSince = currentSince,
            carryOver = carryOver
        ))
        saveIntentions(list)
    }

    fun updateStatus(id: String, newStatus: String, purchases: List<PurchaseEntity> = emptyList()) {
        var list = _intentions.value.toMutableList()
        
        if (newStatus == "COMPLETED") {
            val idx = list.indexOfFirst { it.id == id }
            if (idx != -1) {
                val item = list[idx]
                val since = item.currentSince ?: 0L
                val saved = item.carryOver + purchases.filter { it.wasBlocked && it.timestamp >= since }.sumOf { it.price }
                val target = item.targetAmount ?: 0.0
                val surplus = if (target > 0) (saved - target).coerceAtLeast(0.0) else saved
                
                list[idx] = item.copy(status = "COMPLETED", completedAt = System.currentTimeMillis())
                
                // Auto-promote next future with surplus
                if (list.none { it.status == "CURRENT" }) {
                    val nextIdx = list.indexOfFirst { it.status == "FUTURE" }
                    if (nextIdx != -1) {
                        list[nextIdx] = list[nextIdx].copy(
                            status = "CURRENT", 
                            currentSince = System.currentTimeMillis(),
                            carryOver = surplus
                        )
                    } else {
                        // No future intention to receive surplus — store it for later
                        if (surplus > 0.0) {
                            prefs.edit().putFloat("pending_surplus", surplus.toFloat()).apply()
                        }
                    }
                }
            }
        } else if (newStatus == "CURRENT") {
            // Calculate saved amount of the current intention being demoted
            val currentIdx = list.indexOfFirst { it.status == "CURRENT" }
            var savedFromCurrent = 0.0
            if (currentIdx != -1) {
                val current = list[currentIdx]
                val since = current.currentSince ?: 0L
                savedFromCurrent = current.carryOver + purchases.filter { it.wasBlocked && it.timestamp >= since }.sumOf { it.price }
                list[currentIdx] = current.copy(status = "FUTURE", carryOver = 0.0)
            }
            // Promote new with the transferred balance
            val newIdx = list.indexOfFirst { it.id == id }
            if (newIdx != -1) {
                list[newIdx] = list[newIdx].copy(
                    status = "CURRENT", 
                    currentSince = System.currentTimeMillis(),
                    carryOver = savedFromCurrent
                )
            }
        }
        
        saveIntentions(list)
    }

    fun editIntention(id: String, newText: String, newTarget: Double?) {
        val list = _intentions.value.map {
            if (it.id == id) {
                it.copy(text = newText.take(300), targetAmount = newTarget)
            } else {
                it
            }
        }
        saveIntentions(list)
    }

    fun deleteIntention(id: String) {
        val list = _intentions.value.filter { it.id != id }
        saveIntentions(list)
    }

    fun getIntentionJson(): String {
        return json.encodeToString(_intentions.value)
    }

    fun setIntentionJson(raw: String) {
        prefs.edit().putString("current_intention", raw).apply()
        _intentions.value = loadIntentions()
    }

    fun clear() {
        prefs.edit().remove("current_intention").apply()
        _intentions.value = emptyList()
    }
}
