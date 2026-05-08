package com.joaolucas.spendguard

import kotlinx.serialization.Serializable

@Serializable
data class OpportunityAnalysis(
    val fiis: String,
    val savings: String,
    val stocks: String,
    val motivationalMessage: String
)

@Serializable
data class InterventionResult(
    val allowed: Boolean,
    val message: String,
    val coolingOffTime: Int = 0,
    val category: String = "OUTROS"
)

enum class SpendingCategory(val label: String, val color: Long) {
    ALIMENTACAO ("Alimentação",  0xFF1D9E75L),
    SAUDE       ("Saúde",        0xFF378ADDL),
    VESTUARIO   ("Vestuário",    0xFFD85A30L),
    TECNOLOGIA  ("Tecnologia",   0xFF7F77DDL),
    LAZER       ("Lazer",        0xFFD4537EL),
    TRANSPORTE  ("Transporte",   0xFFBA7517L),
    CASA        ("Casa",         0xFF639922L),
    ASSINATURAS ("Assinaturas",  0xFF5F5E5AL),
    EDUCACAO    ("Educação",     0xFFE24B4AL),
    OUTROS      ("Outros",       0xFF888780L);

    companion object {
        fun fromString(value: String?): SpendingCategory {
            if (value.isNullOrBlank()) return OUTROS

            val upper = value.trim().uppercase()
                .replace("Ã", "A").replace("Ç", "C").replace("Á", "A")
                .replace("É", "E").replace("Ê", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ô", "O").replace("Ú", "U")

            return values().firstOrNull { it.name == upper }
                ?: values().firstOrNull {
                    it.label.uppercase()
                        .replace("Ã", "A").replace("Ç", "C").replace("Á", "A")
                        .replace("É", "E").replace("Ê", "E").replace("Í", "I")
                        .replace("Ó", "O").replace("Ô", "O").replace("Ú", "U") == upper
                }
                ?: OUTROS
        }

        val geminiOptions: String get() = values().joinToString(", ") { it.name }
    }
}

enum class ViewState {
    DASHBOARD,
    HISTORY,
    CALCULATOR,
    SIMULATOR,
    SETTINGS,
    ACHIEVEMENTS,
    PROFILE,
    CHALLENGE,
    IMPORT,
    PAYWALL,
    LIBRARY,
    INTENTIONS
}

@Serializable
data class PurchaseInfo(
    val itemName: String,
    val price: Double
)

@Serializable
data class PixInfo(
    val recipient: String,
    val amount: Double,
    val description: String = ""
)

enum class EmotionalState(val label: String, val emoji: String) {
    WELL("Bem", "😊"),
    TIRED("Cansado", "😴"),
    STRESSED("Estressado", "😤"),
    EXCITED("Animado", "🤩")
}

@Serializable
data class WeeklyInsight(
    val fiis: String = "",
    val savings: String = "",
    val stocks: String = "",
    val motivationalMessage: String = "",
    val summary: String = "",
    val generatedAt: Long = 0L
)