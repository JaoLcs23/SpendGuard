package com.joaolucas.spendguard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val guardianQuotes = listOf(
    "\"A maior vitória é aquela sobre si mesmo.\" — Platão",
    "\"Discipline is the bridge between goals and accomplishment.\" — Jim Rohn",
    "\"Não é o quanto você ganha, mas o quanto você guarda.\" — Robert Kiyosaki",
    "\"A tentação mais perigosa é a que não parece perigosa.\" — C.S. Lewis",
    "\"Cada real poupado hoje é um passo em direção à liberdade amanhã.\"",
    "\"Comprar por impulso é emprestar felicidade do futuro.\"",
    "\"A pausa antes de comprar vale mais que qualquer desconto.\"",
    "\"Quem controla os gastos controla o destino.\"",
    "\"Emoção passa; a dívida fica. Pense antes de comprar.\"",
    "\"A consciência financeira começa na próxima compra.\""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    geminiService: GeminiService,
    database: SpendGuardDatabase,
    proManager: ProManager,
    billingManager: BillingManager,
    userRepository: UserRepository,
    achievementsManager: AchievementsManager,
    referralManager: ReferralManager,
    challengeManager: ChallengeManager,
    streakManager: StreakManager,
    intentionsManager: IntentionsManager,
    autoItemName: String = "",
    autoItemPrice: Double = 0.0,
    isPix: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager(context) }
    val userProfile = remember { profileManager.load() }
    val currentUserId = remember { userRepository.getCurrentUserId() ?: "" }
    val focusManager = LocalFocusManager.current

    val itemNameState      = rememberSaveable { mutableStateOf(autoItemName) }
    val itemPriceState     = rememberSaveable { mutableStateOf(if (autoItemPrice > 0) autoItemPrice.toString() else "") }
    val justificationState = rememberSaveable { mutableStateOf("") }
    var itemName      by itemNameState
    var itemPrice     by itemPriceState
    var justification by justificationState

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    var result    by remember { mutableStateOf<InterventionResult?>(null) }
    var selectedEmotion by remember { mutableStateOf<EmotionalState?>(null) }
    var showEmotionPicker by remember { mutableStateOf(false) }
    val currentIntention by intentionsManager.intention.collectAsState()

    val gold  = MaterialTheme.colorScheme.primary
    val quote = remember { guardianQuotes.random() }

    LaunchedEffect(autoItemName, autoItemPrice) {
        if (autoItemName.isNotEmpty()) itemName = autoItemName
        if (autoItemPrice > 0.0) itemPrice = autoItemPrice.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.FormatQuote,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        if (currentIntention.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = gold.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Sua intenção", style = MaterialTheme.typography.labelSmall,
                            color = gold.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(currentIntention, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                    }
                }
            }
        }

        if (!showEmotionPicker && selectedEmotion == null && result == null) {
            OutlinedButton(
                onClick = { showEmotionPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Mood, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Como você está agora?", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (showEmotionPicker) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Como você está agora?", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = gold)
                    Text("Isso ajuda o Guardião a calibrar a análise para seu estado atual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val emotionRows = EmotionalState.values().toList().chunked(2)
                    emotionRows.forEach { rowItems ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { emotion ->
                                val isSelected = selectedEmotion == emotion
                                Surface(
                                    onClick = {
                                        selectedEmotion = emotion
                                        showEmotionPicker = false
                                    },
                                    modifier  = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape     = RoundedCornerShape(12.dp),
                                    color     = if (isSelected) gold.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.surface,
                                    border    = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) gold
                                                else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier              = Modifier.fillMaxSize(),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(emotion.emoji, fontSize = 18.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            emotion.label,
                                            fontSize   = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color      = if (isSelected) gold
                                                         else MaterialTheme.colorScheme.onSurface,
                                            maxLines   = 1,
                                            softWrap   = false
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        selectedEmotion?.let { emotion ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = gold.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emotion.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Você está ${emotion.label.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { selectedEmotion = null; showEmotionPicker = false },
                        contentPadding = PaddingValues(0.dp)) {
                        Text("Mudar", fontSize = 11.sp)
                    }
                }
            }
        }

        if (isPix) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = gold.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Pix, contentDescription = null, tint = gold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Transferência PIX detectada — reflita antes de confirmar",
                        color = gold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = "Descreva o que você quer comprar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it; result = null },
                    label = { Text("O que você quer comprar?") },
                    leadingIcon = {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = gold)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor  = gold,
                        cursorColor        = gold
                    )
                )

                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it; result = null },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = gold)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor  = gold,
                        cursorColor        = gold
                    )
                )

                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it; result = null },
                    label = { Text("Por que você precisa disso agora?") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Psychology, contentDescription = null, tint = gold)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor  = gold,
                        cursorColor        = gold
                    )
                )
            }
        }

        Button(
            onClick = {
                focusManager.clearFocus()

                val price = itemPrice.replace(",", ".").toDoubleOrNull()
                if (itemName.isBlank() || price == null || price <= 0 || justification.isBlank()) {
                    errorMsg = "Preencha todos os campos corretamente."
                    return@Button
                }

                errorMsg  = null
                isLoading = true
                result    = null

                scope.launch {
                    try {
                        val analysis = geminiService.analyzeImpulse(itemName, price, justification, userProfile)
                        result = analysis

                        val purchase = PurchaseEntity(
                            userId        = currentUserId,
                            itemName      = itemName,
                            price         = price,
                            justification = justification,
                            wasBlocked    = !analysis.allowed,
                            aiMessage     = analysis.message,
                            coolingOffTime = analysis.coolingOffTime,
                            category      = analysis.category
                        )
                        database.purchaseDao().insert(purchase)

                        if (!analysis.allowed && analysis.coolingOffTime > 0) {
                            val workData = androidx.work.workDataOf(
                                "item_name"       to itemName,
                                "notification_id" to System.currentTimeMillis().toInt()
                            )
                            val workRequest = androidx.work.OneTimeWorkRequestBuilder<CoolingOffWorker>()
                                .setInitialDelay(analysis.coolingOffTime.toLong(), java.util.concurrent.TimeUnit.HOURS)
                                .setInputData(workData)
                                .build()
                            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
                        }

                    } catch (e: Exception) {
                        errorMsg = "Não foi possível concluir a análise. Verifique sua conexão e tente novamente."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && itemName.isNotEmpty() && itemPrice.isNotEmpty() && justification.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = gold,
                contentColor   = Color(0xFF121212)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(24.dp),
                    color     = Color(0xFF121212),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analisando...", fontWeight = FontWeight.Bold, color = Color(0xFF121212))
            } else {
                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = Color(0xFF121212))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Analisar com Guardião IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121212)
                )
            }
        }

        errorMsg?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (result == null && errorMsg == null && !isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Como o Guardião avalia?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            GuardianInfoCard(
                icon  = Icons.Outlined.CheckCircle,
                title = "Compras Racionais",
                desc  = "A IA recompensa o planejamento prévio e as necessidades reais do seu dia a dia.",
                color = Color(0xFF81C784)
            )
            GuardianInfoCard(
                icon  = Icons.Outlined.Warning,
                title = "Gatilhos Emocionais",
                desc  = "O Guardião detecta impulsos como \"eu mereço\", \"tá barato\" ou ansiedade (FOMO).",
                color = MaterialTheme.colorScheme.error
            )
            GuardianInfoCard(
                icon  = Icons.Outlined.Timer,
                title = "Período de Reflexão",
                desc  = "Se a compra for bloqueada por impulso, o Guardião impõe uma pausa estratégica para você decidir com clareza.",
                color = Color(0xFF4FC3F7)
            )
        }

        result?.let { analysis ->
            val isBlocked   = !analysis.allowed
            val accentColor = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF81C784)
            val icon        = if (isBlocked) Icons.Outlined.Block else Icons.Outlined.CheckCircle
            val title       = if (isBlocked) "Compra Bloqueada" else "Compra Aprovada"

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }

                            Text(
                                text = analysis.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )

                            if (isBlocked && analysis.coolingOffTime > 0) {
                                Divider(color = accentColor.copy(alpha = 0.2f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Período de reflexão: ${if (analysis.coolingOffTime < 24) "${analysis.coolingOffTime}h" else if (analysis.coolingOffTime == 168) "1 semana" else "${analysis.coolingOffTime}h"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            itemName      = ""
                            itemPrice     = ""
                            justification = ""
                            result        = null
                            errorMsg      = null
                            selectedEmotion = null
                            showEmotionPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nova análise", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GuardianInfoCard(icon: ImageVector, title: String, desc: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}