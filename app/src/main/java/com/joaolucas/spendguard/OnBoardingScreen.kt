package com.joaolucas.spendguard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val highlights: List<Pair<ImageVector, String>> = emptyList(),
    val accentColor: Color? = null,
    val isProComparison: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(isReplay: Boolean = false, onFinish: () -> Unit) {
    val context        = LocalContext.current
    val gold           = MaterialTheme.colorScheme.primary
    val black          = Color(0xFF121212)
    val profileManager = remember { ProfileManager(context) }

    var showProfileStep      by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showProfileStep) {
        ProfileScreen(
            profileManager = profileManager,
            isOnboarding   = true,
            onDone         = { showPermissionDialog = true; showProfileStep = false }
        )
        return
    }

    val pages = listOf(
        OnboardingPage(
            icon        = Icons.Outlined.Shield,
            title       = "Bem-vindo ao SpendGuard",
            description = "Seu guardião financeiro pessoal. Inteligência Artificial aplicada a cada compra para que você gaste com consciência e construa a liberdade financeira que merece.",
            highlights  = listOf(
                Icons.Outlined.Psychology       to "IA analisa cada compra em tempo real",
                Icons.Outlined.LocalFireDepartment to "Streak de dias sem impulso",
                Icons.Outlined.TrackChanges     to "Meta mensal de economia personalizada",
                Icons.Outlined.EmojiEvents      to "Conquistas que recompensam disciplina"
            )
        ),

        OnboardingPage(
            icon        = Icons.Outlined.Psychology,
            title       = "Guardião Anti-Impulso",
            description = "Descreva o que quer comprar. O Guardião analisa sua justificativa com IA e separa necessidade real de impulso — antes que o dinheiro saia da sua conta.",
            highlights  = listOf(
                Icons.Outlined.Mood             to "Check-in emocional antes de cada análise",
                Icons.Outlined.AutoAwesome      to "IA considera emoção e horas de trabalho no veredicto",
                Icons.Outlined.Timer            to "Reflexão de 24h a 1 semana conforme o valor",
                Icons.Outlined.Lightbulb        to "Sua intenção financeira aparece antes de cada análise"
            )
        ),

        OnboardingPage(
            icon        = Icons.Outlined.MenuBook,
            title       = "Biblioteca Financeira",
            description = "Conteúdos selecionados com critério — livros, vídeos, artigos e cursos com valor comprovado ao longo do tempo. Aprenda no momento certo, não antes.",
            accentColor = Color(0xFF7F77DD),
            highlights  = listOf(
                Icons.Outlined.Explore          to "Catálogo completo com busca e filtros por tipo",
                Icons.Outlined.BookmarkAdd      to "Salve conteúdos na sua biblioteca pessoal",
                Icons.Outlined.Lightbulb        to "Sugestões no histórico quando você mais precisa",
                Icons.Outlined.Lock             to "Acesso completo e ilimitado — gratuito para sempre"
            )
        ),

        OnboardingPage(
            icon        = Icons.Outlined.NotificationsActive,
            title       = "Detecção Automática",
            description = "O SpendGuard detecta compras em tempo real nas notificações de apps como Shopee, Shein, Mercado Livre e abre o Guardião automaticamente.",
            highlights  = listOf(
                Icons.Outlined.ShoppingBag      to "Shopee, Shein, Mercado Livre, Amazon e mais",
                Icons.Outlined.Payment          to "Detecção de PIX enviados pelo celular",
                Icons.Outlined.Lock             to "Lemos apenas notificações de compra",
                Icons.Outlined.PhoneAndroid     to "Tudo processado no seu dispositivo"
            )
        ),

        OnboardingPage(
            icon        = Icons.Outlined.Widgets,
            title       = "Registro sem Atrito",
            description = "O widget na tela inicial deixa você registrar um gasto em um toque — escolha a categoria e o Guardião abre pronto para analisar. Quanto menos fricção, mais você registra.",
            accentColor = Color(0xFF4FC3F7),
            highlights  = listOf(
                Icons.Outlined.TouchApp         to "Um toque na categoria já abre o Guardião",
                Icons.Outlined.Category         to "Comida, Lazer, Roupa e Tecnologia de atalho",
                Icons.Outlined.Savings          to "Resumo semanal de economia direto na tela inicial",
                Icons.Outlined.Widgets          to "Tamanho fixo 4×2 — limpo e informativo"
            )
        ),

        OnboardingPage(
            icon        = Icons.Outlined.EmojiEvents,
            title       = "Histórico e Conquistas",
            description = "Acompanhe sua evolução, desbloqueie conquistas por disciplina financeira e exporte seu histórico completo.",
            highlights  = listOf(
                Icons.Outlined.BarChart         to "Gráfico de gastos dos últimos 6 meses",
                Icons.Outlined.DateRange        to "Filtro por período personalizado com calendário",
                Icons.Outlined.MilitaryTech     to "Conquistas por marcos de disciplina financeira",
                Icons.Outlined.FileDownload     to "Exporte sua planilha financeira com segurança biométrica"
            )
        ),

        OnboardingPage(
            icon            = Icons.Outlined.WorkspacePremium,
            title           = "Grátis vs Pro",
            description     = "Comece grátis. Evolua quando quiser.",
            isProComparison = true
        ),

        OnboardingPage(
            icon        = Icons.Outlined.Lock,
            title       = "Seus Dados, Sua Privacidade",
            description = "O SpendGuard foi desenvolvido em conformidade com a LGPD. Sem rastreamento, sem venda de dados, sem surpresas.",
            highlights  = listOf(
                Icons.Outlined.PhonelinkLock        to "Histórico salvo apenas no seu celular",
                Icons.Outlined.VisibilityOff        to "A IA analisa apenas o texto da compra",
                Icons.Outlined.PersonOff            to "Nenhum dado pessoal coletado",
                Icons.Outlined.SettingsBackupRestore to "Revogue permissões a qualquer momento"
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    val proScrollState     = rememberScrollState()
    val isProPage          = pages[pagerState.currentPage].isProComparison
    val showBottomControls = !isProPage || !proScrollState.canScrollForward

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon  = { Icon(Icons.Outlined.NotificationsActive, null, tint = gold) },
            title = { Text("Autorizar leitura de notificações?", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "O SpendGuard lerá apenas notificações de apps de compra " +
                            "(Shopee, Shein, Mercado Livre, etc.).\n\n" +
                            "O texto da notificação é enviado ao Gemini (Google) apenas para " +
                            "identificar produto e preço — sem armazenamento.\n\n" +
                            "Você pode revogar a qualquer momento em:\n" +
                            "Configurações > Apps > Acesso especial > Notificações",
                    style      = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)
                ) { Text("Autorizar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false; onFinish() }) {
                    Text("Pular por agora")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(black)) {

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val p = pages[page]
            if (p.isProComparison) {
                ProComparisonPageContent(scrollState = proScrollState)
            } else {
                OnboardingPageContent(page = p)
            }
        }

        AnimatedVisibility(
            visible  = showBottomControls,
            enter    = fadeIn(animationSpec = tween(300)),
            exit     = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier              = Modifier.padding(bottom = 48.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(20.dp)
            ) {
                AnimatedVisibility(visible = pagerState.currentPage == 0, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowRight, null,
                            tint     = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp))
                        Text(
                            "deslize para avançar",
                            style         = MaterialTheme.typography.labelSmall,
                            color         = Color.White.copy(alpha = 0.3f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected   = pagerState.currentPage == index
                        val accentColor  = pages[index].accentColor ?: gold
                        val dotColor     = if (isSelected) accentColor else Color.White.copy(alpha = 0.25f)
                        val dotWidth     by animateFloatAsState(
                            targetValue   = if (isSelected) 28f else 8f,
                            animationSpec = tween(250, easing = EaseOutCubic),
                            label         = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = dotWidth.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(dotColor)
                        )
                    }
                }

                AnimatedVisibility(visible = isLastPage, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Button(
                            onClick = { if (isReplay) onFinish() else showProfileStep = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)
                        ) {
                            Icon(
                                if (isReplay) Icons.Outlined.Check else Icons.Outlined.Shield,
                                null, tint = black, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isReplay) "Finalizar revisão" else "Começar agora",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !isLastPage, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pages.size - 1) } }) {
                        Text(
                            "Pular introdução",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val defaultGold = MaterialTheme.colorScheme.primary
    val accent      = page.accentColor ?: defaultGold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 32.dp, end = 32.dp, top = 80.dp, bottom = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f, fill = false))

        Surface(
            shape    = RoundedCornerShape(28.dp),
            color    = accent.copy(alpha = 0.15f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = page.icon,
                    contentDescription = null,
                    tint               = accent,
                    modifier           = Modifier.size(52.dp)
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text       = page.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center,
            color      = Color.White
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text       = page.description,
            style      = MaterialTheme.typography.bodyLarge,
            textAlign  = TextAlign.Center,
            color      = Color.White.copy(alpha = 0.72f),
            lineHeight = 26.sp
        )

        if (page.highlights.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    page.highlights.forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape    = RoundedCornerShape(50),
                                color    = accent.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint     = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text  = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = false))
    }
}

private data class FeatureRow(
    val icon: ImageVector,
    val label: String,
    val free: String,
    val pro: String
)

@Composable
fun ProComparisonPageContent(scrollState: ScrollState) {
    val gold  = MaterialTheme.colorScheme.primary
    val green = Color(0xFF81C784)

    val rows = listOf(
        FeatureRow(Icons.Outlined.Psychology,          "Análises com Guardião",        "5/semana",  "Ilimitadas"),
        FeatureRow(Icons.Outlined.Mood,                "Check-in emocional",            "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.Lightbulb,           "Intenções financeiras",         "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.LocalFireDepartment, "Streak anti-impulso",           "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.TrackChanges,        "Meta mensal de economia",       "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.Widgets,             "Widget de registro rápido",     "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.MenuBook,            "Biblioteca financeira",         "Completa",  "Completa"),
        FeatureRow(Icons.Outlined.History,             "Histórico de análises",         "Sim",       "Sim"),
        FeatureRow(Icons.Outlined.EmojiEvents,         "Conquistas",                    "Base",      "+ exclusivas"),
        FeatureRow(Icons.Outlined.FileDownload,        "Exportar planilha",             "—",         "Sim"),
        FeatureRow(Icons.Outlined.FileUpload,          "Importar extrato bancário",     "—",         "Sim"),
        FeatureRow(Icons.Outlined.NotificationsActive, "Detecção automática de compras","—",         "Sim"),
        FeatureRow(Icons.Outlined.Shield,              "Modo estrito",                  "—",         "Sim"),
        FeatureRow(Icons.Outlined.WifiOff,             "Análise offline",               "—",         "Sim"),
        FeatureRow(Icons.Outlined.AutoAwesome,         "Insight semanal por IA",        "—",         "Sim"),
        FeatureRow(Icons.Outlined.CardGiftcard,        "Programa de indicação",         "—",         "7 dias grátis")
    )

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 140.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = gold.copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = gold, modifier = Modifier.size(36.dp))
            }
        }

        Text(
            "Grátis vs Pro",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            textAlign  = TextAlign.Center
        )
        Text(
            "Comece grátis. Evolua quando quiser.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A0A))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {

                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(2.2f))
                    Text(
                        "Grátis",
                        modifier   = Modifier.weight(1.3f),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White.copy(alpha = 0.45f),
                        textAlign  = TextAlign.Center
                    )
                    Row(
                        modifier              = Modifier.weight(1.5f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.WorkspacePremium, null,
                            tint = gold, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Pro",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color      = gold)
                    }
                }

                Divider(color = gold.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 14.dp))

                rows.forEachIndexed { idx, row ->
                    val isProOnly = row.free == "—"
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .background(if (isProOnly) gold.copy(alpha = 0.04f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            row.icon, null,
                            tint     = if (isProOnly) gold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            row.label,
                            modifier = Modifier.weight(2.2f),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (isProOnly) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        FeatureCell(row.free, Modifier.weight(1.3f), gold, green, false)
                        FeatureCell(row.pro,  Modifier.weight(1.5f), gold, green, true)
                    }
                    if (idx < rows.size - 1)
                        Divider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFF2A2200)),
            border   = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.25f))
        ) {
            Column(
                modifier              = Modifier.padding(16.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = gold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SpendGuard Pro",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = gold)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text("R$", style = MaterialTheme.typography.bodyMedium, color = gold.copy(alpha = 0.7f))
                    Spacer(Modifier.width(2.dp))
                    Text("9,90",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = gold)
                    Spacer(Modifier.width(4.dp))
                    Text("/mês",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = gold.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(
                    "ou R$ 69,90/ano — economia de 41%",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Você pode assinar a qualquer momento dentro do app.",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = Color.White.copy(alpha = 0.28f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureCell(
    value: String,
    modifier: Modifier,
    gold: Color,
    green: Color,
    isPro: Boolean
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (value) {
            "—"   -> Text("—",
                style     = MaterialTheme.typography.labelSmall,
                color     = Color.White.copy(alpha = 0.18f),
                textAlign = TextAlign.Center)
            "Sim" -> Icon(
                Icons.Outlined.CheckCircle, null,
                tint     = if (isPro) gold else green.copy(alpha = 0.65f),
                modifier = Modifier.size(15.dp)
            )
            else  -> Text(
                value,
                style      = MaterialTheme.typography.labelSmall,
                color      = if (isPro) gold else Color.White.copy(alpha = 0.55f),
                textAlign  = TextAlign.Center,
                fontWeight = if (isPro) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}