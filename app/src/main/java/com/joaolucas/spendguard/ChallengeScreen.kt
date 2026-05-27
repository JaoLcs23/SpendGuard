package com.joaolucas.spendguard

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    challengeManager: ChallengeManager,
    isPro: Boolean,
    onBack: () -> Unit,
    onShowPaywall: () -> Unit
) {
    val gold  = MaterialTheme.colorScheme.primary
    val black = Color(0xFF121212)

    val state by challengeManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        challengeManager.checkCompletion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desafio 30 Dias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                !isPro -> ProGateCard(gold, onShowPaywall)

                state is ChallengeManager.ChallengeState.Idle ->
                    IdleCard(gold, black, onStart = { challengeManager.start() })

                state is ChallengeManager.ChallengeState.Active ->
                    ActiveCard(gold, state as ChallengeManager.ChallengeState.Active)

                state is ChallengeManager.ChallengeState.Failed ->
                    FailedCard(
                        gold,
                        state as ChallengeManager.ChallengeState.Failed,
                        onRetry = { challengeManager.reset() }
                    )

                state is ChallengeManager.ChallengeState.Completed ->
                    CompletedCard(gold, black)
            }

            RulesCard(gold)
        }
    }
}

@Composable
private fun ProGateCard(gold: Color, onShowPaywall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Lock, null, tint = gold, modifier = Modifier.size(36.dp))
            Text(
                "Recurso exclusivo Pro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = gold,
                textAlign = TextAlign.Center
            )
            Text(
                "O Desafio 30 Dias é exclusivo para assinantes Pro. " +
                        "Assine para testar sua disciplina financeira ao máximo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onShowPaywall,
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.WorkspacePremium, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Assinar Pro", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IdleCard(gold: Color, black: Color, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = gold.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.EmojiEvents, null, tint = gold, modifier = Modifier.size(36.dp))
                }
            }
            Text(
                "30 dias sem impulso",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = gold,
                textAlign = TextAlign.Center
            )
            Text(
                "Prove para si mesmo que você controla seu dinheiro — não o contrário. " +
                        "Nenhuma compra por impulso durante 30 dias corridos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar desafio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ActiveCard(gold: Color, state: ChallengeManager.ChallengeState.Active) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progressFraction,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "challenge_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalFireDepartment, null, tint = gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Desafio em andamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayStat(
                    value = state.elapsedDays.toString(),
                    label = "dias completos",
                    gold  = gold
                )
                HorizontalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = gold.copy(alpha = 0.2f)
                )
                DayStat(
                    value = state.daysRemaining.toString(),
                    label = "dias restantes",
                    gold  = gold
                )
                HorizontalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = gold.copy(alpha = 0.2f)
                )
                DayStat(
                    value = "30",
                    label = "dias no total",
                    gold  = gold.copy(alpha = 0.4f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Progresso",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gold
                    )
                }
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = gold,
                    trackColor = gold.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Início: ${state.startDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
                Text(
                    "Meta: ${state.endDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun FailedCard(
    gold: Color,
    state: ChallengeManager.ChallengeState.Failed,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.SentimentDissatisfied,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "Desafio encerrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Você cedeu a um impulso no dia ${state.onDay}" +
                        if (state.itemName.isNotBlank()) " (${state.itemName})." else ".",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            Text(
                "Isso não é fracasso — é dado. Você chegou até o dia ${state.onDay}. " +
                        "Tente de novo com esse aprendizado.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tentar novamente", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletedCard(gold: Color, black: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = gold.copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.EmojiEvents, null, tint = gold, modifier = Modifier.size(42.dp))
                }
            }
            Text(
                "Desafio concluído!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = gold,
                textAlign = TextAlign.Center
            )
            Text(
                "30 dias sem nenhum impulso. Você provou que tem controle real sobre seu dinheiro. " +
                        "A conquista \"Guardião Perfeito\" foi desbloqueada.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = gold.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.MilitaryTech, null, tint = gold, modifier = Modifier.size(18.dp))
                    Text(
                        "Guardião Perfeito — desbloqueado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gold
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesCard(gold: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Regras do desafio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }
            RuleItem("Compras aprovadas racionalmente pelo Guardião são permitidas")
            RuleItem("Qualquer compra que o Guardião bloqueou e você ignorou encerra o desafio")
            RuleItem("O desafio conta dias corridos — não reinicia se você pular um dia")
            RuleItem("Ao completar, você recebe a conquista exclusiva \"Guardião Perfeito\"")
        }
    }
}

@Composable
private fun RuleItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("·", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun DayStat(value: String, label: String, gold: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = gold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}