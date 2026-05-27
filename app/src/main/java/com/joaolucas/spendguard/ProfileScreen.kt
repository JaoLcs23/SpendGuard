package com.joaolucas.spendguard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileManager: ProfileManager,
    isOnboarding: Boolean = false,
    onDone: () -> Unit
) {
    val gold  = MaterialTheme.colorScheme.primary
    val black = Color(0xFF121212)

    val current = remember { profileManager.load() }

    val incomeRanges = listOf(
        "Prefiro não informar" to 0.0,
        "Até R\$2.000"         to 1000.0,
        "R\$2.000 – R\$4.000"  to 3000.0,
        "R\$4.000 – R\$8.000"  to 6000.0,
        "R\$8.000 – R\$15.000" to 11500.0,
        "Acima de R\$15.000"   to 20000.0
    )

    var selectedIncomeIdx by remember {
        val idx = incomeRanges.indexOfFirst { it.second == current.monthlyIncome }
        mutableStateOf(if (idx < 0) 0 else idx)
    }
    var selectedGoal by remember { mutableStateOf(current.financialGoal) }
    var selectedCategories by remember { mutableStateOf(current.spendingCategories.toMutableSet()) }
    var saved by remember { mutableStateOf(false) }

    fun save() {
        profileManager.save(
            FinancialProfile(
                monthlyIncome       = incomeRanges[selectedIncomeIdx].second,
                financialGoal       = selectedGoal,
                spendingCategories  = selectedCategories.toList(),
                isComplete          = selectedGoal.isNotEmpty()
            )
        )
        if (isOnboarding) {
            onDone()
        } else {
            saved = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header — same style as IntentionsScreen
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = gold)
                }
                Text(
                    "Perfil Financeiro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }

            // Explanatory card — same style as IntentionsScreen
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, null, tint = gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Para que serve meu perfil?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                    }
                    Text(
                        "Seus dados personalizam as análises do Guardião para a sua realidade financeira. Nada é compartilhado externamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Sections
            ProfileSection(title = "Renda mensal", icon = Icons.Outlined.AccountBalance) {
                incomeRanges.forEachIndexed { idx, (label, _) ->
                    ProfileOptionRow(
                        label    = label,
                        selected = selectedIncomeIdx == idx,
                        onClick  = { selectedIncomeIdx = idx; saved = false }
                    )
                }
            }

            ProfileSection(title = "Principal objetivo", icon = Icons.Outlined.Flag) {
                FinancialGoal.values().forEach { goal ->
                    ProfileOptionRow(
                        label    = goal.label,
                        selected = selectedGoal == goal.name,
                        onClick  = { selectedGoal = goal.name; saved = false }
                    )
                }
            }

            ProfileSection(
                title    = "Onde você mais gasta",
                icon     = Icons.Outlined.ShoppingCart,
                subtitle = "Selecione todas que se aplicam"
            ) {
                ProfileSpendingCategory.values().forEach { cat ->
                    val checked = cat.name in selectedCategories
                    ProfileCheckRow(
                        label   = cat.label,
                        checked = checked,
                        onToggle = {
                            selectedCategories = selectedCategories.toMutableSet().apply {
                                if (checked) remove(cat.name) else add(cat.name)
                            }
                            saved = false
                        }
                    )
                }
            }

            // Action buttons — same style as IntentionsScreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOnboarding) {
                    OutlinedButton(
                        onClick  = { onDone() },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.SkipNext, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pular")
                    }
                }

                Button(
                    onClick  = { save() },
                    enabled  = selectedGoal.isNotEmpty() && !saved,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (saved) Icons.Outlined.Check
                        else if (isOnboarding) Icons.Outlined.ArrowForward
                        else Icons.Outlined.Save,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            saved -> "Salvo!"
                            isOnboarding -> "Continuar"
                            else -> "Salvar perfil"
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun ProfileOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        color   = if (selected) gold.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (selected)
                Icon(Icons.Outlined.CheckCircle, null, tint = gold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileCheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onToggle,
        color   = if (checked) gold.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Checkbox(
                checked         = checked,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(checkedColor = gold)
            )
        }
    }
}