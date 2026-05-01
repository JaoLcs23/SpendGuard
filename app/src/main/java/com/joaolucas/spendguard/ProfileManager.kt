package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class FinancialGoal(val label: String) {
    EMERGENCY_FUND("Montar reserva de emergência"),
    INVEST("Começar a investir"),
    PAY_DEBT("Quitar dívidas"),
    SAVE_GOAL("Juntar para um objetivo"),
    SPEND_CONSCIOUS("Consumir com mais consciência")
}

enum class ProfileSpendingCategory(val label: String, val icon: String) {
    FOOD("Alimentação", "restaurant"),
    LEISURE("Lazer e entretenimento", "sports_esports"),
    ONLINE_SHOPPING("Compras online", "shopping_bag"),
    CLOTHING("Roupas e acessórios", "checkroom"),
    TECH("Tecnologia e eletrônicos", "devices"),
    SUBSCRIPTIONS("Assinaturas e serviços", "subscriptions"),
    OTHER("Outros", "more_horiz")
}

@Serializable
data class FinancialProfile(
    val monthlyIncome: Double = 0.0,
    val financialGoal: String = "",
    val spendingCategories: List<String> = emptyList(),
    val isComplete: Boolean = false
) {
    fun incomeLabel(): String = when {
        monthlyIncome <= 0     -> "Não informado"
        monthlyIncome < 2000   -> "Até R\$2.000"
        monthlyIncome < 4000   -> "R\$2.000 – R\$4.000"
        monthlyIncome < 8000   -> "R\$4.000 – R\$8.000"
        monthlyIncome < 15000  -> "R\$8.000 – R\$15.000"
        else                   -> "Acima de R\$15.000"
    }

    fun goalLabel(): String =
        FinancialGoal.values().firstOrNull { it.name == financialGoal }?.label ?: "Não informado"

    fun categoriesLabel(): String =
        spendingCategories
            .mapNotNull { name -> ProfileSpendingCategory.values().firstOrNull { it.name == name }?.label }
            .joinToString(", ")
            .ifEmpty { "Não informado" }

    fun toPromptContext(): String {
        if (!isComplete) return ""
        val parts = mutableListOf<String>()
        if (monthlyIncome > 0)
            parts.add("Renda mensal aproximada: ${incomeLabel()}")
        if (financialGoal.isNotEmpty())
            parts.add("Objetivo financeiro principal: ${goalLabel()}")
        if (spendingCategories.isNotEmpty())
            parts.add("Categorias onde mais gasta: ${categoriesLabel()}")
        return parts.joinToString(". ")
    }
}

class ProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendguard_profile", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(profile: FinancialProfile) {
        prefs.edit()
            .putString("profile_json", json.encodeToString(profile))
            .apply()
    }

    fun load(): FinancialProfile {
        val raw = prefs.getString("profile_json", null) ?: return FinancialProfile()
        return try { json.decodeFromString<FinancialProfile>(raw) } catch (_: Exception) { FinancialProfile() }
    }

    fun isComplete(): Boolean = load().isComplete

    fun clear() = prefs.edit().remove("profile_json").apply()
}