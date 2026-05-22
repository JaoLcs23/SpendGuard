package com.joaolucas.spendguard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun DashboardScreen(
    database: SpendGuardDatabase,
    userRepository: UserRepository,
    achievementsManager: AchievementsManager,
    challengeManager: ChallengeManager,
    streakManager: StreakManager,
    goalManager: GoalManager,
    weeklyInsightManager: WeeklyInsightManager,
    proManager: ProManager,
    onNavigate: (ViewState) -> Unit
) {
    val context       = LocalContext.current
    val gold          = MaterialTheme.colorScheme.primary
    val currentUserId = userRepository.getCurrentUserId() ?: ""
    val purchases     by database.purchaseDao().getPurchasesByUser(currentUserId).collectAsState(initial = emptyList())
    val challengeState by challengeManager.state.collectAsState()
    val streak        by streakManager.streak.collectAsState()
    val monthlyGoal   by goalManager.monthlyGoal.collectAsState()
    val weeklyInsight by weeklyInsightManager.insight.collectAsState()
    val currentHour   = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightRisk   = currentHour in 22..23 || currentHour in 0..2

    val monthlySpent = remember(purchases) {
        val cal   = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val year  = cal.get(Calendar.YEAR)
        purchases.filter { !it.wasBlocked && !it.isImported }.filter {
            val c = Calendar.getInstance().also { c -> c.timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }.sumOf { it.price }
    }

    val savedAmount    = remember(purchases) { purchases.filter { it.wasBlocked }.sumOf { it.price } }

    val last6MonthsData = remember(purchases) {
        val cal = Calendar.getInstance()
        (5 downTo 0).map { monthsAgo ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -monthsAgo)
            val m = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val label = arrayOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")[m]
            val total = purchases.filter { !it.wasBlocked && !it.isImported }.filter { p ->
                val c2 = Calendar.getInstance().also { it.timeInMillis = p.timestamp }
                c2.get(Calendar.MONTH) == m && c2.get(Calendar.YEAR) == y
            }.sumOf { it.price }
            Pair(label, total)
        }
    }
    val unlockedCount  = achievementsManager.unlockedCount()
    val totalAchievements = achievementsManager.all.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter            = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier           = Modifier.size(28.dp),
                        tint               = Color.Unspecified
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SpendGuard",
                        style       = MaterialTheme.typography.titleLarge,
                        fontWeight  = FontWeight.ExtraBold,
                        color       = gold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    greetingByHour(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = gold.copy(alpha = 0.1f),
                modifier = Modifier.clickable { onNavigate(ViewState.SETTINGS) }
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Configurações",
                    tint     = gold,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "GASTO ESTE MÊS",
                    style       = MaterialTheme.typography.labelSmall,
                    fontWeight  = FontWeight.ExtraBold,
                    color       = gold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "R$ ${"%.2f".format(monthlySpent)}",
                    style      = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = gold
                )
                Text(
                    "apenas compras aprovadas",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = gold
                )

                Spacer(Modifier.height(20.dp))
                Divider(color = gold.copy(alpha = 0.12f))
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = Color(0xFF81C784).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Savings, null,
                                tint     = Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "PROTEGIDO PELO GUARDIÃO",
                            style       = MaterialTheme.typography.labelSmall,
                            color       = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            letterSpacing = 1.sp,
                            fontSize    = 9.sp
                        )
                        Text(
                            "R$ ${"%.2f".format(savedAmount)}",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF81C784)
                        )
                    }
                }
            }
        }

        if (isNightRisk) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Nightlight, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Alerta noturno",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error)
                        Text("À noite o autocontrole está no mínimo. Evite compras agora — você agradece amanhã.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            lineHeight = 17.sp)
                    }
                }
            }
        }

        if (monthlyGoal > 0) {
            val progress = goalManager.progressFraction(savedAmount)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TrackChanges, null, tint = gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("META DO MÊS", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = gold, letterSpacing = 1.sp)
                        Spacer(Modifier.weight(1f))
                        Text("R$ ${"%.0f".format(savedAmount)} / R$ ${"%.0f".format(monthlyGoal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (progress >= 1f) Color(0xFF81C784) else gold,
                        trackColor = gold.copy(alpha = 0.15f)
                    )
                    if (progress >= 1f) {
                        Text("Meta atingida! Parabéns pelo autocontrole.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (streak > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = gold.copy(alpha = 0.1f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (streak >= 7) "" else "", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$streak dia${if (streak > 1) "s" else ""} sem impulso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = gold,
                        )
                        Text("Continue resistindo para manter sua sequência",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (last6MonthsData.any { it.second > 0 }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GASTOS MENSAIS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = gold, letterSpacing = 1.sp)
                    SpendingBarChart(data = last6MonthsData, color = gold)
                }
            }
        }

        val isPro by proManager.isPro.collectAsState()
        weeklyInsight?.let { insight ->
            if (insight.summary.isNotEmpty() && isPro) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(ViewState.CALCULATOR) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = gold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("INSIGHT SEMANAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = gold, letterSpacing = 1.sp)
                        }
                        Text(insight.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                        if (insight.motivationalMessage.isNotEmpty()) {
                            Text(insight.motivationalMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = gold.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SectionLabel("AÇÕES RÁPIDAS")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    title     = "Guardião",
                    subtitle  = "Analisar compra",
                    icon      = Icons.Outlined.Shield,
                    color     = gold,
                    onClick   = { onNavigate(ViewState.SIMULATOR) }
                )
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    title     = "Biblioteca",
                    subtitle  = "Explorar conteúdos",
                    icon      = Icons.AutoMirrored.Outlined.MenuBook,
                    color     = Color(0xFF7F77DD),
                    onClick   = { onNavigate(ViewState.LIBRARY) }
                )
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    title     = "Histórico",
                    subtitle  = "Ver análises",
                    icon      = Icons.AutoMirrored.Outlined.ReceiptLong,
                    color     = Color(0xFF81C784),
                    onClick   = { onNavigate(ViewState.HISTORY) }
                )
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    title     = "Intenções",
                    subtitle  = "Meu propósito",
                    icon      = Icons.Outlined.Lightbulb,
                    color     = Color(0xFFBA7517),
                    onClick   = { onNavigate(ViewState.INTENTIONS) }
                )
            }

        }

        SectionLabel("PROGRESSO")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(ViewState.ACHIEVEMENTS) },
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    color    = gold.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.EmojiEvents, null, tint = gold, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Conquistas",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = gold
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress   = unlockedCount.toFloat() / totalAchievements,
                        modifier   = Modifier.fillMaxWidth().height(5.dp),
                        color      = gold,
                        trackColor = gold.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$unlockedCount de $totalAchievements desbloqueadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ChevronRight, null, tint = gold.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SpendingBarChart(data: List<Pair<String, Double>>, color: Color) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    val barColor = color

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = (size.width - (data.size - 1) * 8.dp.toPx()) / data.size
                data.forEachIndexed { i, (_, value) ->
                    val fraction = if (maxVal > 0) (value / maxVal).toFloat() else 0f
                    val barHeight = size.height * fraction
                    val x = i * (barWidth + 8.dp.toPx())
                    drawRoundRect(
                        color = barColor.copy(alpha = if (i == data.lastIndex) 1f else 0.4f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp)
                    if (value > 0) {
                        Text("R${"$"}${"%.0f".format(value)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style       = MaterialTheme.typography.labelSmall,
        fontWeight  = FontWeight.Bold,
        color       = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        letterSpacing = 1.5.sp
    )
}

private fun greetingByHour(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Bom dia — tome decisões conscientes hoje"
        hour < 18 -> "Boa tarde — seu guardião está ativo"
        else      -> "Boa noite — evite compras impulsivas agora"
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = color.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text(
                    title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}