package com.joaolucas.spendguard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    database: SpendGuardDatabase,
    userRepository: UserRepository,
    isPro: Boolean,
    onBack: () -> Unit,
    onShowPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope   = LocalContext.current.let { rememberCoroutineScope() }
    val gold    = MaterialTheme.colorScheme.primary

    var parseResult         by remember { mutableStateOf<CsvParser.ParseResult?>(null) }
    var pendingTransactions by remember { mutableStateOf<List<CsvParser.ParsedTransaction>>(emptyList()) }
    var isLoading           by remember { mutableStateOf(false) }
    var errorMsg            by remember { mutableStateOf<String?>(null) }
    var importDone          by remember { mutableStateOf(false) }
    var importedCount       by remember { mutableStateOf(0) }
    var showErrors          by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading  = true
        errorMsg   = null
        parseResult = null
        pendingTransactions = emptyList()
        importDone  = false

        scope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: throw IllegalStateException("Não foi possível ler o arquivo")
                }
                val result = withContext(Dispatchers.Default) { CsvParser.parse(content) }
                parseResult = result
                pendingTransactions = result.transactions

                if (result.transactions.isEmpty()) {
                    errorMsg = "Nenhuma transação de débito encontrada no arquivo."
                }
            } catch (e: Exception) {
                errorMsg = "Erro ao ler arquivo: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar CSV") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (!isPro) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Lock, null, tint = gold, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Importar CSV é Pro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = gold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Importe seu extrato bancário e veja todos os seus gastos organizados no SpendGuard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BanksInfoCard(gold)

                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                ) {
                    Icon(Icons.Outlined.FileUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Selecionar arquivo CSV", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = gold)
                    }
                }

                errorMsg?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0A0A))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(msg, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (importDone) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2A0A))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF81C784),
                                modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "$importedCount transações importadas!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                                Text(
                                    "Visíveis no Histórico com tag 'Importado'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF81C784).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                parseResult?.let { result ->
                    if (pendingTransactions.isNotEmpty()) {
                        PreviewSection(
                            result              = result,
                            pendingTransactions = pendingTransactions,
                            gold                = gold,
                            showErrors          = showErrors,
                            onToggleErrors      = { showErrors = !showErrors },
                            onDeleteTransaction = { transactionToRemove ->
                                pendingTransactions = pendingTransactions.filter { it != transactionToRemove }
                            }
                        )
                    } else if (!importDone) {
                        Text(
                            "Todas as transações foram removidas da lista de importação.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }
            }

            if (pendingTransactions.isNotEmpty() && !importDone) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val userId = withContext(Dispatchers.IO) {
                                    userRepository.getCurrentUserId() ?: ""
                                }
                                val entities = pendingTransactions.map { t ->
                                    val inferredCategory = OfflineAnalyzer.inferCategory(t.itemName, "Importado").name
                                    PurchaseEntity(
                                        userId        = userId,
                                        itemName      = t.itemName,
                                        price         = t.price,
                                        justification = "Importado via CSV",
                                        wasBlocked    = false,
                                        aiMessage     = "Transação importada do extrato bancário",
                                        coolingOffTime = 0,
                                        timestamp     = t.timestamp,
                                        isImported    = true,
                                        category      = inferredCategory
                                    )
                                }
                                withContext(Dispatchers.IO) {
                                    database.purchaseDao().insertAll(entities)
                                }
                                importedCount = entities.size
                                importDone    = true
                                pendingTransactions = emptyList()
                                parseResult   = null
                                snackbarHostState.showSnackbar(
                                    "$importedCount transações importadas com sucesso"
                                )
                            } catch (e: Exception) {
                                errorMsg = "Erro ao salvar: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                ) {
                    Icon(Icons.Outlined.SaveAlt, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Importar ${pendingTransactions.size} transações",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BanksInfoCard(gold: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalance, null, tint = gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Bancos suportados",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }
            Text(
                "Nubank · Inter · Bradesco · Itaú · C6 Bank · CSV genérico",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                "Exporte o extrato em CSV pelo app do seu banco e selecione o arquivo aqui. " +
                        "Apenas débitos são importados — créditos e estornos são ignorados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PreviewSection(
    result: CsvParser.ParseResult,
    pendingTransactions: List<CsvParser.ParsedTransaction>,
    gold: Color,
    showErrors: Boolean,
    onToggleErrors: () -> Unit,
    onDeleteTransaction: (CsvParser.ParsedTransaction) -> Unit
) {
    val currFmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val dateFmt = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
    val totalValue = pendingTransactions.sumOf { it.price }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A00))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Formato detectado: ${result.format}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Text(
                            "${result.totalLines - 1} linhas lidas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = gold.copy(alpha = 0.12f)) {
                        Text(
                            "${pendingTransactions.size} trans.",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = gold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatChip("Total gasto", currFmt.format(totalValue), gold)

                    val userDeleted = result.transactions.size - pendingTransactions.size
                    val totalIgnored = result.skippedLines + userDeleted

                    if (totalIgnored > 0)
                        StatChip("Ignoradas", "$totalIgnored", Color.White.copy(alpha = 0.35f))

                    if (result.errors.isNotEmpty())
                        StatChip("Erros", "${result.errors.size}", MaterialTheme.colorScheme.error)
                }

                if (result.errors.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onToggleErrors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (showErrors) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showErrors) "Ocultar erros" else "Ver ${result.errors.size} aviso(s)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (showErrors) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            result.errors.take(10).forEach { err ->
                                Text(
                                    "• $err",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )
                            }
                            if (result.errors.size > 10)
                                Text(
                                    "...e mais ${result.errors.size - 10} avisos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                        }
                    }
                }
            }
        }

        Text(
            "Pré-visualização (Remova o que não quiser)",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f)
        )

        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(pendingTransactions) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                t.itemName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                dateFmt.format(java.util.Date(t.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            currFmt.format(t.price),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onDeleteTransaction(t) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Remover",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (pendingTransactions.last() != t)
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.55f))
    }
}