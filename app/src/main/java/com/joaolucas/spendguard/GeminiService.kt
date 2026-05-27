package com.joaolucas.spendguard

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject

class GeminiService(private val baseUrl: String) {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000L
        }
    }

    suspend fun calculateOpportunityCost(amount: Double): OpportunityAnalysis {
        val requestBody = buildJsonObject {
            put("amount", amount)
        }
        
        val response: OpportunityAnalysis = client.post("$baseUrl/api/gemini/opportunity") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()
        
        return response
    }

    suspend fun analyzeImpulse(
        item: String,
        price: Double,
        justification: String,
        userProfile: FinancialProfile = FinancialProfile(),
        emotionalState: EmotionalState? = null
    ): InterventionResult {
        val requestBody = buildJsonObject {
            put("item", item)
            put("price", price)
            put("justification", justification)
            put("userProfileContext", userProfile.toPromptContext())
            put("emotionalStateLabel", emotionalState?.label ?: "")
            put("categoryOptions", SpendingCategory.geminiOptions)
            
            val hourlyRate = if (userProfile.monthlyIncome > 0) userProfile.monthlyIncome / 160.0 else 0.0
            val workHours  = if (hourlyRate > 0) "%.1f".format(price / hourlyRate) else ""
            put("workHours", workHours)
        }

        val result: InterventionResult = client.post("$baseUrl/api/gemini/impulse") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        return result.copy(category = SpendingCategory.fromString(result.category).name)
    }

    suspend fun generateWeeklyInsight(
        purchases: List<PurchaseEntity>,
        userProfile: FinancialProfile = FinancialProfile()
    ): WeeklyInsight {
        val blocked  = purchases.filter { it.wasBlocked }
        val approved = purchases.filter { !it.wasBlocked }
        val saved    = blocked.sumOf { it.price }
        val spent    = approved.sumOf { it.price }
        val topCategory = blocked.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "N/A"
        
        val requestBody = buildJsonObject {
            put("blockedCount", blocked.size)
            put("saved", saved)
            put("approvedCount", approved.size)
            put("spent", spent)
            put("topCategory", topCategory)
            put("profileContext", userProfile.toPromptContext())
        }

        val result: WeeklyInsight = client.post("$baseUrl/api/gemini/weekly") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        return result.copy(generatedAt = System.currentTimeMillis())
    }

    suspend fun extractPurchaseInfo(notificationText: String): PurchaseInfo? {
        return try {
            val requestBody = buildJsonObject {
                put("text", notificationText)
            }
            client.post("$baseUrl/api/gemini/extract-purchase") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractPixInfo(notificationText: String): PixInfo? {
        return try {
            val requestBody = buildJsonObject {
                put("text", notificationText)
            }
            client.post("$baseUrl/api/gemini/extract-pix") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun predictInsight(
        patterns: PredictivePatterns,
        userProfile: FinancialProfile = FinancialProfile()
    ): PredictiveInsight {
        val requestBody = buildJsonObject {
            put("topSpendingDay", patterns.topSpendingDay)
            put("topSpendingHour", patterns.topSpendingHour)
            put("weeklySpendingCurrent", patterns.weeklySpendingCurrent)
            put("weeklySpendingPrevious", patterns.weeklySpendingPrevious)
            put("spendingTrend", patterns.spendingTrend)
            put("impulseRate", patterns.impulseRate)
            put("impulseRateTrend", patterns.impulseRateTrend)
            put("riskCategory", patterns.riskCategory)
            put("riskCategoryBlockRate", patterns.riskCategoryBlockRate)
            put("totalAnalyzed", patterns.totalAnalyzed)
            put("totalBlocked", patterns.totalBlocked)
            put("monthTotal", patterns.monthTotal)
            put("profileContext", userProfile.toPromptContext())
        }

        return client.post("$baseUrl/api/gemini/predict") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}