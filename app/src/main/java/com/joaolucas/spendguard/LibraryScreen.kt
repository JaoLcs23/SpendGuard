@file:OptIn(ExperimentalMaterial3Api::class)

package com.joaolucas.spendguard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private fun typeAccentColor(type: String): Color = when (type.uppercase()) {
    "LIVRO"  -> Color(0xFF7F77DD)
    "VIDEO"  -> Color(0xFFE24B4A)
    "SITE"   -> Color(0xFF1D9E75)
    "ARTIGO" -> Color(0xFF378ADD)
    "CURSO"  -> Color(0xFFD85A30)
    else     -> Color(0xFFB0A070)
}

private fun typeAccentColorLocal(type: ResourceType): Color = when (type) {
    ResourceType.LIVRO  -> Color(0xFF7F77DD)
    ResourceType.VIDEO  -> Color(0xFFE24B4A)
    ResourceType.SITE   -> Color(0xFF1D9E75)
    ResourceType.ARTIGO -> Color(0xFF378ADD)
    ResourceType.CURSO  -> Color(0xFFD85A30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    educationRepository: EducationRepository,
    userRepository: UserRepository,
    proManager: ProManager,
    billingManager: BillingManager
) {
    val gold = MaterialTheme.colorScheme.primary

    var savedResources by remember { mutableStateOf<List<EducationalResourceRemote>>(emptyList()) }
    var isLoadingSaved by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val userId = userRepository.getCurrentUserId() ?: ""
            savedResources = educationRepository.getUserLibrary(userId)
        } catch (_: Exception) { } finally {
            isLoadingSaved = false
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    var showPaywall by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.primaryContainer,
            contentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color    = gold
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text = {
                    Text(
                        "Explorar",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = { Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Minha Biblioteca",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedTab) {
            0 -> ExploreTab(
                proManager          = proManager,
                educationRepository = educationRepository,
                userRepository      = userRepository,
                onResourceSaved     = { newResource -> savedResources = savedResources + newResource },
                onShowPaywall       = { showPaywall = true }
            )
            1 -> MyLibraryTab(
                resources           = savedResources,
                isLoading           = isLoadingSaved,
                educationRepository = educationRepository,
                onResourceDeleted   = { id -> savedResources = savedResources.filter { it.id != id } },
                onReadToggled       = { id, isRead ->
                    savedResources = savedResources.map { if (it.id == id) it.copy(isRead = isRead) else it }
                }
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreTab(
    proManager: ProManager,
    educationRepository: EducationRepository,
    userRepository: UserRepository,
    onResourceSaved: (EducationalResourceRemote) -> Unit,
    onShowPaywall: () -> Unit
) {
    val scope        = rememberCoroutineScope()
    val gold         = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current

    val isPro        by proManager.isPro.collectAsState()
    val savesLeft    = proManager.librarySavesLeft()

    var searchQuery        by remember { mutableStateOf("") }
    var selectedType       by remember { mutableStateOf<ResourceType?>(null) }
    var savingId           by remember { mutableStateOf<String?>(null) }
    var saveMessage        by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val allResources = EducationLibrary.resources
    val filtered = remember(searchQuery, selectedType) {
        allResources.filter { resource ->
            val matchesType  = selectedType == null || resource.type == selectedType
            val matchesQuery = searchQuery.isBlank() ||
                    resource.title.contains(searchQuery, ignoreCase = true) ||
                    resource.author.contains(searchQuery, ignoreCase = true) ||
                    resource.description.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {


        OutlinedTextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder   = { Text("Buscar título, autor ou tema…", fontSize = 14.sp) },
            leadingIcon   = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = gold)
            },
            trailingIcon  = if (searchQuery.isNotEmpty()) ({
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }) else null,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape         = RoundedCornerShape(14.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = gold,
                focusedLabelColor    = gold,
                cursorColor          = gold,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        )

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick  = { selectedType = null },
                    label    = { Text("Todos (${allResources.size})", fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = gold,
                        selectedLabelColor     = Color(0xFF121212)
                    )
                )
            }
            items(ResourceType.entries.toTypedArray()) { type ->
                val count = allResources.count { it.type == type }
                FilterChip(
                    selected = selectedType == type,
                    onClick  = { selectedType = if (selectedType == type) null else type },
                    label    = { Text("${type.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)", fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(typeAccentColorLocal(type))
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = typeAccentColorLocal(type).copy(alpha = 0.2f),
                        selectedLabelColor     = typeAccentColorLocal(type)
                    ),
                    border = FilterChipDefaults.filterChipBorder()
                )
            }
        }

        Divider(
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            "${filtered.size} resultado${if (filtered.size != 1) "s" else ""}",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        saveMessage?.let { (msg, isSuccess) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSuccess) Color(0xFF1D9E75).copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier          = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                        contentDescription = null,
                        tint     = if (isSuccess) Color(0xFF1D9E75) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSuccess) Color(0xFF1D9E75) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.SearchOff, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Nenhum resultado para \"$searchQuery\"",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.title }) { resource ->
                    CatalogResourceCard(
                        resource  = resource,
                        isSaving  = savingId == resource.title,
                        canSave   = true,
                        onSave    = {
                            savingId = resource.title
                            scope.launch {
                                try {
                                    val userId = userRepository.getCurrentUserId() ?: ""
                                    val remote = EducationalResourceRemote(
                                        userId      = userId,
                                        title       = resource.title,
                                        author      = resource.author,
                                        type        = resource.type.name,
                                        description = resource.description,
                                        link        = resource.link
                                    )
                                    val saved = educationRepository.saveToLibrary(remote)
                                    if (saved) {
                                        onResourceSaved(remote)
                                        saveMessage = "\"${resource.title}\" salvo na sua biblioteca!" to true
                                    } else {
                                        saveMessage = "Você já salvou este recurso." to false
                                    }
                                } catch (_: Exception) {
                                    saveMessage = "Erro ao salvar. Verifique sua conexão." to false
                                } finally {
                                    savingId = null
                                }
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MyLibraryTab(
    resources: List<EducationalResourceRemote>,
    isLoading: Boolean,
    educationRepository: EducationRepository,
    onResourceDeleted: (String) -> Unit,
    onReadToggled: (String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val gold  = MaterialTheme.colorScheme.primary

    var selectedFilter by remember { mutableIntStateOf(0) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = gold, strokeWidth = 2.dp)
                Text(
                    "Carregando sua biblioteca…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        return
    }

    val filteredResources = when (selectedFilter) {
        1    -> resources.filter { it.isRead }
        2    -> resources.filter { !it.isRead }
        else -> resources
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Todas",     Icons.Outlined.LibraryBooks,         resources.size),
                Triple("Lidas",     Icons.Outlined.CheckCircle,          resources.count { it.isRead }),
                Triple("Não lidas", Icons.Outlined.RadioButtonUnchecked, resources.count { !it.isRead })
            ).forEachIndexed { index, (label, _, count) ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick  = { selectedFilter = index },
                    label    = {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(label, fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedFilter == index)
                                    Color(0xFF121212).copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "$count",
                                    modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (selectedFilter == index) Color(0xFF121212)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = gold,
                        selectedLabelColor     = Color(0xFF121212)
                    ),
                    border = FilterChipDefaults.filterChipBorder()
                )
            }
        }

        Divider(
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (resources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "Sua biblioteca está vazia",
                        style      = MaterialTheme.typography.titleSmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Explore o catálogo e salve conteúdos que quiser estudar",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredResources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhum item nesta categoria",
                    style  = MaterialTheme.typography.bodyMedium,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(filteredResources, key = { it.id ?: it.title }) { resource ->
                    LibraryResourceCard(
                        resource     = resource,
                        onDelete     = {
                            scope.launch {
                                try {
                                    educationRepository.deleteFromLibrary(resource.id!!)
                                    onResourceDeleted(resource.id)
                                } catch (_: Exception) {}
                            }
                        },
                        onToggleRead = { newStatus ->
                            scope.launch {
                                try {
                                    educationRepository.updateReadStatus(resource.id!!, newStatus)
                                    onReadToggled(resource.id, newStatus)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogResourceCard(
    resource: EducationalResource,
    isSaving: Boolean,
    canSave: Boolean,
    onSave: () -> Unit
) {
    val context   = LocalContext.current
    val gold      = MaterialTheme.colorScheme.primary
    val typeColor = typeAccentColorLocal(resource.type)
    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(typeColor, typeColor.copy(alpha = 0.3f)))
                    )
            )

            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            resource.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines   = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow   = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Text(
                            "por ${resource.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            resource.type.name,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            color      = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp
                        )
                    }
                }

                if (resource.description.isNotEmpty()) {
                    Text(
                        resource.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick        = onSave,
                                enabled        = !isSaving,
                                modifier       = Modifier.weight(1f),
                                shape          = RoundedCornerShape(10.dp),
                                colors         = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (canSave) gold
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                ),
                                border         = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            gold.copy(alpha = if (canSave) 0.5f else 0.2f),
                                            gold.copy(alpha = if (canSave) 0.5f else 0.2f)
                                        )
                                    )
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color       = gold
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.BookmarkAdd, null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("Salvar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (resource.link.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.link)))
                                        } catch (_: Exception) {}
                                    },
                                    modifier       = Modifier.weight(1f),
                                    shape          = RoundedCornerShape(10.dp),
                                    colors         = ButtonDefaults.buttonColors(
                                        containerColor = gold,
                                        contentColor   = Color(0xFF121212)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Acessar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryResourceCard(
    resource: EducationalResourceRemote,
    onDelete: () -> Unit,
    onToggleRead: (Boolean) -> Unit
) {
    val context   = LocalContext.current
    val gold      = MaterialTheme.colorScheme.primary
    var isExpanded       by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val typeColor   = typeAccentColor(resource.type)
    val readColor   = Color(0xFF81C784)
    val accentColor = if (resource.isRead) readColor else gold

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remover da biblioteca?", fontWeight = FontWeight.Bold) },
            text  = { Text("\"${resource.title}\" será removido permanentemente.", style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remover", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    ElevatedCard(
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(typeColor, typeColor.copy(alpha = 0.3f)))
                    )
            )

            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = if (resource.isRead) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint     = accentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        resource.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f),
                        maxLines   = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick  = { showDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (resource.description.isNotEmpty()) {
                    Text(
                        resource.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Person, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        resource.author,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            resource.type,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            color      = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick        = { onToggleRead(!resource.isRead) },
                                modifier       = Modifier.weight(1f),
                                shape          = RoundedCornerShape(10.dp),
                                colors         = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                                border         = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.5f),
                                            accentColor.copy(alpha = 0.5f)
                                        )
                                    )
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (resource.isRead) Icons.Outlined.RemoveDone else Icons.Outlined.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (resource.isRead) "Não lido" else "Concluir",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (resource.link.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.link)))
                                        } catch (_: Exception) {}
                                    },
                                    modifier       = Modifier.weight(1f),
                                    shape          = RoundedCornerShape(10.dp),
                                    colors         = ButtonDefaults.buttonColors(
                                        containerColor = gold,
                                        contentColor   = Color(0xFF121212)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Acessar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.tabIndicatorOffset(tabPosition: TabPosition): Modifier =
    this.fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = tabPosition.left)
        .width(tabPosition.width)