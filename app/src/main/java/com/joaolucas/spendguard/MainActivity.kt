package com.joaolucas.spendguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.joaolucas.spendguard.ui.theme.SpendGuardTheme
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class AppScreen { SPLASH, ONBOARDING, AUTH, MAIN }

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val pendingDeepLink      = MutableStateFlow<Uri?>(null)
    private val pendingReevaluation  = MutableStateFlow<String?>(null)
    private val pendingWidgetItem    = MutableStateFlow<String?>(null)
    private val pendingWidgetPrice   = MutableStateFlow<Double>(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            android.util.Log.e("SpendGuard", "Uncaught exception", throwable)
        }


        intent?.data?.let { pendingDeepLink.value = it }
        intent?.getStringExtra("reavaliar_item")?.let { pendingReevaluation.value = it }
        intent?.getStringExtra("auto_item_name")?.takeIf { it.isNotEmpty() }?.let {
            pendingWidgetItem.value  = it
            pendingWidgetPrice.value = intent?.getDoubleExtra("auto_item_price", 0.0) ?: 0.0
        }

        enableEdgeToEdge()
        setContent {
            val context      = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val appTheme     by themeManager.theme.collectAsState()
            val forceDark: Boolean? = when (appTheme) {
                AppTheme.DARK   -> true
                AppTheme.LIGHT  -> false
                AppTheme.SYSTEM -> null
            }
            SpendGuardTheme(forceDark = forceDark) {
                val prefs          = remember { SecurePrefs.create(context, "spendguard_prefs_secure") }
                val userRepository = remember { UserRepository() }
                val onboardingDone = remember { prefs.getBoolean("onboarding_completed", false) }
                var screen         by remember { mutableStateOf(AppScreen.SPLASH) }
                val scope          = rememberCoroutineScope()

                val deepLinkUri by pendingDeepLink.collectAsState()
                LaunchedEffect(deepLinkUri) {
                    val uri = deepLinkUri ?: return@LaunchedEffect
                    when {
                        uri.scheme == "spendguard" && uri.host == "login-callback" -> {
                            while (screen == AppScreen.SPLASH) delay(100)
                            userRepository.handleGoogleCallback(uri.toString())
                            if (userRepository.isLoggedIn.value) screen = AppScreen.MAIN
                        }
                        uri.scheme == "spendguard" && uri.host == "referral" -> {
                            val code = uri.getQueryParameter("code")
                            if (!code.isNullOrBlank()) {
                                val referralPrefs = context.getSharedPreferences("spendguard_referral", Context.MODE_PRIVATE)
                                referralPrefs.edit().putString("pending_referral_code", code).apply()
                            }
                        }
                    }
                    pendingDeepLink.value = null
                }

                LaunchedEffect(Unit) {
                    delay(1500)
                    if (screen == AppScreen.SPLASH) {
                        when {
                            !onboardingDone -> screen = AppScreen.ONBOARDING
                            else -> {
                                userRepository.loadUserProfile()
                                screen = if (userRepository.isLoggedIn.value) AppScreen.MAIN else AppScreen.AUTH
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) {}
                    LaunchedEffect(screen) {
                        if (screen == AppScreen.MAIN) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Crossfade(targetState = screen, label = "app_screen") { currentScreen ->
                    when (currentScreen) {
                        AppScreen.SPLASH     -> SplashScreen()
                        AppScreen.ONBOARDING -> OnboardingScreen(
                            isReplay = false,
                            onFinish = {
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                screen = AppScreen.AUTH
                            }
                        )
                        AppScreen.AUTH       -> AuthScreen(
                            userRepository = userRepository,
                            onAuthSuccess  = { screen = AppScreen.MAIN }
                        )
                        AppScreen.MAIN       -> MainScreen(
                            userRepository      = userRepository,
                            pendingReevaluation = pendingReevaluation,
                            pendingWidgetItem    = pendingWidgetItem,
                            pendingWidgetPrice   = pendingWidgetPrice,
                            themeManager        = themeManager,
                            onSignOut           = { screen = AppScreen.AUTH }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { pendingDeepLink.value = it }
        intent.getStringExtra("reavaliar_item")?.let { pendingReevaluation.value = it }
        intent.getStringExtra("auto_item_name")?.takeIf { it.isNotEmpty() }?.let {
            pendingWidgetItem.value  = it
            pendingWidgetPrice.value = intent.getDoubleExtra("auto_item_price", 0.0)
        }
    }
}

@Composable
fun SplashScreen() {
    val gold  = Color(0xFFFFD700)
    val black = Color(0xFF121212)

    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(80);  phase = 1
        delay(420); phase = 2
        delay(380); phase = 3
        delay(300); phase = 4
    }

    val logoAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 1) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "la"
    )
    val logoScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 1) 1f else 0.6f,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.EaseOutBack),
        label = "ls"
    )
    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 2) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "ta"
    )
    val textOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 2) 0f else 20f,
        animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "to"
    )
    val featureAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 3) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "fa"
    )
    val progressAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (phase >= 4) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "pa"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = black) {
        Box(modifier = Modifier.fillMaxSize()) {

            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color  = gold.copy(alpha = 0.03f),
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f)
                )
                drawCircle(
                    color  = gold.copy(alpha = 0.025f),
                    radius = size.width * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.85f)
                )
            }

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .alpha(logoAlpha)
                        .scale(logoScale)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(130.dp)) {
                        drawCircle(color = gold.copy(alpha = 0.06f), radius = size.minDimension / 2f)
                        drawCircle(color = gold.copy(alpha = 0.04f), radius = size.minDimension / 2.3f)
                    }
                    Surface(
                        shape    = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                        color    = gold.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Center)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter            = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier           = Modifier.size(72.dp),
                                tint               = Color.Unspecified
                            )
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(textAlpha)
                        .offset(y = textOffset.dp)
                ) {
                    Text(
                        "SpendGuard",
                        style         = MaterialTheme.typography.displaySmall,
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = gold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "IA",
                                modifier  = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style     = MaterialTheme.typography.labelSmall,
                                color     = gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Seu guardião financeiro pessoal",
                            style         = MaterialTheme.typography.bodySmall,
                            color         = gold.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                Column(
                    modifier            = Modifier.alpha(featureAlpha),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "" to "Guardião anti-impulso com IA",
                        "" to "Streak de dias sem impulso",
                        "" to "Meta de economia personalizada",
                        "" to "Gráfico e histórico detalhado"
                    ).forEach { (emoji, label) ->
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(emoji, fontSize = 14.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier            = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .alpha(progressAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    modifier   = Modifier
                        .padding(horizontal = 80.dp)
                        .fillMaxWidth()
                        .height(2.dp),
                    color      = gold.copy(alpha = 0.8f),
                    trackColor = gold.copy(alpha = 0.12f)
                )
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userRepository: UserRepository,
    pendingReevaluation: MutableStateFlow<String?>,
    pendingWidgetItem: MutableStateFlow<String?>,
    pendingWidgetPrice: MutableStateFlow<Double>,
    themeManager: ThemeManager,
    onSignOut: () -> Unit
) {
    var currentView          by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showOnboardingOverlay by remember { mutableStateOf(false) }

    val context             = LocalContext.current
    val scope               = rememberCoroutineScope()
    val database            = remember { SpendGuardDatabase.getDatabase(context) }
    val geminiService       = remember { GeminiService(BuildConfig.BACKEND_URL) }
    val proManager          = remember { ProManager(context) }
    val billingManager      = remember { BillingManager(context, proManager) }
    val achievementsManager = remember { AchievementsManager(context) }
    val referralManager     = remember { ReferralManager(context, proManager) }
    val challengeManager       = remember { ChallengeManager(context) }
    val educationRepository    = remember { EducationRepository(SupabaseClient.client.postgrest) }
    val streakManager          = remember { StreakManager(context) }
    val goalManager            = remember { GoalManager(context) }
    val intentionsManager      = remember { IntentionsManager(context) }
    val weeklyInsightManager   = remember { WeeklyInsightManager(context) }

    var autoItemName  by remember { mutableStateOf("") }
    var autoItemPrice by remember { mutableStateOf(0.0) }
    var isPix         by remember { mutableStateOf(false) }

    val widgetItem  by pendingWidgetItem.collectAsState()
    val widgetPrice by pendingWidgetPrice.collectAsState()
    LaunchedEffect(widgetItem) {
        val name = widgetItem ?: return@LaunchedEffect
        if (name.isNotEmpty()) {
            autoItemName  = name
            autoItemPrice = widgetPrice
            currentView   = ViewState.SIMULATOR
            pendingWidgetItem.value  = null
            pendingWidgetPrice.value = 0.0
        }
    }

    val itemToReevaluate by pendingReevaluation.collectAsState()
    var showJustificationField  by remember { mutableStateOf(false) }
    var newJustification        by remember { mutableStateOf("") }
    var isAnalyzingReevaluation by remember { mutableStateOf(false) }

    val lastPurchase by if (itemToReevaluate != null) {
        database.purchaseDao().getLastPurchaseByName(itemToReevaluate!!).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    val currentUserId = remember { userRepository.getCurrentUserId() ?: "" }
    val allPurchases by database.purchaseDao().getPurchasesByUser(currentUserId).collectAsState(initial = emptyList())

    LaunchedEffect(allPurchases) {
        if (allPurchases.isNotEmpty()) {
            achievementsManager.checkAfterAnalysis(allPurchases, isPix = false)
        }
    }

    val newlyUnlocked by achievementsManager.newlyUnlocked.collectAsState()
    if (newlyUnlocked != null) {
        AlertDialog(
            onDismissRequest = { achievementsManager.consumeNewlyUnlocked() },
            icon = { Icon(newlyUnlocked!!.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Conquista Desbloqueada!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(newlyUnlocked!!.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(newlyUnlocked!!.description, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(onClick = { achievementsManager.consumeNewlyUnlocked() }) {
                    Text("Incrível!")
                }
            }
        )
    }

    if (itemToReevaluate != null) {
        val gold = MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = { },
            properties       = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            icon  = { Icon(Icons.Outlined.AcUnit, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(32.dp)) },
            title = { Text("Período de reflexão concluído", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("A emoção para '$itemToReevaluate' baixou. Você ainda deseja seguir com essa compra?")
                    if (showJustificationField) {
                        OutlinedTextField(
                            value         = newJustification,
                            onValueChange = { newJustification = it },
                            label         = { Text("Nova justificativa lógica") },
                            modifier      = Modifier.fillMaxWidth(),
                            placeholder   = { Text("Por que isso se tornou essencial?") },
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = gold,
                                focusedLabelColor  = gold
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (!showJustificationField) {
                    Button(
                        onClick = { pendingReevaluation.value = null },
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784), contentColor = Color(0xFF121212))
                    ) { Text("Desisti (Vitória!)", fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        enabled = newJustification.isNotBlank() && !isAnalyzingReevaluation,
                        onClick = {
                            isAnalyzingReevaluation = true
                            scope.launch {
                                try {
                                    val price   = lastPurchase?.price ?: 0.0
                                    val profile = ProfileManager(context).load()
                                    val analysis = geminiService.analyzeImpulse(itemToReevaluate!!, price, newJustification, profile)
                                    val existingId = lastPurchase?.id ?: 0
                                    if (existingId > 0) {
                                        database.purchaseDao().update(
                                            lastPurchase!!.copy(
                                                justification  = newJustification,
                                                wasBlocked     = !analysis.allowed,
                                                aiMessage      = analysis.message,
                                                coolingOffTime = analysis.coolingOffTime,
                                                category       = analysis.category
                                            )
                                        )
                                    } else {
                                        database.purchaseDao().insert(
                                            PurchaseEntity(
                                                userId        = userRepository.getCurrentUserId() ?: "",
                                                itemName      = itemToReevaluate!!,
                                                price         = price,
                                                justification = newJustification,
                                                wasBlocked    = !analysis.allowed,
                                                aiMessage     = analysis.message,
                                                coolingOffTime = analysis.coolingOffTime,
                                                category      = analysis.category
                                            )
                                        )
                                    }
                                    pendingReevaluation.value = null
                                    showJustificationField    = false
                                    newJustification          = ""
                                    currentView               = ViewState.HISTORY
                                } finally {
                                    isAnalyzingReevaluation = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF121212))
                    ) {
                        if (isAnalyzingReevaluation)
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF121212))
                        else
                            Text("Analisar Agora")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    if (!showJustificationField) showJustificationField = true
                    else { showJustificationField = false; pendingReevaluation.value = null }
                }) { Text(if (!showJustificationField) "Ainda quero" else "Cancelar") }
            }
        )
    }

    if (showOnboardingOverlay) {
        OnboardingScreen(isReplay = true, onFinish = { showOnboardingOverlay = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (currentView) {
                        ViewState.LIBRARY    -> "Biblioteca"
                        ViewState.SIMULATOR  -> "Guardião"
                        ViewState.DASHBOARD  -> "Início"
                        ViewState.HISTORY    -> "Histórico de Análises"
                        ViewState.SETTINGS   -> "Configurações"
                        else                 -> "SpendGuard"
                    })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                NavigationBarItem(
                    icon     = { Icon(Icons.Outlined.Home, null) },
                    label    = { Text("Início", maxLines = 1, softWrap = false) },
                    selected = currentView == ViewState.DASHBOARD,
                    onClick  = { currentView = ViewState.DASHBOARD },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Outlined.MenuBook, null) },
                    label    = { Text("Biblioteca", maxLines = 1, softWrap = false) },
                    selected = currentView == ViewState.LIBRARY,
                    onClick  = { currentView = ViewState.LIBRARY },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Outlined.Shield, null) },
                    label    = { Text("Guardião", maxLines = 1, softWrap = false) },
                    selected = currentView == ViewState.SIMULATOR,
                    onClick  = { currentView = ViewState.SIMULATOR },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Outlined.ReceiptLong, null) },
                    label    = { Text("Histórico", maxLines = 1, softWrap = false) },
                    selected = currentView == ViewState.HISTORY,
                    onClick  = { currentView = ViewState.HISTORY },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Outlined.Settings, null) },
                    label    = { Text("Ajustes", maxLines = 1, softWrap = false) },
                    selected = currentView == ViewState.SETTINGS,
                    onClick  = { currentView = ViewState.SETTINGS },
                    alwaysShowLabel = false
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentView) {
                ViewState.LIBRARY -> LibraryScreen(
                    database            = database,
                    educationRepository = educationRepository,
                    userRepository      = userRepository,
                    proManager          = proManager,
                    billingManager      = billingManager
                )
                ViewState.SIMULATOR -> SimulatorScreen(
                    geminiService       = geminiService,
                    database            = database,
                    proManager          = proManager,
                    billingManager      = billingManager,
                    userRepository      = userRepository,
                    achievementsManager = achievementsManager,
                    referralManager     = referralManager,
                    challengeManager    = challengeManager,
                    streakManager       = streakManager,
                    intentionsManager   = intentionsManager,
                    autoItemName        = autoItemName,
                    autoItemPrice       = autoItemPrice,
                    isPix               = isPix
                )
                ViewState.DASHBOARD -> DashboardScreen(
                    database             = database,
                    userRepository       = userRepository,
                    achievementsManager  = achievementsManager,
                    challengeManager     = challengeManager,
                    streakManager        = streakManager,
                    goalManager          = goalManager,
                    weeklyInsightManager = weeklyInsightManager,
                    proManager           = proManager,
                    onNavigate           = { destination -> currentView = destination }
                )
                ViewState.HISTORY -> HistoryScreen(
                    database            = database,
                    userRepository      = userRepository,
                    educationRepository = educationRepository,
                    proManager          = proManager,
                    onOpenImport        = { currentView = ViewState.IMPORT },
                    onShowPaywall       = { currentView = ViewState.PAYWALL }
                )
                ViewState.SETTINGS -> SettingsScreen(
                    database         = database,
                    proManager       = proManager,
                    billingManager   = billingManager,
                    userRepository   = userRepository,
                    themeManager     = themeManager,
                    goalManager      = goalManager,
                    onSignOut        = onSignOut,
                    onOpenOnboarding = { showOnboardingOverlay = true }
                )
                ViewState.ACHIEVEMENTS -> AchievementsScreen(
                    achievementsManager = achievementsManager,
                    onBack              = { currentView = ViewState.DASHBOARD }
                )
                ViewState.CHALLENGE -> { currentView = ViewState.DASHBOARD }
                ViewState.INTENTIONS -> IntentionsScreen(
                    intentionsManager = intentionsManager,
                    onBack            = { currentView = ViewState.DASHBOARD }
                )
                ViewState.IMPORT -> ImportScreen(
                    database       = database,
                    userRepository = userRepository,
                    isPro          = proManager.isPro.collectAsState().value,
                    onBack         = { currentView = ViewState.HISTORY },
                    onShowPaywall  = { currentView = ViewState.PAYWALL }
                )
                ViewState.PAYWALL -> PaywallScreen(
                    billingManager = billingManager,
                    proManager     = proManager,
                    onDismiss      = { currentView = ViewState.HISTORY }
                )
                else -> DashboardScreen(
                    database             = database,
                    userRepository       = userRepository,
                    achievementsManager  = achievementsManager,
                    challengeManager     = challengeManager,
                    streakManager        = streakManager,
                    goalManager          = goalManager,
                    weeklyInsightManager = weeklyInsightManager,
                    proManager           = proManager,
                    onNavigate           = { destination -> currentView = destination }
                )
            }
        }
    }
}