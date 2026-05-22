package com.joaolucas.spendguard

object ContentRecommender {

    private const val MIN_PURCHASES = 5
    private const val MAX_RECOMMENDATIONS = 5

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "VESTUARIO"   to listOf("moda", "roupa", "impulso", "consumismo", "status", "tirania"),
        "LAZER"       to listOf("bem-estar", "satisfação", "contentamento", "entretenimento"),
        "ALIMENTACAO" to listOf("mercado", "mensal", "semanal", "alimentação", "supermercado", "classe média"),
        "TECNOLOGIA"  to listOf("tecnologia", "gadget", "eletrônico", "aparelho"),
        "ASSINATURAS" to listOf("passivo", "mensalidade", "dividendo", "renda passiva"),
        "SAUDE"       to listOf("saúde", "estilo de vida"),
        "TRANSPORTE"  to listOf("carro", "veículo", "combustível"),
        "CASA"        to listOf("imóvel", "aluguel", "aluguéis", "fundos imobiliários", "moradia"),
        "EDUCACAO"    to listOf("carreira", "autogestão", "gestão", "aprendizado", "habilidade"),
        "OUTROS"      to listOf("finanças", "dinheiro", "economia", "investimento", "orçamento")
    )

    private val impulseKeywords = listOf(
        "psicologia", "emocional", "emoções", "emoção",
        "impulso", "impulsivas", "comportamento", "comportamentos",
        "viés", "vieses", "conveniência", "sabotam",
        "irracional", "erros sistemáticos", "ego", "autogestão"
    )

    fun recommend(
        purchases: List<PurchaseEntity>,
        allResources: List<EducationalResource>
    ): List<EducationalResource> {
        if (purchases.size < MIN_PURCHASES) return emptyList()

        val ctx = buildContext(purchases)

        return allResources
            .map  { it to score(it, ctx) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_RECOMMENDATIONS)
            .map  { it.first }
    }

    private data class RecommendationContext(
        val topCategories:    List<String>,
        val impulseRate:      Float,
        val recentBlockCount: Int,
        val dominantCategory: String
    )

    private fun buildContext(purchases: List<PurchaseEntity>): RecommendationContext {
        val blocked    = purchases.count { it.wasBlocked }
        val impulseRate = if (purchases.isNotEmpty()) blocked.toFloat() / purchases.size else 0f

        val thirtyDaysAgo    = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recentBlockCount = purchases.count { it.wasBlocked && it.timestamp > thirtyDaysAgo }

        val topCategories = purchases
            .groupBy  { it.category.uppercase() }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        return RecommendationContext(
            topCategories    = topCategories,
            impulseRate      = impulseRate,
            recentBlockCount = recentBlockCount,
            dominantCategory = topCategories.firstOrNull() ?: "OUTROS"
        )
    }

    private fun score(resource: EducationalResource, ctx: RecommendationContext): Int {
        val text = "${resource.title} ${resource.description}".lowercase()
        var score = 0

        val dominantKws = categoryKeywords[ctx.dominantCategory] ?: emptyList()
        if (dominantKws.any { text.contains(it, ignoreCase = true) }) score += 3

        for (cat in ctx.topCategories.drop(1)) {
            val kws = categoryKeywords[cat] ?: continue
            if (kws.any { text.contains(it, ignoreCase = true) }) {
                score += 2
                break
            }
        }

        if (ctx.impulseRate > 0.4f && impulseKeywords.any { text.contains(it, ignoreCase = true) }) {
            score += 2
        }

        if (ctx.recentBlockCount >= 3 && impulseKeywords.any { text.contains(it, ignoreCase = true) }) {
            score += 1
        }

        return score
    }
}
