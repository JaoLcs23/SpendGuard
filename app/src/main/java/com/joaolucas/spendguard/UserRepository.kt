package com.joaolucas.spendguard

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class UserRepository {

    private val client = SupabaseClient.client

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    suspend fun handleGoogleCallback(url: String) {
        try {
            val uri      = android.net.Uri.parse(url)
            val fragment = uri.fragment ?: return

            val params = fragment.split("&").associate {
                val key = it.substringBefore("=")
                val value = it.substringAfter("=")
                key to value
            }

            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]

            if (accessToken != null && refreshToken != null) {
                client.auth.importAuthToken(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )

                val sessionUser = client.auth.currentUserOrNull() ?: return
                val userId      = sessionUser.id
                val email       = sessionUser.email

                val existing = try {
                    client.postgrest["users"]
                        .select(Columns.ALL) { filter { eq("id", userId) } }
                        .decodeSingleOrNull<UserProfile>()
                } catch (_: Exception) { null }

                if (existing == null) {
                    try {
                        client.postgrest["users"].insert(
                            UserProfile(id = userId, email = email)
                        )
                    } catch (_: Exception) { }
                }

                loadUserProfile()
            }
        } catch (_: Exception) { }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            loadUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
            }
            val sessionUser = client.auth.currentUserOrNull()
            val userId      = sessionUser?.id ?: return Result.failure(Exception("Erro ao obter usuário"))
            client.postgrest["users"].insert(
                UserProfile(id = userId, email = email)
            )
            loadUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(onUrl: (String) -> Unit): Result<Unit> {
        return try {
            val url = client.auth.getOAuthUrl(
                provider    = Google,
                redirectUrl = "spendguard://login-callback"
            )
            onUrl(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
        } finally {
            _currentUser.value = null
            _isLoggedIn.value  = false
        }
    }


    suspend fun loadUserProfile() {
        try {
            val sessionUser = client.auth.currentUserOrNull()
            val userId      = sessionUser?.id ?: run {
                _isLoggedIn.value = false
                return
            }
            val profile = try {
                client.postgrest["users"]
                    .select(Columns.ALL) { filter { eq("id", userId) } }
                    .decodeSingleOrNull<UserProfile>()
            } catch (_: Exception) {
                UserProfile(id = userId, email = sessionUser.email)
            }
            _currentUser.value = profile
            _isLoggedIn.value  = true
        } catch (_: Exception) {
            _isLoggedIn.value = false
        }
    }

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun activatePro(planType: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Não autenticado"))
            try {
                client.postgrest["users"].update(
                    mapOf("is_pro" to true, "plan_type" to planType)
                ) { filter { eq("id", userId) } }
                loadUserProfile()
            } catch (e: Exception) {
                _currentUser.value = _currentUser.value?.copy(isPro = true, planType = planType)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivatePro(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Não autenticado"))
            try {
                client.postgrest["users"].update(
                    mapOf("is_pro" to false, "plan_type" to "none")
                ) { filter { eq("id", userId) } }
                loadUserProfile()
            } catch (e: Exception) {
                // Se o Supabase bloquear, força localmente
                _currentUser.value = _currentUser.value?.copy(isPro = false, planType = "none")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun forceProState(isPro: Boolean) {
        _currentUser.value = _currentUser.value?.copy(isPro = isPro)
    }

    suspend fun isPro(): Boolean = _currentUser.value?.isPro ?: false

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    suspend fun getWeeklyUsage(): WeeklyUsage? {
        return try {
            val userId  = getCurrentUserId() ?: return null
            val weekKey = currentWeekKey()
            client.postgrest["weekly_usage"]
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        eq("week_key", weekKey)
                    }
                }
                .decodeSingleOrNull<WeeklyUsage>()
        } catch (_: Exception) { null }
    }

    suspend fun canUseGuardian(): Boolean {
        if (isPro()) return true
        val usage = getWeeklyUsage()
        return (usage?.guardianUses ?: 0) < ProManager.FREE_WEEKLY_LIMIT
    }

    suspend fun canUseCalculator(): Boolean {
        if (isPro()) return true
        val usage = getWeeklyUsage()
        return (usage?.calculatorUses ?: 0) < ProManager.FREE_WEEKLY_LIMIT
    }

    suspend fun registerGuardianUse() {
        if (isPro()) return
        upsertWeeklyUsage(guardianDelta = 1)
    }

    suspend fun registerCalculatorUse() {
        if (isPro()) return
        upsertWeeklyUsage(calculatorDelta = 1)
    }

    private suspend fun upsertWeeklyUsage(guardianDelta: Int = 0, calculatorDelta: Int = 0) {
        try {
            val userId   = getCurrentUserId() ?: return
            val weekKey  = currentWeekKey()
            val existing = getWeeklyUsage()

            if (existing == null) {
                client.postgrest["weekly_usage"].insert(
                    WeeklyUsage(
                        userId         = userId,
                        weekKey        = weekKey,
                        guardianUses   = guardianDelta,
                        calculatorUses = calculatorDelta
                    )
                )
            } else {
                client.postgrest["weekly_usage"].update(
                    mapOf(
                        "guardian_uses"   to existing.guardianUses + guardianDelta,
                        "calculator_uses" to existing.calculatorUses + calculatorDelta
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("week_key", weekKey)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun syncPurchase(purchase: PurchaseEntity) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("UserRepository", "syncPurchase: userId is null, skipping")
            return
        }
        try {
            Log.d("UserRepository", "syncPurchase: uploading '${purchase.itemName}' for user $userId")
            client.postgrest["purchases"].insert(
                PurchaseRemote(
                    userId         = userId,
                    itemName       = purchase.itemName,
                    price          = purchase.price,
                    justification  = purchase.justification,
                    wasBlocked     = purchase.wasBlocked,
                    aiMessage      = purchase.aiMessage,
                    coolingOffTime = purchase.coolingOffTime,
                    timestamp      = purchase.timestamp,
                    category       = purchase.category
                )
            )
            Log.d("UserRepository", "syncPurchase: SUCCESS for '${purchase.itemName}'")
        } catch (e: Exception) {
            Log.e("UserRepository", "syncPurchase: FAILED for '${purchase.itemName}'", e)
        }
    }

    suspend fun clearAllPurchases() {
        val userId = getCurrentUserId() ?: return
        try {
            client.postgrest["purchases"].delete {
                filter { eq("user_id", userId) }
            }
            Log.d("UserRepository", "clearAllPurchases: SUCCESS")
        } catch (e: Exception) {
            Log.e("UserRepository", "clearAllPurchases: FAILED", e)
        }
    }

    suspend fun deletePurchase(timestamp: Long, itemName: String) {
        val userId = getCurrentUserId() ?: return
        try {
            client.postgrest["purchases"].delete {
                filter { 
                    eq("user_id", userId)
                    eq("timestamp", timestamp)
                    eq("item_name", itemName)
                }
            }
            Log.d("UserRepository", "deletePurchase: SUCCESS")
        } catch (e: Exception) {
            Log.e("UserRepository", "deletePurchase: FAILED", e)
        }
    }
}