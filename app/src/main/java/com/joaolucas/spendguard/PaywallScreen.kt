package com.joaolucas.spendguard

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(
    proManager: ProManager,
    billingManager: BillingManager,
    reason: PaywallReason = PaywallReason.GENERIC,
    referralManager: ReferralManager? = null,
    onDismiss: () -> Unit
) {
    val gold = Color(0xFFFFD700)
    val black = Color(0xFF121212)
    val card = Color(0xFF2A2A2A)

    var selectedPlan by remember { mutableStateOf("yearly") }
    val billingState by billingManager.billingState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val myReferralCode = remember { referralManager?.getMyCode() }

    LaunchedEffect(billingState) {
        if (billingState is BillingManager.BillingState.Success) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null,
                        tint = gold, modifier = Modifier.size(44.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("SpendGuard Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (reason) {
                        PaywallReason.GUARDIAN_LIMIT    -> "Você usou suas 5 análises grátis desta semana no Guardião"
                        PaywallReason.CALCULATOR_LIMIT  -> "Você usou suas 5 análises grátis desta semana na Calculadora"
                        PaywallReason.NOTIFICATIONS     -> "Detecção automática de compras é exclusiva do Pro"
                        PaywallReason.GENERIC           -> "Desbloqueie todo o potencial do SpendGuard"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = card)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tudo incluso no Pro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = gold)
                        Spacer(modifier = Modifier.height(16.dp))
                        ProBenefit(Icons.Outlined.AllInclusive, "Análises ilimitadas no Guardião", gold)
                        ProBenefit(Icons.Outlined.Calculate, "Calculadora de Oportunidade ilimitada", gold)
                        ProBenefit(Icons.Outlined.NotificationsActive, "Detecção automática de compras", gold)
                        ProBenefit(Icons.Outlined.History, "Histórico completo sem limite", gold)
                        ProBenefit(Icons.Outlined.SupportAgent, "Suporte prioritário", gold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Escolha seu plano",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                PlanCard(
                    selected = selectedPlan == "yearly",
                    onClick = { selectedPlan = "yearly" },
                    title = "Anual",
                    price = "R$ 69,90",
                    subtitle = "R$ 5,83/mês — economize 41%",
                    badge = "MAIS POPULAR",
                    gold = gold, card = card
                )

                Spacer(modifier = Modifier.height(10.dp))

                PlanCard(
                    selected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" },
                    title = "Mensal",
                    price = "R$ 9,90",
                    subtitle = "por mês — cancele quando quiser",
                    badge = null,
                    gold = gold, card = card
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (billingState is BillingManager.BillingState.Error) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                (billingState as BillingManager.BillingState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                val isLoading = billingState is BillingManager.BillingState.Loading

                Button(
                    onClick = {
                        activity ?: return@Button
                        val productId = if (selectedPlan == "yearly")
                            BillingManager.PRODUCT_YEARLY
                        else
                            BillingManager.PRODUCT_MONTHLY

                        scope.launch {
                            billingManager.launchBillingFlow(activity, productId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF121212),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processando...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Outlined.WorkspacePremium, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedPlan == "yearly") "Assinar por R$ 69,90/ano"
                            else "Assinar por R$ 9,90/mês",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Cancele a qualquer momento · Sem taxa de cancelamento",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )

                if (myReferralCode != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A00))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CardGiftcard,
                                    contentDescription = null,
                                    tint = gold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Indique e ganhe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gold
                                )
                            }

                            Text(
                                "Compartilhe seu código e ganhe 7 dias Pro grátis quando alguém se cadastrar. " +
                                        "O indicado recebe 30% off no primeiro mês.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 18.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2A2200))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    myReferralCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = gold,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    "SEU CÓDIGO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gold.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareText = referralManager?.getShareText() ?: return@OutlinedButton
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartilhar convite"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Compartilhar convite",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Continuar com a versão gratuita",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        textDecoration = TextDecoration.Underline)
                }
            }
        }
    }
}

@Composable
private fun ProBenefit(icon: ImageVector, text: String, gold: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun PlanCard(
    selected: Boolean, onClick: () -> Unit,
    title: String, price: String, subtitle: String,
    badge: String?, gold: Color, card: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) gold else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (selected) gold.copy(alpha = 0.08f) else card)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = gold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(20.dp), color = gold) {
                            Text(badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF121212),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp)
                        }
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f))
            }
            Text(price, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) gold else Color.White)
        }
    }
}

enum class PaywallReason {
    GUARDIAN_LIMIT,
    CALCULATOR_LIMIT,
    NOTIFICATIONS,
    GENERIC
}