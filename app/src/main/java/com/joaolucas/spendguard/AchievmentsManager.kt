package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class AchievementsManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "spendguard_achievements_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("spendguard_achievements", Context.MODE_PRIVATE)
    }

    private val _newlyUnlocked = MutableStateFlow<Achievement?>(null)
    val newlyUnlocked: StateFlow<Achievement?> = _newlyUnlocked

    val all: List<Achievement> = listOf(
        Achievement(
            id          = "first_block",
            icon        = Icons.Outlined.Shield,
            title       = "Primeiro Passo",
            description = "Você pausou antes de agir. Essa é a base de tudo.",
            hint        = "Consulte o Guardião antes de uma compra e siga a orientação de bloqueio"
        ),
        Achievement(
            id          = "first_approved",
            icon        = Icons.Outlined.AutoAwesome,
            title       = "Compra Consciente",
            description = "Sua primeira compra aprovada com consciência. Você pensou antes de agir.",
            hint        = "Tenha uma compra aprovada pelo Guardião"
        ),
        Achievement(
            id          = "streak_3",
            icon        = Icons.Outlined.Whatshot,
            title       = "Força de Vontade",
            description = "Três vezes seguidas você escolheu não ceder ao impulso.",
            hint        = "Resista a 3 compras por impulso consecutivas"
        ),
        Achievement(
            id          = "streak_7",
            icon        = Icons.Outlined.LocalFireDepartment,
            title       = "Chama Acesa",
            description = "Sete bloqueios seguidos. Seu autocontrole é impressionante.",
            hint        = "Resista a 7 compras por impulso consecutivas"
        ),
        Achievement(
            id          = "saved_50",
            icon        = Icons.Outlined.Savings,
            title       = "R\$50 Protegidos",
            description = "Os primeiros R\$50 que ficaram no seu bolso em vez de sair por impulso.",
            hint        = "Acumule R\$50 em compras que você optou por não fazer"
        ),
        Achievement(
            id          = "saved_100",
            icon        = Icons.Outlined.Savings,
            title       = "R\$100 Protegidos",
            description = "R\$100 que ficaram no seu bolso em vez de sair por impulso.",
            hint        = "Acumule R\$100 em compras que você optou por não fazer"
        ),
        Achievement(
            id          = "saved_500",
            icon        = Icons.Outlined.Diamond,
            title       = "R\$500 Protegidos",
            description = "Meio salário mínimo guardado ao recusar impulsos. Isso é disciplina.",
            hint        = "Acumule R\$500 em compras que você optou por não fazer"
        ),
        Achievement(
            id          = "saved_1000",
            icon        = Icons.Outlined.EmojiEvents,
            title       = "R\$1000 Protegidos",
            description = "Mil reais que você decidiu não desperdiçar. Uma conquista real.",
            hint        = "Acumule R\$1.000 em compras que você optou por não fazer"
        ),
        Achievement(
            id          = "saved_5000",
            icon        = Icons.Outlined.WorkspacePremium,
            title       = "R\$5000 Protegidos",
            description = "Cinco mil reais blindados contra impulsos. Nível elite de disciplina financeira.",
            hint        = "Acumule R\$5.000 em compras que você optou por não fazer"
        ),
        Achievement(
            id          = "analyst_10",
            icon        = Icons.Outlined.Psychology,
            title       = "Hábito Formado",
            description = "Consultar o Guardião virou parte da sua rotina de consumo.",
            hint        = "Pause e reflita antes de 10 decisões de compra"
        ),
        Achievement(
            id          = "analyst_30",
            icon        = Icons.Outlined.Bolt,
            title       = "Mente Afiada",
            description = "30 decisões tomadas com consciência. Seu cérebro já pensa diferente.",
            hint        = "Pause e reflita antes de 30 decisões de compra"
        ),
        Achievement(
            id          = "analyst_100",
            icon        = Icons.Outlined.Star,
            title       = "Analista Mestre",
            description = "100 análises completadas. Você transformou reflexão em modo de vida.",
            hint        = "Complete 100 análises com o Guardião"
        ),
        Achievement(
            id          = "categories_5",
            icon        = Icons.Outlined.Category,
            title       = "Guardião Versátil",
            description = "Você protegeu seu dinheiro em 5 categorias diferentes. Consciência ampla.",
            hint        = "Bloqueie compras em pelo menos 5 categorias distintas"
        ),
        Achievement(
            id          = "night_resistance",
            icon        = Icons.Outlined.NightlightRound,
            title       = "Resistência Noturna",
            description = "Você não cedeu à compra impulsiva da madrugada. A mais difícil delas.",
            hint        = "Bloqueie um impulso entre 22h e 6h — o horário mais traiçoeiro"
        ),
        Achievement(
            id          = "pix_reflection",
            icon        = Icons.Outlined.SwapHoriz,
            title       = "PIX Consciente",
            description = "Você parou para refletir mesmo após enviar um PIX. Autoconsciência real.",
            hint        = "Use o Guardião para refletir sobre um PIX enviado"
        ),
        Achievement(
            id          = "high_value_block",
            icon        = Icons.Outlined.TrendingUp,
            title       = "Decisão de R\$1000",
            description = "Você bloqueou uma compra acima de R\$1.000. Coragem financeira real.",
            hint        = "Bloqueie uma compra acima de R\$1.000"
        ),
        Achievement(
            id          = "self_control",
            icon        = Icons.Outlined.SelfImprovement,
            title       = "Mestre do Autocontrole",
            description = "Você bloqueou o mesmo tipo de impulso três vezes. Padrão reconhecido, padrão vencido.",
            hint        = "Bloqueie compras na mesma categoria 3 vezes"
        ),
        Achievement(
            id          = "week_vigilant",
            icon        = Icons.Outlined.CalendarMonth,
            title       = "Semana Vigilante",
            description = "Sete dias seguidos consultando o Guardião. O hábito está consolidado.",
            hint        = "Mantenha o hábito de reflexão por 7 dias consecutivos"
        ),
        Achievement(
            id          = "saved_10000",
            icon        = Icons.Outlined.AccountBalance,
            title       = "Titã da Economia",
            description = "Você já salvou mais de dez mil reais com o Guardião. Uma verdadeira fortaleza financeira.",
            hint        = "Acumule R\$10.000 em compras que você optou por não fazer",
            isPro       = true
        ),
        Achievement(
            id          = "streak_30",
            icon        = Icons.Outlined.AcUnit,
            title       = "Minimalista Extremo",
            description = "Você recusou 30 compras por impulso em sequência. Seu autocontrole é inabalável.",
            hint        = "Resista a 30 compras por impulso consecutivas",
            isPro       = true
        ),
        Achievement(
            id          = "categories_10",
            icon        = Icons.Outlined.Visibility,
            title       = "Visão 360º",
            description = "Você blindou seu dinheiro em 10 categorias diferentes. Nenhum gatilho de consumo te engana mais.",
            hint        = "Bloqueie compras em pelo menos 10 categorias distintas",
            isPro       = true
        )
    )

    fun checkAfterAnalysis(purchases: List<PurchaseEntity>, isPix: Boolean = false) {
        val blocked       = purchases.filter { it.wasBlocked }
        val approved      = purchases.filter { !it.wasBlocked }
        val savedAmount   = blocked.sumOf { it.price }
        val totalAnalyses = purchases.size
        val hour          = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight       = hour >= 22 || hour < 6

        val candidates = mutableListOf<Achievement>()

        if (blocked.isNotEmpty())
            candidates.add(all.first { it.id == "first_block" })
        if (approved.isNotEmpty())
            candidates.add(all.first { it.id == "first_approved" })

        val consecutiveBlocked = purchases.takeWhile { it.wasBlocked }.size
        if (consecutiveBlocked >= 3)
            candidates.add(all.first { it.id == "streak_3" })
        if (consecutiveBlocked >= 7)
            candidates.add(all.first { it.id == "streak_7" })
        if (consecutiveBlocked >= 30)
            candidates.add(all.first { it.id == "streak_30" })

        if (savedAmount >= 50)   candidates.add(all.first { it.id == "saved_50" })
        if (savedAmount >= 100)  candidates.add(all.first { it.id == "saved_100" })
        if (savedAmount >= 500)  candidates.add(all.first { it.id == "saved_500" })
        if (savedAmount >= 1000) candidates.add(all.first { it.id == "saved_1000" })
        if (savedAmount >= 5000) candidates.add(all.first { it.id == "saved_5000" })
        if (savedAmount >= 10000) candidates.add(all.first { it.id == "saved_10000" })

        if (totalAnalyses >= 10)  candidates.add(all.first { it.id == "analyst_10" })
        if (totalAnalyses >= 30)  candidates.add(all.first { it.id == "analyst_30" })
        if (totalAnalyses >= 100) candidates.add(all.first { it.id == "analyst_100" })

        val blockedCategories = blocked.map { it.category }.toSet()
        if (blockedCategories.size >= 5)
            candidates.add(all.first { it.id == "categories_5" })
        if (blockedCategories.size >= 10)
            candidates.add(all.first { it.id == "categories_10" })

        val categoryCount = blocked.groupingBy { it.category }.eachCount()
        if (categoryCount.any { it.value >= 3 })
            candidates.add(all.first { it.id == "self_control" })

        if (blocked.any { it.price >= 1000.0 })
            candidates.add(all.first { it.id == "high_value_block" })

        if (isNight && purchases.firstOrNull()?.wasBlocked == true)
            candidates.add(all.first { it.id == "night_resistance" })

        if (isPix)
            candidates.add(all.first { it.id == "pix_reflection" })

        if (checkWeekStreak(purchases))
            candidates.add(all.first { it.id == "week_vigilant" })

        var firstNew: Achievement? = null
        for (achievement in candidates) {
            if (!isUnlocked(achievement.id)) {
                unlock(achievement.id)
                if (firstNew == null) firstNew = achievement
            }
        }
        _newlyUnlocked.value = firstNew
    }

    fun consumeNewlyUnlocked() { _newlyUnlocked.value = null }
    fun isUnlocked(id: String): Boolean = prefs.getBoolean("ach_$id", false)
    fun unlockedCount(): Int = all.count { isUnlocked(it.id) }

    private fun unlock(id: String) = prefs.edit().putBoolean("ach_$id", true).apply()

    private fun checkWeekStreak(purchases: List<PurchaseEntity>): Boolean {
        if (purchases.size < 7) return false
        val cal   = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val activeDays = purchases.map {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_YEAR)
        }.toSet()
        return (0..6).all { daysAgo -> (today - daysAgo) in activeDays }
    }
}

data class Achievement(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val hint: String,
    val isPro: Boolean = false
)