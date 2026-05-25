package com.joaolucas.spendguard

import android.content.Context
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

    suspend fun syncDownload() {
        withContext(Dispatchers.IO) {
            val userId = userRepository.getCurrentUserId() ?: return@withContext

            try {
                val remotePurchases = client.postgrest["purchases"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeList<PurchaseRemote>()

                val localPurchases = purchaseDao.getPurchasesByUserDirect(userId)
                val localIds = localPurchases.map { "${it.timestamp}_${it.itemName}" }.toSet()

                for (remote in remotePurchases) {
                    val key = "${remote.timestamp}_${remote.itemName}"
                    if (!localIds.contains(key)) {
                        val newEntity = PurchaseEntity(
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
                        purchaseDao.insert(newEntity)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val remoteData = client.postgrest["user_data"]
                    .select(Columns.ALL) { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserDataRemote>()

                if (remoteData != null) {
                    streakManager.setStreakCount(remoteData.streakCount)
                    streakManager.setStreakLastDay(remoteData.streakLastDay)

                    goalManager.setMonthlyGoalBits(remoteData.monthlyGoalBits)

                    intentionsManager.setIntention(remoteData.currentIntention)

                    val achJson = JSONObject(remoteData.achievementsJson)
                    achievementsManager.restoreFromJson(achJson)

                    profileManager.restoreFromJson(remoteData.profileJson)
                } else {
                    syncUpload()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncUpload() {
        withContext(Dispatchers.IO) {
            val userId = userRepository.getCurrentUserId() ?: return@withContext

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
                    client.postgrest["user_data"].insert(userData)
                } else {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
