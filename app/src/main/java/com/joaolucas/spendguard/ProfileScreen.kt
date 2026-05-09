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

    fun save() {
        profileManager.save(
            FinancialProfile(
                monthlyIncome       = incomeRanges[selectedIncomeIdx].second,
                financialGoal       = selectedGoal,
                spendingCategories  = selectedCategories.toList(),
                isComplete          = selectedGoal.isNotEmpty()
            )
        )
        onDone()
    }

    val content: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isOnboarding) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = gold.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = gold, modifier = Modifier.size(40.dp))
                        }
                    }
                    Text(
                        "Seu perfil financeiro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = gold
                    )
                    Text(
                        "Isso personaliza as análises do Guardião para a sua realidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            ProfileSection(title = "Renda mensal", icon = Icons.Outlined.AccountBalance) {
                incomeRanges.forEachIndexed { idx, (label, _) ->
                    ProfileOptionRow(
                        label    = label,
                        selected = selectedIncomeIdx == idx,
                        onClick  = { selectedIncomeIdx = idx }
                    )
                }
            }

            ProfileSection(title = "Principal objetivo", icon = Icons.Outlined.Flag) {
                FinancialGoal.values().forEach { goal ->
                    ProfileOptionRow(
                        label    = goal.label,
                        selected = selectedGoal == goal.name,
                        onClick  = { selectedGoal = goal.name }
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
                        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)
            ) {
                Icon(
                    if (isOnboarding) Icons.Outlined.ArrowForward else Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isOnboarding) "Continuar" else "Salvar perfil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (isOnboarding) {
                TextButton(
                    onClick  = { onDone() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Pular por agora",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (isOnboarding) {
        content(PaddingValues(0.dp))
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Perfil financeiro") },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Outlined.ArrowBack, "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            content = content
        )
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