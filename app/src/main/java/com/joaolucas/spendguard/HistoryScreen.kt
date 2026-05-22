package com.joaolucas.spendguard

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private const val JIT_DOMINANT_THRESHOLD = 0.40f
private const val JIT_MIN_TOTAL = 30.0

enum class PeriodFilter(val label: String) {
    ALL("Todo o período"),
    TODAY("Hoje"),
    WEEK("Esta semana"),
    MONTH("Este mês"),
    CUSTOM("Personalizado")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    database: SpendGuardDatabase,
    userRepository: UserRepository,
    educationRepository: EducationRepository,
    proManager: ProManager,
    onOpenImport: () -> Unit,
    onShowPaywall: () -> Unit
) {
    val isPro by proManager.isPro.collectAsState()
    val currentUserId = userRepository.getCurrentUserId() ?: ""
    val purchases by database.purchaseDao()
        .getPurchasesByUser(currentUserId)
        .collectAsState(initial = emptyList())

    val scope   = rememberCoroutineScope()
    val context = LocalContext.current
    val gold    = MaterialTheme.colorScheme.primary

    var selectedFilter        by remember { mutableStateOf(0) }
    var selectedPeriod        by remember { mutableStateOf(PeriodFilter.ALL) }
    var customStart           by remember { mutableStateOf<Long?>(null) }
    var customEnd             by remember { mutableStateOf<Long?>(null) }
    var showPeriodMenu        by remember { mutableStateOf(false) }
    var showClearDialog       by remember { mutableStateOf(false) }
    var showShareDialog       by remember { mutableStateOf(false) }
    var showCategoryBreakdown by remember { mutableStateOf(false) }
    var exportLoading         by remember { mutableStateOf(false) }
    var editingPurchase       by remember { mutableStateOf<PurchaseEntity?>(null) }
    var expandedCardId        by remember { mutableStateOf(-1) }

    val recommendations       = remember(purchases) { ContentRecommender.recommend(purchases, EducationLibrary.resources) }
    var selectedRecommendation by remember { mutableStateOf<EducationalResource?>(null) }
    var savingRecommendation   by remember { mutableStateOf(false) }
    var saveMessage            by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val prefs      = remember { context.getSharedPreferences("spendguard_prefs", Context.MODE_PRIVATE) }

    val periodFiltered = remember(purchases, selectedPeriod, customStart, customEnd) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            PeriodFilter.ALL    -> purchases
            PeriodFilter.TODAY  -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                purchases.filter { it.timestamp >= cal.timeInMillis }
            }
            PeriodFilter.WEEK   -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                purchases.filter { it.timestamp >= cal.timeInMillis }
            }
            PeriodFilter.MONTH  -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                purchases.filter { it.timestamp >= cal.timeInMillis }
            }
            PeriodFilter.CUSTOM -> {
                val s = customStart ?: 0L
                val e = (customEnd ?: now) + 86_400_000L
                purchases.filter { it.timestamp in s..e }
            }
        }
    }

    val filteredPurchases = when (selectedFilter) {
        1    -> periodFiltered.filter { !it.wasBlocked }
        2    -> periodFiltered.filter { it.wasBlocked }
        else -> periodFiltered
    }

    val totalBlocked  = purchases.count { it.wasBlocked }
    val totalApproved = purchases.count { !it.wasBlocked }
    val savedAmount   = purchases.filter { it.wasBlocked }.sumOf { it.price }

    val categoryBreakdown: Map<SpendingCategory, Double> = remember(purchases) {
        purchases
            .filter { !it.wasBlocked }
            .groupBy { SpendingCategory.fromString(it.category) }
            .mapValues { (_, list) -> list.sumOf { it.price } }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }



    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon  = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar histórico?", fontWeight = FontWeight.Bold) },
            text  = { Text("Todos os registros serão apagados permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { database.purchaseDao().deleteByUser(currentUserId) }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showShareDialog) {
        val shareText = buildShareText(purchases.size, totalBlocked, savedAmount, categoryBreakdown)
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            icon  = { Icon(Icons.Outlined.Share, null, tint = gold) },
            title = { Text("Compartilhar resultados", fontWeight = FontWeight.Bold) },
            text  = {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        shareText,
                        modifier = Modifier.padding(16.dp),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = gold,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
                        showShareDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                ) { Text("Compartilhar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Fechar") }
            }
        )
    }



    editingPurchase?.let { purchaseToEdit ->
        EditPurchaseDialog(
            purchase = purchaseToEdit,
            onDismiss = { editingPurchase = null },
            onSave    = { newName, newCategory ->
                scope.launch {
                    database.purchaseDao().update(purchaseToEdit.copy(itemName = newName, category = newCategory.name))
                    editingPurchase = null
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(Modifier.weight(1f), "Analisadas",  "${purchases.size}", Icons.Outlined.Analytics,  gold)
                SummaryCard(Modifier.weight(1f), "Aprovadas",   "$totalApproved",    Icons.Outlined.CheckCircle, Color(0xFF81C784))
                SummaryCard(Modifier.weight(1f), "Bloqueadas",  "$totalBlocked",     Icons.Outlined.Block,       MaterialTheme.colorScheme.error)
            }
        }

        

        if (categoryBreakdown.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PieChart, null, tint = gold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Gastos por categoria",
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = gold
                                )
                            }
                            TextButton(
                                onClick        = { showCategoryBreakdown = !showCategoryBreakdown },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    if (showCategoryBreakdown) "Ocultar" else "Mostrar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gold.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (showCategoryBreakdown) {
                            Spacer(Modifier.height(8.dp))
                            val totalApprovedAmount = categoryBreakdown.values.sum()
                            categoryBreakdown.entries.forEach { (category, amount) ->
                                val fraction = if (totalApprovedAmount > 0) (amount / totalApprovedAmount).toFloat() else 0f
                                val pctStr   = "%.1f".format(fraction * 100)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape    = RoundedCornerShape(4.dp),
                                                color    = Color(category.color).copy(alpha = 0.2f),
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                category.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "R$ ${"%.2f".format(amount)} · $pctStr%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress   = fraction,
                                        modifier   = Modifier.fillMaxWidth().height(4.dp),
                                        color      = Color(category.color),
                                        trackColor = Color(category.color).copy(alpha = 0.12f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome, null,
                            tint     = gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Aprendizado no momento certo",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding        = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(recommendations, key = { it.title + "_rec" }) { resource ->
                            RecommendationCard(
                                resource = resource,
                                onClick  = { selectedRecommendation = resource }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        listOf("Todas", "Aprovadas", "Bloqueadas").forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedFilter == index,
                                onClick  = { selectedFilter = index },
                                label    = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.padding(end = 6.dp),
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.inversePrimary,
                                    selectedLabelColor     = Color(0xFF121212)
                                )
                            )
                        }
                    }
                    Box {
                        Surface(
                            onClick  = { showPeriodMenu = true },
                            shape    = RoundedCornerShape(50.dp),
                            color    = if (selectedPeriod != PeriodFilter.ALL)
                                           MaterialTheme.colorScheme.inversePrimary
                                       else MaterialTheme.colorScheme.surface,
                            border   = androidx.compose.foundation.BorderStroke(
                                width = if (selectedPeriod != PeriodFilter.ALL) 0.dp else 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.height(36.dp).widthIn(min = 130.dp)
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = if (selectedPeriod != PeriodFilter.ALL)
                                                   Color(0xFF121212)
                                               else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text       = when (selectedPeriod) {
                                        PeriodFilter.ALL -> "Período"
                                        PeriodFilter.CUSTOM -> {
                                            if (customStart != null && customEnd != null) {
                                                val sdf = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                                                "De: ${sdf.format(java.util.Date(customStart!!))}, Até: ${sdf.format(java.util.Date(customEnd!!))}"
                                            } else "Personalizado"
                                        }
                                        else -> selectedPeriod.label
                                    },
                                    fontSize   = 12.sp,
                                    maxLines   = 1,
                                    softWrap   = false,
                                    fontWeight = if (selectedPeriod != PeriodFilter.ALL) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (selectedPeriod != PeriodFilter.ALL)
                                                     Color(0xFF121212)
                                                 else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        DropdownMenu(
                            expanded         = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            PeriodFilter.values().forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.label, fontSize = 14.sp) },
                                    onClick = {
                                        if (period == PeriodFilter.CUSTOM) {
                                            showPeriodMenu = false
                                            val calNow = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val start = Calendar.getInstance()
                                                    start.set(y, m, d, 0, 0, 0)
                                                    customStart = start.timeInMillis
                                                    val endPicker = DatePickerDialog(
                                                        context,
                                                        { _, y2, m2, d2 ->
                                                            val end = Calendar.getInstance()
                                                            end.set(y2, m2, d2, 23, 59, 59)
                                                            customEnd = end.timeInMillis
                                                            selectedPeriod = PeriodFilter.CUSTOM
                                                        },
                                                        calNow.get(Calendar.YEAR),
                                                        calNow.get(Calendar.MONTH),
                                                        calNow.get(Calendar.DAY_OF_MONTH)
                                                    )
                                                    endPicker.setTitle("Até:")
                                                    endPicker.show()
                                                },
                                                calNow.get(Calendar.YEAR),
                                                calNow.get(Calendar.MONTH),
                                                calNow.get(Calendar.DAY_OF_MONTH)
                                            ).also { it.setTitle("De:") }.show()
                                        } else {
                                            selectedPeriod = period
                                            customStart = null
                                            customEnd   = null
                                            showPeriodMenu = false
                                        }
                                    },
                                    leadingIcon = {
                                        if (selectedPeriod == period) {
                                            Icon(Icons.Outlined.Check, null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedPeriod == PeriodFilter.CUSTOM && customStart != null) {
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")) }
                    val rangeText = if (customEnd != null)
                        "${sdf.format(Date(customStart!!))} → ${sdf.format(Date(customEnd!!))}"
                    else
                        "A partir de ${sdf.format(Date(customStart!!))}"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.DateRange, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(rangeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    selectedPeriod = PeriodFilter.ALL
                                    customStart = null; customEnd = null
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("Limpar", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ActionButton(
                        icon    = Icons.Outlined.FileDownload,
                        label   = "Exportar",
                        tint    = if (isPro) MaterialTheme.colorScheme.onSurfaceVariant
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        onClick = {
                            if (!isPro) { onShowPaywall(); return@ActionButton }
                            exportLoading = true
                            scope.launch {
                                exportToCsv(context, purchases)
                                exportLoading = false
                            }
                        },
                        loading = exportLoading
                    )
                    ActionButton(
                        icon    = Icons.Outlined.FileUpload,
                        label   = "Importar",
                        tint    = if (isPro) MaterialTheme.colorScheme.onSurfaceVariant
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        onClick = { if (!isPro) onShowPaywall() else onOpenImport() }
                    )

                    ActionButton(
                        icon    = Icons.Outlined.Share,
                        label   = "Compartilhar",
                        tint    = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showShareDialog = true }
                    )
                    ActionButton(
                        icon    = Icons.Outlined.DeleteSweep,
                        label   = "Limpar",
                        tint    = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        onClick = { showClearDialog = true }
                    )
                }
            }
        }

        if (filteredPurchases.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Inbox, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp))
                        Text(
                            "Nenhuma análise ainda",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Use o Guardião para analisar sua próxima compra",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        } else {
            items(filteredPurchases, key = { it.id }) { purchase ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PurchaseHistoryCard(
                        purchase    = purchase,
                        isExpanded  = expandedCardId == purchase.id,
                        onToggle    = {
                            expandedCardId = if (expandedCardId == purchase.id) -1 else purchase.id
                        },
                        onDelete    = { scope.launch { database.purchaseDao().delete(purchase) } },
                        onEdit      = { editingPurchase = purchase }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedRecommendation?.let { resource ->
        ModalBottomSheet(
            onDismissRequest = { selectedRecommendation = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ResourceDetailSheet(
                resource  = resource,
                isSaving  = savingRecommendation,
                onSave    = {
                    savingRecommendation = true
                    scope.launch {
                        try {
                            val remote = EducationalResourceRemote(
                                userId      = currentUserId,
                                title       = resource.title,
                                author      = resource.author,
                                type        = resource.type.name,
                                description = resource.description,
                                link        = resource.link
                            )
                            val saved = educationRepository.saveToLibrary(remote)
                            if (saved) {
                                saveMessage = "\"${resource.title}\" salvo na sua biblioteca!" to true
                            } else {
                                saveMessage = "Você já salvou este recurso." to false
                            }
                        } catch (_: Exception) {
                            saveMessage = "Erro ao salvar. Verifique sua conexão." to false
                        } finally {
                            savingRecommendation = false
                            selectedRecommendation = null
                        }
                    }
                },
                onDismiss = { selectedRecommendation = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseDialog(
    purchase: PurchaseEntity,
    onDismiss: () -> Unit,
    onSave: (String, SpendingCategory) -> Unit
) {
    var name     by remember { mutableStateOf(purchase.itemName) }
    var category by remember { mutableStateOf(SpendingCategory.fromString(purchase.category)) }
    var expanded by remember { mutableStateOf(false) }
    val gold     = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Transação", fontWeight = FontWeight.Bold, color = gold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Nome do estabelecimento ou item") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor  = gold,
                        cursorColor        = gold
                    )
                )

                ExposedDropdownMenuBox(
                    expanded        = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value         = category.label,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Categoria") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gold,
                            focusedLabelColor  = gold
                        )
                    )
                    ExposedDropdownMenu(
                        expanded        = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SpendingCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat.label) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, category) },
                colors  = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
            ) { Text("Salvar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun buildShareText(
    total: Int,
    blocked: Int,
    savedAmount: Double,
    categoryBreakdown: Map<SpendingCategory, Double>
): String {
    val approved = total - blocked
    val sdf   = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
    val month = sdf.format(Date()).replaceFirstChar { it.uppercase() }
    val sb    = StringBuilder()
    sb.appendLine("SpendGuard — Relatório de Consciência Financeira")
    sb.appendLine(month)
    sb.appendLine()
    sb.appendLine("Compras analisadas: $total")
    sb.appendLine("Aprovadas conscientemente: $approved")
    sb.appendLine("Bloqueadas pelo Guardião: $blocked")
    if (savedAmount > 0) sb.appendLine("Valor protegido: R$ ${"%.2f".format(savedAmount)}")
    if (categoryBreakdown.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("Onde mais gastei:")
        categoryBreakdown.entries.take(5).forEach { (cat, amt) ->
            sb.appendLine("  ${cat.label}: R$ ${"%.2f".format(amt)}")
        }
    }
    sb.appendLine()
    sb.appendLine("Cada compra analisada é um passo em direção à liberdade financeira.")
    sb.appendLine()
    sb.append("SpendGuard — Guarde seu dinheiro com inteligência.")
    return sb.toString()
}

private suspend fun exportToCsv(context: Context, purchases: List<PurchaseEntity>) {
    withContext(Dispatchers.IO) {
        try {
            val sdf     = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val sdfFile = SimpleDateFormat("yyyyMMdd_HHmm", Locale("pt", "BR"))
            val now     = Date()

            val totalBlocked  = purchases.count { it.wasBlocked }
            val totalApproved = purchases.count { !it.wasBlocked }
            val totalSaved    = purchases.filter { it.wasBlocked }.sumOf { it.price }

            val categoryTotals = purchases
                .filter { !it.wasBlocked }
                .groupBy { SpendingCategory.fromString(it.category) }
                .mapValues { (_, list) -> list.sumOf { it.price } }
                .entries
                .sortedByDescending { it.value }

            val sb = StringBuilder()
            sb.append("\uFEFF")
            sb.append("SpendGuard - Relatório de Consciência Financeira\n")
            sb.append("Gerado em:;${sdf.format(now)}\n")
            sb.append("Total de registros:;${purchases.size}\n\n")
            sb.append("RESUMO\n")
            sb.append("Compras analisadas;${purchases.size}\n")
            sb.append("Aprovadas;$totalApproved\n")
            sb.append("Bloqueadas;$totalBlocked\n")
            sb.append("Valor protegido (R$);${"%.2f".format(totalSaved)}\n\n")

            if (categoryTotals.isNotEmpty()) {
                sb.append("GASTOS POR CATEGORIA (aprovadas)\n")
                categoryTotals.forEach { (cat, amt) -> sb.append("${cat.label};${"%.2f".format(amt)}\n") }
                sb.append("\n")
            }

            sb.append("REGISTROS DETALHADOS\n")
            sb.append("Data e Hora;Categoria;Item;Valor (R$);Status;Origem;Justificativa;Tempo de Geladeira (min);Mensagem do Guardião\n")
            purchases.forEach { p ->
                val date     = sdf.format(Date(p.timestamp))
                val category = SpendingCategory.fromString(p.category).label
                val safeItem = p.itemName.replace(";", ",").replace("\n", " ").trim()
                val safeJust = p.justification.replace(";", ",").replace("\n", " ").trim()
                val safeMsg  = p.aiMessage.replace(";", ",").replace("\n", " ").trim()
                val decision = if (p.wasBlocked) "Bloqueada" else "Aprovada"
                val origem   = if (p.isImported) "Importado" else "Análise manual"
                val cooling  = if (p.coolingOffTime > 0) "${p.coolingOffTime}" else "-"
                sb.append("$date;$category;$safeItem;${"%.2f".format(p.price)};$decision;$origem;$safeJust;$cooling;$safeMsg\n")
            }

            val fileName = "Relatorio_SpendGuard_${sdfFile.format(now)}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório SpendGuard")
                putExtra(Intent.EXTRA_TEXT, "Aqui está o histórico de análises do SpendGuard gerado em ${sdf.format(now)}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar relatório"))
        } catch (_: Exception) { }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    loading: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier            = Modifier.width(64.dp)
    ) {
        IconButton(onClick = onClick, enabled = !loading, modifier = Modifier.size(40.dp)) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = tint)
            else Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = if (loading) 0.4f else 0.8f), maxLines = 1, fontSize = 9.sp)
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryCard(
    purchase:   PurchaseEntity,
    isExpanded: Boolean,
    onToggle:   () -> Unit,
    onDelete:   () -> Unit,
    onEdit:     () -> Unit
) {
    val gold        = MaterialTheme.colorScheme.primary
    val isBlocked   = purchase.wasBlocked
    val accentColor = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF81C784)
    val sdf         = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val category    = remember(purchase.category) { SpendingCategory.fromString(purchase.category) }

    ElevatedCard(
        shape   = RoundedCornerShape(12.dp),
        onClick = onToggle
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isBlocked) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                    null, tint = accentColor, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    purchase.itemName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    "R$ ${"%.2f".format(purchase.price)}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = gold
                )
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp).padding(start = 4.dp)
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Schedule, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp))
                Text(
                    sdf.format(Date(purchase.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Surface(shape = RoundedCornerShape(20.dp), color = Color(category.color).copy(alpha = 0.15f)) {
                    Text(
                        category.label,
                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = Color(category.color),
                        fontWeight = FontWeight.Medium,
                        fontSize   = 10.sp
                    )
                }
                if (isBlocked && purchase.coolingOffTime > 0) {
                    Surface(shape = RoundedCornerShape(20.dp), color = accentColor.copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, tint = accentColor, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${purchase.coolingOffTime}min", style = MaterialTheme.typography.labelSmall, color = accentColor)
                        }
                    }
                }
            }

            if (isExpanded) {
                Divider(
                    modifier  = Modifier.padding(vertical = 4.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                if (purchase.justification.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Justificativa",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            purchase.justification,
                            style      = MaterialTheme.typography.bodySmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (purchase.aiMessage.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Veredicto do Guardião",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor.copy(alpha = 0.8f)
                        )
                        Text(
                            purchase.aiMessage,
                            style      = MaterialTheme.typography.bodySmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (purchase.isImported) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.FileUpload, null, tint = gold.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        Text("Importado via CSV", style = MaterialTheme.typography.labelSmall, color = gold.copy(alpha = 0.5f))
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick        = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick        = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors         = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Excluir", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    resource: EducationalResource,
    onClick: () -> Unit
) {
    val typeColor = typeAccentColorLocal(resource.type)

    ElevatedCard(
        onClick   = onClick,
        shape     = RoundedCornerShape(14.dp),
        modifier  = Modifier
            .width(160.dp)
            .height(130.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(typeColor, typeColor.copy(alpha = 0.3f))
                        )
                    )
            )
            Column(
                modifier            = Modifier
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        resource.type.name,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = typeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 9.sp
                    )
                }
                Text(
                    resource.title,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 3,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    resource.author,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResourceDetailSheet(
    resource: EducationalResource,
    isSaving: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context   = LocalContext.current
    val gold      = MaterialTheme.colorScheme.primary
    val typeColor = typeAccentColorLocal(resource.type)

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                .align(Alignment.CenterHorizontally)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = typeColor.copy(alpha = 0.12f)
        ) {
            Text(
                resource.type.name,
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style      = MaterialTheme.typography.labelSmall,
                color      = typeColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            resource.title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp
        )
        Text(
            "por ${resource.author}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        if (resource.description.isNotEmpty()) {
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Text(
                resource.description,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                lineHeight = 21.sp
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick        = onSave,
                enabled        = !isSaving,
                modifier       = Modifier.weight(1f),
                shape          = RoundedCornerShape(10.dp),
                colors         = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                border         = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color       = gold
                    )
                } else {
                    Icon(Icons.Outlined.BookmarkAdd, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Salvar na Biblioteca", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (resource.link.isNotEmpty()) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.link)))
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier       = Modifier.weight(1f),
                    shape          = RoundedCornerShape(10.dp),
                    colors         = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor   = Color(0xFF121212)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Acessar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun typeAccentColorLocal(type: ResourceType): Color {
    return when (type) {
        ResourceType.LIVRO  -> Color(0xFF4CAF50)
        ResourceType.VIDEO  -> Color(0xFFF44336)
        ResourceType.ARTIGO -> Color(0xFF2196F3)
        ResourceType.CURSO  -> Color(0xFFFF9800)
        ResourceType.SITE   -> Color(0xFF9C27B0)
    }
}