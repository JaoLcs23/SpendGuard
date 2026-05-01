package com.joaolucas.spendguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    private val pendingDeepLink     = MutableStateFlow<Uri?>(null)
    private val pendingReevaluation = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.data?.let { pendingDeepLink.value = it }
        intent?.getStringExtra("reavaliar_item")?.let { pendingReevaluation.value = it }

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
                val prefs          = remember { context.getSharedPreferences("spendguard_prefs", Context.MODE_PRIVATE) }
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
    }
}

@Composable
fun SplashScreen() {
    val gold    = Color(0xFFFFD700)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue    = if (visible) 1f else 0f,
        animationSpec  = androidx.compose.animation.core.tween(600),
        label          = "alpha"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.alpha(alpha).padding(32.dp)
            ) {
                Icon(
                    painter            = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo SpendGuard",
                    modifier           = Modifier.size(120.dp),
                    tint               = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    "SpendGuard",
                    style         = MaterialTheme.typography.displayMedium,
                    color         = Color.White,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Seu guardião financeiro",
                    style         = MaterialTheme.typography.titleMedium,
                    color         = gold.copy(alpha = 0.85f),
                    fontWeight    = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
            }
            CircularProgressIndicator(
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .size(24.dp)
                    .alpha(alpha),
                color       = gold,
                strokeWidth = 2.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userRepository: UserRepository,
    pendingReevaluation: MutableStateFlow<String?>,
    themeManager: ThemeManager,
    onSignOut: () -> Unit
) {
    var currentView          by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showOnboardingOverlay by remember { mutableStateOf(false) }

    val context             = LocalContext.current
    val scope               = rememberCoroutineScope()
    val database            = remember { SpendGuardDatabase.getDatabase(context) }
    val geminiService       = remember { GeminiService(BuildConfig.GEMINI_API_KEY) }
    val proManager          = remember { ProManager(context) }
    val billingManager      = remember { BillingManager(context, proManager) }
    val achievementsManager = remember { AchievementsManager(context) }
    val referralManager     = remember { ReferralManager(context, proManager) }
    val challengeManager    = remember { ChallengeManager(context) }
    val educationRepository = remember { EducationRepository(SupabaseClient.client.postgrest) }

    var autoItemName  by remember { mutableStateOf("") }
    var autoItemPrice by remember { mutableStateOf(0.0) }
    var isPix         by remember { mutableStateOf(false) }

    val itemToReevaluate by pendingReevaluation.collectAsState()
    var showJustificationField  by remember { mutableStateOf(false) }
    var newJustification        by remember { mutableStateOf("") }
    var isAnalyzingReevaluation by remember { mutableStateOf(false) }

    val lastPurchase by if (itemToReevaluate != null) {
        database.purchaseDao().getLastPurchaseByName(itemToReevaluate!!).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
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
                    autoItemName        = autoItemName,
                    autoItemPrice       = autoItemPrice,
                    isPix               = isPix
                )
                ViewState.DASHBOARD -> DashboardScreen(
                    database            = database,
                    userRepository      = userRepository,
                    achievementsManager = achievementsManager,
                    challengeManager    = challengeManager,
                    onNavigate          = { destination -> currentView = destination }
                )
                ViewState.HISTORY -> HistoryScreen(
                    database       = database,
                    userRepository = userRepository,
                    onOpenImport   = { currentView = ViewState.IMPORT }
                )
                ViewState.SETTINGS -> SettingsScreen(
                    database         = database,
                    proManager       = proManager,
                    billingManager   = billingManager,
                    userRepository   = userRepository,
                    themeManager     = themeManager,
                    onSignOut        = onSignOut,
                    onOpenOnboarding = { showOnboardingOverlay = true }
                )
                ViewState.ACHIEVEMENTS -> AchievementsScreen(
                    achievementsManager = achievementsManager,
                    onBack              = { currentView = ViewState.DASHBOARD }
                )
                ViewState.CHALLENGE -> ChallengeScreen(
                    challengeManager = challengeManager,
                    isPro            = proManager.isPro.value,
                    onBack           = { currentView = ViewState.DASHBOARD },
                    onShowPaywall    = { currentView = ViewState.PAYWALL }
                )
                ViewState.IMPORT -> ImportScreen(
                    database       = database,
                    userRepository = userRepository,
                    isPro          = proManager.isPro.value,
                    onBack         = { currentView = ViewState.HISTORY },
                    onShowPaywall  = { currentView = ViewState.PAYWALL }
                )
                ViewState.PAYWALL -> PaywallScreen(
                    billingManager = billingManager,
                    proManager     = proManager,
                    onDismiss      = { currentView = ViewState.HISTORY }
                )
                else -> DashboardScreen(
                    database            = database,
                    userRepository      = userRepository,
                    achievementsManager = achievementsManager,
                    challengeManager    = challengeManager,
                    onNavigate          = { destination -> currentView = destination }
                )
            }
        }
    }
}