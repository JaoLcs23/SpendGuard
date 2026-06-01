package com.joaolucas.spendguard

import kotlinx.serialization.Serializable

@Serializable
data class FinancialIntention(
    val id: String,
    val text: String,
    val targetAmount: Double? = null,
    val status: String, // "CURRENT", "FUTURE", "COMPLETED"
    val currentSince: Long? = null,
    val completedAt: Long? = null,
    val carryOver: Double = 0.0
)
