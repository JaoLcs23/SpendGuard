package com.joaolucas.spendguard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onNavigate: (ViewState) -> Unit
) {
    val gold          = MaterialTheme.colorScheme.primary
    val currentUserId = userRepository.getCurrentUserId() ?: ""
    val purchases     by database.purchaseDao().getPurchasesByUser(currentUserId).collectAsState(initial = emptyList())
    val challengeState by challengeManager.state.collectAsState()

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
                    icon      = Icons.Outlined.MenuBook,
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
                    icon      = Icons.Outlined.ReceiptLong,
                    color     = Color(0xFF81C784),
                    onClick   = { onNavigate(ViewState.HISTORY) }
                )
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    title     = "Ajustes",
                    subtitle  = "Preferências",
                    icon      = Icons.Outlined.Settings,
                    color     = Color(0xFFB0BEC5),
                    onClick   = { onNavigate(ViewState.SETTINGS) }
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