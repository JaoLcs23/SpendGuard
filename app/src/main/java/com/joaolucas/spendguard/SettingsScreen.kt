package com.joaolucas.spendguard

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: SpendGuardDatabase,
    proManager: ProManager,
    billingManager: BillingManager,
    userRepository: UserRepository,
    themeManager: ThemeManager,
    goalManager: GoalManager,
    onSignOut: () -> Unit,
    onOpenOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gold = MaterialTheme.colorScheme.primary

    var notificationsEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog      by remember { mutableStateOf(false) }
    var showSignOutDialog      by remember { mutableStateOf(false) }
    var showDataDialog         by remember { mutableStateOf(false) }
    var showPaywall            by remember { mutableStateOf(false) }
    var showProfileScreen      by remember { mutableStateOf(false) }
    var showThemeDialog        by remember { mutableStateOf(false) }
    var showGoalDialog         by remember { mutableStateOf(false) }
    var goalInput              by remember { mutableStateOf(goalManager.getMonthlyGoal().let { if (it > 0) "%.0f".format(it) else "" }) }
    val currentGoal            by goalManager.monthlyGoal.collectAsState()

    val currentUser by userRepository.currentUser.collectAsState()
    val isPro       by proManager.isPro.collectAsState()
    val plan        by proManager.plan.collectAsState()
    val currentTheme by themeManager.theme.collectAsState()
    val profileManager = remember { ProfileManager(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon  = { Icon(Icons.Outlined.Palette, null, tint = gold) },
            title = { Text("Aparência", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTheme.values().forEach { theme ->
                        val (icon, desc) = when (theme) {
                            AppTheme.DARK   -> Icons.Outlined.DarkMode    to "Fundo escuro com acento dourado"
                            AppTheme.LIGHT  -> Icons.Outlined.LightMode   to "Fundo claro com acento dourado"
                            AppTheme.SYSTEM -> Icons.Outlined.SettingsBrightness to "Segue a preferência do sistema"
                        }
                        Surface(
                            onClick = {
                                themeManager.setTheme(theme)
                                showThemeDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentTheme == theme) gold.copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    null,
                                    tint = if (currentTheme == theme) gold
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        theme.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentTheme == theme) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentTheme == theme) gold
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                if (currentTheme == theme) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        null,
                                        tint = gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Fechar") }
            }
        )
    }

    if (showDataDialog) {
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            icon  = { Icon(Icons.Outlined.Info, null, tint = gold) },
            title = { Text("Como seus dados são usados", fontWeight = FontWeight.Bold) },
            text  = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "O SpendGuard funciona 100% local para seus dados pessoais:\n\n" +
                                "• Histórico de compras — salvo apenas no seu celular\n" +
                                "• Notificações lidas — processadas localmente e descartadas\n" +
                                "• Análise de IA — o texto da compra é enviado ao Gemini (Google) apenas para gerar a resposta, sem armazenamento\n\n" +
                                "Nenhum servidor próprio do SpendGuard recebe seus dados.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                ) { Text("Entendi", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon  = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar histórico?", fontWeight = FontWeight.Bold) },
            text  = { Text("Todos os registros de compras serão apagados permanentemente. Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { database.purchaseDao().deleteAll() }
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Limpar tudo") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon  = { Icon(Icons.Outlined.Lock, null, tint = gold) },
            title = { Text("Política de Privacidade", fontWeight = FontWeight.Bold) },
            text  = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "POLÍTICA DE PRIVACIDADE — SpendGuard\n" +
                                "Versão 1.0 | Março de 2026\n\n" +
                                "1. RESPONSÁVEL PELO TRATAMENTO\n" +
                                "SpendGuard é um aplicativo independente desenvolvido para auxiliar no controle de gastos pessoais.\n\n" +
                                "2. DADOS TRATADOS\n" +
                                "• Registros de compras inseridos pelo usuário\n" +
                                "• Texto de notificações de apps de compra (temporário)\n\n" +
                                "3. FINALIDADE\n" +
                                "Os dados são usados exclusivamente para análise financeira pessoal dentro do próprio dispositivo.\n\n" +
                                "4. COMPARTILHAMENTO\n" +
                                "O texto das análises é enviado à API Gemini (Google LLC) para processamento momentâneo. Nenhum dado é retido por nossos servidores.\n\n" +
                                "5. SEUS DIREITOS (LGPD)\n" +
                                "Você pode excluir todos os dados a qualquer momento em Configurações > Limpar histórico.\n\n" +
                                "6. CONTATO\n" +
                                "contato@spendguard.com.br",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                ) { Text("Fechar", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = gold.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            null,
                            tint = gold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.email ?: "Usuário",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isPro) gold.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (isPro) Icons.Outlined.WorkspacePremium else Icons.Outlined.Person,
                                null,
                                tint = if (isPro) gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                if (isPro) "Pro ativo" else "Plano Gratuito",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPro) gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (isPro) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                TextButton(onClick = { showSignOutDialog = true }) {
                    Text("Sair", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                icon  = { Icon(Icons.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Sair da conta?", fontWeight = FontWeight.Bold) },
                text  = { Text("Seus dados locais serão mantidos. Você precisará entrar novamente para sincronizar com a nuvem.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            scope.launch {
                                userRepository.signOut()
                                proManager.deactivatePro()
                                onSignOut()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sair") }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) { Text("Cancelar") }
                }
            )
        }

        Spacer(Modifier.height(4.dp))
        if (isPro) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        null,
                        tint = gold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SpendGuard Pro ativo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Text(
                            if (plan == "yearly") "Plano anual" else if (plan == "trial") "Período de teste" else "Plano mensal",
                            style = MaterialTheme.typography.bodySmall,
                            color = gold.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { showPaywall = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        null,
                        tint = gold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Upgrade para Pro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Análises ilimitadas + notificações automáticas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        null,
                        tint = gold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SettingsSectionTitle("Personalização do Guardião", Icons.Outlined.Psychology)
        SettingsCard {
            val profile = remember { profileManager.load() }
            SettingsItem(
                icon    = Icons.Outlined.TrendingUp,
                title   = "Contexto financeiro",
                subtitle = if (profile.isComplete)
                    "Renda: ${profile.incomeLabel()} · Objetivo: ${profile.goalLabel()}"
                else
                    "Informe sua renda e objetivo para análises mais precisas da IA",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (!profile.isComplete)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                if (!profile.isComplete) "Configurar" else "Editar",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!profile.isComplete)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onClick = { showProfileScreen = true }
            )
        }

        Spacer(Modifier.height(4.dp))
        SettingsSectionTitle("Aparência", Icons.Outlined.Palette)
        SettingsCard {
            SettingsItem(
                icon    = when (currentTheme) {
                    AppTheme.DARK   -> Icons.Outlined.DarkMode
                    AppTheme.LIGHT  -> Icons.Outlined.LightMode
                    AppTheme.SYSTEM -> Icons.Outlined.SettingsBrightness
                },
                title   = "Tema",
                subtitle = currentTheme.label,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = gold.copy(alpha = 0.12f)
                        ) {
                            Text(
                                currentTheme.label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                },
                onClick = { showThemeDialog = true }
            )
        }

        Spacer(Modifier.height(4.dp))
        SettingsSectionTitle("Notificações", Icons.Outlined.Notifications)
        SettingsCard {
            SettingsItem(
                icon    = Icons.Outlined.NotificationsActive,
                title   = "Leitura de notificações de compras",
                subtitle = if (notificationsEnabled)
                    "Ativo — detecta compras em apps de e-commerce e PIX"
                else
                    "Inativo — toque para ativar",
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            context.startActivity(
                                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF121212),
                            checkedTrackColor = MaterialTheme.colorScheme.inversePrimary,
                        )
                    )
                }
            )
        }

        Spacer(Modifier.height(4.dp))
        SettingsSectionTitle("Meta de Economia", Icons.Outlined.TrackChanges)
        SettingsCard {
            SettingsItem(
                icon     = Icons.Outlined.TrackChanges,
                title    = "Meta mensal de economia",
                subtitle = if (currentGoal > 0) "R$ ${"%.2f".format(currentGoal)} por mês" else "Nenhuma meta definida",
                onClick  = { showGoalDialog = true }
            )
        }

        Spacer(Modifier.height(4.dp))
        SettingsSectionTitle("Privacidade e Dados", Icons.Outlined.Security)
        SettingsCard {
            SettingsItem(
                icon    = Icons.Outlined.Info,
                title   = "Como seus dados são usados",
                subtitle = "Tudo fica no seu celular — 100% privado",
                onClick = { showDataDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon       = Icons.Outlined.Delete,
                title      = "Limpar histórico de compras",
                subtitle   = "Apaga todos os registros permanentemente",
                onClick    = { showClearHistoryDialog = true },
                titleColor = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))
        SettingsSectionTitle("Sobre o App", Icons.Outlined.Info)
        SettingsCard {
            SettingsItem(
                icon    = Icons.Outlined.Star,
                title   = "Versão",
                subtitle = "SpendGuard 1.0.0"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon    = Icons.Outlined.Refresh,
                title   = "Ver introdução novamente",
                subtitle = "Relembre como o Guardião funciona",
                onClick = onOpenOnboarding
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon    = Icons.Outlined.Lock,
                title   = "Política de Privacidade",
                subtitle = "LGPD — seus direitos e como os respeitamos",
                onClick = { showPrivacyDialog = true }
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsSectionTitle("Conta", Icons.Outlined.AccountCircle)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSignOutDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Logout, null,
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Sair da conta",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Shield,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = gold
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Conformidade LGPD",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = gold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "O SpendGuard não coleta, armazena ou compartilha dados pessoais. " +
                                "Todo processamento de IA ocorre de forma pontual via API Gemini, sem retenção.",
                        style = MaterialTheme.typography.bodySmall,
                        color = gold.copy(alpha = 0.8f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (BuildConfig.DEBUG) {
            DebugProPanel(proManager = proManager, isPro = isPro, plan = plan, gold = gold)
            DebugNotificationPanel(context = context)
        }
    }

    if (showProfileScreen) {
        ProfileScreen(
            profileManager = profileManager,
            isOnboarding   = false,
            onDone         = { showProfileScreen = false }
        )
        return
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            icon  = { Icon(Icons.Outlined.TrackChanges, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Meta mensal", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Defina quanto quer economizar por mês. Essa meta aparece no dashboard como barra de progresso.")
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Meta em R$") },
                        prefix = { Text("R$ ") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (currentGoal > 0) {
                        TextButton(onClick = { goalManager.clearGoal(); goalInput = ""; showGoalDialog = false }) {
                            Text("Remover meta", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = goalInput.toDoubleOrNull()
                        if (value != null && value > 0) {
                            goalManager.setMonthlyGoal(value)
                            showGoalDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPaywall) {
        PaywallScreen(
            proManager     = proManager,
            billingManager = billingManager,
            reason         = PaywallReason.GENERIC,
            onDismiss      = { showPaywall = false }
        )
    }
}

@Composable
private fun DebugProPanel(
    proManager: ProManager,
    isPro: Boolean,
    plan: String,
    gold: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF7F77DD).copy(alpha = 0.2f)) {
                    Text(
                        "DEBUG",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7F77DD),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Simulação de plano",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAFA9EC)
                )
            }
            Text(
                "Status atual: ${if (isPro) "Pro ($plan)" else "Gratuito"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isPro) gold else Color.White.copy(alpha = 0.5f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { proManager.activatePro("monthly") },
                    enabled = !isPro || plan != "monthly",
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ativar mensal", style = MaterialTheme.typography.labelSmall, color = gold)
                }
                OutlinedButton(
                    onClick = { proManager.activateTrialPro(days = 7) },
                    enabled = !isPro || plan != "trial",
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F77DD).copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trial 7 dias", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAFA9EC))
                }
            }
            if (isPro) {
                OutlinedButton(
                    onClick = { proManager.deactivatePro() },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Desativar Pro (voltar ao gratuito)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                "Não aparece em builds de release.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DebugNotificationPanel(context: android.content.Context) {
    val debugPurple = Color(0xFF7F77DD)
    val debugPurpleLight = Color(0xFFAFA9EC)
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf("") }

    val scenarios = listOf(
        Triple(
            "🛒 Compra real (ML)",
            "Pedido confirmado. Você comprou: T\u00eanis Nike Air Max 90 · R\$ 349,90. Seu pedido #ML-88271 foi aprovado.",
            true
        ),
        Triple(
            "💳 PIX enviado (Nubank)",
            "PIX enviado. Voc\u00ea transferiu R\$ 150,00 para Jo\u00e3o Silva. Pagamento PIX conclu\u00eddo com sucesso.",
            true
        ),
        Triple(
            "📧 Promo\u00e7\u00e3o (deve ignorar)",
            "Oferta especial para voc\u00ea! Aproveite at\u00e9 50% de desconto nos melhores produtos. Ver oferta agora!",
            false
        ),
        Triple(
            "📄 Nota Fiscal (deve ignorar)",
            "Sua nota fiscal est\u00e1 dispon\u00edvel. Pedido #ML-88271 — acesse sua nota fiscal eletr\u00f4nica.",
            false
        ),
        Triple(
            "📦 Saiu p/ entrega (deve ignorar)",
            "Seu pedido saiu para entrega! Previs\u00e3o de entrega: amanh\u00e3 at\u00e9 22h. Objeto postado em SP.",
            false
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = debugPurple.copy(alpha = 0.07f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, debugPurple.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = debugPurple.copy(alpha = 0.2f)) {
                    Text(
                        "DEBUG",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = debugPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Testar notifica\u00e7\u00f5es",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = debugPurpleLight
                )
            }

            Text(
                "Dispara o pipeline completo (filtros \u2192 Gemini \u2192 dedup 12h). " +
                "Itens marcados com \u274c n\u00e3o devem gerar notifica\u00e7\u00e3o.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )

            scenarios.forEach { (label, rawText, shouldNotify) ->
                OutlinedButton(
                    onClick = {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val listener = DebugNotificationHelper(context)
                                listener.fireTestNotification(rawText, shouldNotify)
                                lastResult = "$label — disparado"
                            } catch (e: Exception) {
                                lastResult = "Erro: ${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (shouldNotify) debugPurple.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (shouldNotify) debugPurpleLight
                                    else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (shouldNotify) "\u2705 deve notificar" else "\u274c deve ignorar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (shouldNotify) Color(0xFF81C784).copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (lastResult.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = debugPurple.copy(alpha = 0.1f)
                ) {
                    Text(
                        "\u00daltimo: $lastResult",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = debugPurpleLight
                    )
                }
            }

            Text(
                "N\u00e3o aparece em builds de release.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (titleColor == Color.Unspecified)
                        MaterialTheme.colorScheme.onSurface else titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(context.packageName)
}