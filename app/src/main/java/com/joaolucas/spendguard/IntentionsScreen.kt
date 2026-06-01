package com.joaolucas.spendguard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionsScreen(
    intentionsManager: IntentionsManager,
    dataSyncManager: DataSyncManager,
    database: SpendGuardDatabase,
    onBack: () -> Unit
) {
    val intentions by intentionsManager.intentions.collectAsStateWithLifecycle()
    val history by database.purchaseDao().getAllPurchases().collectAsStateWithLifecycle(emptyList())

    var text by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var editingIntention by remember { mutableStateOf<FinancialIntention?>(null) }

    val gold = MaterialTheme.colorScheme.primary

    if (editingIntention != null) {
        var editText by remember { mutableStateOf(editingIntention!!.text) }
        var editTarget by remember { mutableStateOf(if (editingIntention!!.targetAmount != null) editingIntention!!.targetAmount.toString() else "") }

        AlertDialog(
            onDismissRequest = { editingIntention = null },
            title = { Text("Editar Intenção", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Nome da intenção") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editTarget,
                        onValueChange = { editTarget = it },
                        label = { Text("Valor alvo (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Text("R$", modifier = Modifier.padding(start=16.dp)) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isNotBlank()) {
                        intentionsManager.editIntention(editingIntention!!.id, editText, editTarget.toDoubleOrNull())
                        GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                        editingIntention = null
                    }
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editingIntention = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intenções Financeiras", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Qual sua intenção?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Valor alvo (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Text("R$", modifier = Modifier.padding(start=16.dp)) }
                )
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            intentionsManager.addIntention(text, targetAmount.toDoubleOrNull())
                            GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                            text = ""
                            targetAmount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Adicionar Intenção")
                }
            }

            Divider()

            val currents = intentions.filter { it.status == "CURRENT" }
            val futures = intentions.filter { it.status == "FUTURE" }
            val completeds = intentions.filter { it.status == "COMPLETED" }

            // Auto-complete: if current intention reached its target
            LaunchedEffect(currents, history) {
                currents.forEach { item ->
                    if (item.targetAmount != null && item.targetAmount > 0) {
                        val since = item.currentSince ?: 0L
                        val saved = item.carryOver + history.filter { it.wasBlocked && it.timestamp >= since }.sumOf { it.price }
                        if (saved >= item.targetAmount) {
                            intentionsManager.updateStatus(item.id, "COMPLETED", history)
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                        }
                    }
                }
            }

            if (currents.isNotEmpty()) {
                IntentionSection("Intenção Atual", currents, MaterialTheme.colorScheme.secondary, history,
                    onStatusChange = null,
                    onDelete = { id -> 
                        intentionsManager.deleteIntention(id)
                        GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                    },
                    onEdit = { editingIntention = it },
                    actionIcon = null,
                    actionText = ""
                )
            }

            if (futures.isNotEmpty()) {
                IntentionSection("Intenções Futuras", futures, MaterialTheme.colorScheme.secondary, history,
                    onStatusChange = { id, _ -> 
                        intentionsManager.updateStatus(id, "CURRENT", history)
                        GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                    },
                    onDelete = { id -> 
                        intentionsManager.deleteIntention(id)
                        GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                    },
                    onEdit = { editingIntention = it },
                    actionIcon = Icons.Outlined.ArrowUpward,
                    actionText = "Tornar Atual"
                )
            }

            if (completeds.isNotEmpty()) {
                IntentionSection("Intenções Concluídas", completeds, MaterialTheme.colorScheme.secondary, history,
                    onStatusChange = null,
                    onDelete = { id -> 
                        intentionsManager.deleteIntention(id)
                        GlobalScope.launch(Dispatchers.IO) { dataSyncManager.syncUpload() }
                    },
                    onEdit = { editingIntention = it },
                    actionIcon = null,
                    actionText = ""
                )
            }

            if (intentions.isEmpty()) {
                Text(
                    text = "Nenhuma intenção cadastrada ainda. Use o Guardião a seu favor para lembrar dos seus objetivos maiores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun IntentionSection(
    title: String,
    items: List<FinancialIntention>,
    color: androidx.compose.ui.graphics.Color,
    history: List<PurchaseEntity>,
    onStatusChange: ((String, String) -> Unit)?,
    onDelete: (String) -> Unit,
    onEdit: (FinancialIntention) -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    actionText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        
        items.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(item.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    
                    if (item.targetAmount != null && item.targetAmount > 0) {
                        val rawSaved = if (item.status == "FUTURE") {
                            0.0
                        } else if (item.status == "COMPLETED") {
                            // Cap at target for completed intentions
                            val since = item.currentSince ?: 0L
                            val end = item.completedAt ?: Long.MAX_VALUE
                            val total = item.carryOver + history.filter { it.wasBlocked && it.timestamp in since..end }.sumOf { it.price }
                            minOf(total, item.targetAmount)
                        } else {
                            // CURRENT: show real accumulated value
                            val since = item.currentSince ?: 0L
                            item.carryOver + history.filter { it.wasBlocked && it.timestamp >= since }.sumOf { it.price }
                        }
                        val itemSaved = rawSaved
                        val progress = (itemSaved / item.targetAmount).toFloat().coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Poupado: R$${String.format("%.2f", itemSaved)}", style = MaterialTheme.typography.bodySmall)
                                Text("Meta: R$${String.format("%.2f", item.targetAmount)}", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = color
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Apagar", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onEdit(item) }) {
                            Icon(Icons.Outlined.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (onStatusChange != null && actionIcon != null) {
                            TextButton(onClick = { onStatusChange(item.id, "") }) {
                                Icon(actionIcon, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(actionText)
                            }
                        }
                    }
                }
            }
        }
    }
}
