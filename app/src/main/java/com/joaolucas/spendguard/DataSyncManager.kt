package com.joaolucas.spendguard

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DataSyncManager(
    private val context: Context,
    private val userRepository: UserRepository,
    private val purchaseDao: PurchaseDao,
    private val achievementsManager: AchievementsManager,
    private val streakManager: StreakManager,
    private val goalManager: GoalManager,
    private val intentionsManager: IntentionsManager,
    private val profileManager: ProfileManager
) {
    private val client = SupabaseClient.client

    companion object {
        private const val TAG = "DataSyncManager"
    }

    /**
     * Download all user data from Supabase into local storage.
     * Called once when the user opens the app after login.
     */
    suspend fun syncDownload() {
        withContext(Dispatchers.IO) {
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "syncDownload: userId is null, skipping")
                return@withContext
            }
            Log.d(TAG, "syncDownload: starting for user $userId")

            // --- Download purchases ---
            try {
                val remotePurchases = client.postgrest["purchases"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeList<PurchaseRemote>()

                Log.d(TAG, "syncDownload: got ${remotePurchases.size} remote purchases")

                val localPurchases = purchaseDao.getPurchasesByUserDirect(userId)
                val localKeys = localPurchases.map { "${it.timestamp}_${it.itemName}" }.toSet()

                var insertedCount = 0
                for (remote in remotePurchases) {
                    val key = "${remote.timestamp}_${remote.itemName}"
                    if (!localKeys.contains(key)) {
                        purchaseDao.insert(
                            PurchaseEntity(
                                userId = remote.userId,
                                itemName = remote.itemName,
                                price = remote.price,
                                justification = remote.justification ?: "",
                                wasBlocked = remote.wasBlocked,
                                aiMessage = remote.aiMessage ?: "",
                                coolingOffTime = remote.coolingOffTime,
                                timestamp = remote.timestamp,
                                category = remote.category
                            )
                        )
                        insertedCount++
                    }
                }
                Log.d(TAG, "syncDownload: inserted $insertedCount new purchases locally")
            } catch (e: Exception) {
                Log.e(TAG, "syncDownload: FAILED to download purchases", e)
            }

            // --- Download user_data (streak, goals, achievements, etc.) ---
            try {
                val remoteData = client.postgrest["user_data"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserDataRemote>()

                if (remoteData != null) {
                    Log.d(TAG, "syncDownload: found user_data, restoring...")
                    streakManager.setStreakCount(remoteData.streakCount)
                    streakManager.setStreakLastDay(remoteData.streakLastDay)
                    goalManager.setMonthlyGoalBits(remoteData.monthlyGoalBits)
                    intentionsManager.setIntention(remoteData.currentIntention)

                    try {
                        val achJson = JSONObject(remoteData.achievementsJson)
                        achievementsManager.restoreFromJson(achJson)
                    } catch (e: Exception) {
                        Log.w(TAG, "syncDownload: failed to parse achievements JSON", e)
                    }

                    profileManager.restoreFromJson(remoteData.profileJson)
                    Log.d(TAG, "syncDownload: user_data restored successfully")
                } else {
                    Log.d(TAG, "syncDownload: no user_data found, uploading current state")
                    syncUpload()
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncDownload: FAILED to download user_data", e)
            }
        }
    }

    /**
     * Upload current local state to Supabase.
     * Called when local data changes (achievements, streak, etc.)
     */
    suspend fun syncUpload() {
        withContext(Dispatchers.IO) {
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "syncUpload: userId is null, skipping")
                return@withContext
            }
            Log.d(TAG, "syncUpload: starting for user $userId")

            try {
                val userData = UserDataRemote(
                    userId = userId,
                    streakCount = streakManager.getStreakCount(),
                    streakLastDay = streakManager.getStreakLastDay(),
                    monthlyGoalBits = goalManager.getMonthlyGoalBits(),
                    currentIntention = intentionsManager.getIntention(),
                    achievementsJson = achievementsManager.toJson().toString(),
                    profileJson = profileManager.toJson()
                )

                val existing = client.postgrest["user_data"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserDataRemote>()

                if (existing == null) {
                    Log.d(TAG, "syncUpload: no existing record, inserting")
                    client.postgrest["user_data"].insert(userData)
                } else {
                    Log.d(TAG, "syncUpload: updating existing record")
                    client.postgrest["user_data"].update(
                        mapOf(
                            "streak_count" to userData.streakCount,
                            "streak_last_day" to userData.streakLastDay,
                            "monthly_goal_bits" to userData.monthlyGoalBits,
                            "current_intention" to userData.currentIntention,
                            "achievements_json" to userData.achievementsJson,
                            "profile_json" to userData.profileJson
                        )
                    ) { filter { eq("user_id", userId) } }
                }
                Log.d(TAG, "syncUpload: success")
            } catch (e: Exception) {
                Log.e(TAG, "syncUpload: FAILED", e)
            }

            // --- Also upload any local purchases not yet in the cloud ---
            try {
                val localPurchases = purchaseDao.getPurchasesByUserDirect(userId)
                val remotePurchases = client.postgrest["purchases"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeList<PurchaseRemote>()

                val remoteKeys = remotePurchases.map { "${it.timestamp}_${it.itemName}" }.toSet()

                var uploadedCount = 0
                for (local in localPurchases) {
                    val key = "${local.timestamp}_${local.itemName}"
                    if (!remoteKeys.contains(key)) {
                        client.postgrest["purchases"].insert(
                            PurchaseRemote(
                                userId = userId,
                                itemName = local.itemName,
                                price = local.price,
                                justification = local.justification,
                                wasBlocked = local.wasBlocked,
                                aiMessage = local.aiMessage,
                                coolingOffTime = local.coolingOffTime,
                                timestamp = local.timestamp,
                                category = local.category
                            )
                        )
                        uploadedCount++
                    }
                }
                Log.d(TAG, "syncUpload: uploaded $uploadedCount missing purchases")
            } catch (e: Exception) {
                Log.e(TAG, "syncUpload: FAILED to upload purchases", e)
            }
        }
    }
}
