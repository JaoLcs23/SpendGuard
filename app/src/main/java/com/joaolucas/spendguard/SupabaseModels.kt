@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.joaolucas.spendguard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("plan_type") val planType: String = "free",
    @SerialName("plan_expiry") val planExpiry: String? = null
)

@Serializable
data class WeeklyUsage(
    @SerialName("user_id") val userId: String,
    @SerialName("week_key") val weekKey: String,
    @SerialName("guardian_uses") val guardianUses: Int = 0,
    @SerialName("calculator_uses") val calculatorUses: Int = 0
)

@Serializable
data class PurchaseRemote(
    @SerialName("user_id") val userId: String,
    @SerialName("item_name") val itemName: String,
    val price: Double,
    val justification: String? = null,
    @SerialName("was_blocked") val wasBlocked: Boolean = false,
    @SerialName("ai_message") val aiMessage: String? = null,
    @SerialName("cooling_off_time") val coolingOffTime: Int = 0,
    val timestamp: Long,
    val category: String = ""
)

@Serializable
data class UserDataRemote(
    @SerialName("user_id") val userId: String,
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("streak_last_day") val streakLastDay: String = "",
    @SerialName("monthly_goal_bits") val monthlyGoalBits: Long = 0,
    @SerialName("current_intention") val currentIntention: String = "",
    @SerialName("achievements_json") val achievementsJson: String = "{}",
    @SerialName("profile_json") val profileJson: String = "{}",
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class EducationalResourceRemote(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val title: String,
    val author: String,
    val type: String,
    val description: String,
    val link: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_saved") val isSaved: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)