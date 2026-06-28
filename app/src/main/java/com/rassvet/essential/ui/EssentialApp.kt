package com.rassvet.essential.ui

import android.net.Uri
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rassvet.essential.data.local.AppDatabase
import com.rassvet.essential.data.local.VaultPreferencesRepository
import com.rassvet.essential.data.chat.ChatEngine
import com.rassvet.essential.data.index.IndexRepository
import com.rassvet.essential.data.network.NetworkMonitor
import com.rassvet.essential.data.llm.HybridLocalLlmEngine
import com.rassvet.essential.data.llm.ChatModelSelection
import com.rassvet.essential.ui.auth.AuthScreen
import com.rassvet.essential.ui.auth.PostRegisterWelcomeScreen
import com.rassvet.essential.ui.chat.ChatScreen
import com.rassvet.essential.ui.editor.NoteEditorScreen
import com.rassvet.essential.ui.graph.GraphScreen
import com.rassvet.essential.ui.home.HomeChatState
import com.rassvet.essential.ui.home.MainHomeScreen
import com.rassvet.essential.ui.onboarding.CreateVaultScreen
import com.rassvet.essential.ui.onboarding.OnboardingScreen
import com.rassvet.essential.ui.settings.ChangeVaultScreen
import com.rassvet.essential.ui.settings.SettingsScreen
import com.rassvet.essential.ui.splash.SplashScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class SplashTarget {
    Auth,
    Welcome,
    OnboardingViaAuth,
    Main,
}

private fun androidx.navigation.NavHostController.openNoteEditor(noteUri: Uri) {
    val encoded = URLEncoder.encode(noteUri.toString(), StandardCharsets.UTF_8.toString())
    navigate("editor/$encoded") {
        launchSingleTop = true
    }
}


data class PendingNoteContext(
    val title: String,
    val body: String,
    val uri: Uri? = null,
)

@Composable
fun EssentialApp(
    db: AppDatabase,
    prefs: VaultPreferencesRepository,
    indexRepository: IndexRepository,
    chatEngine: ChatEngine,
    localEngine: HybridLocalLlmEngine,
    networkMonitor: NetworkMonitor,
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val token by prefs.authToken.collectAsState(initial = null)
    var authSessionReady by remember { mutableStateOf(false) }


    LaunchedEffect(prefs) {
        prefs.authToken.first()
        authSessionReady = true
    }

    LaunchedEffect(token, authSessionReady) {
        if (!authSessionReady) return@LaunchedEffect
        if (!token.isNullOrBlank()) return@LaunchedEffect
        when (navController.currentDestination?.route) {
            AppRoutes.Main,
            AppRoutes.Onboarding,
            AppRoutes.CreateVault,
            AppRoutes.Welcome,
            -> {
                navController.navigate(AppRoutes.Auth) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Splash,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(tween(300, easing = EaseOutCubic)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(300, easing = EaseOutCubic)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(tween(300, easing = EaseOutCubic)) { it / 4 }
        },
    ) {
        composable(AppRoutes.Splash) {
            var animationDone by remember { mutableStateOf(false) }
            var splashTarget by remember { mutableStateOf<SplashTarget?>(null) }

            LaunchedEffect(Unit) {
                val tok = prefs.authToken.first()
                val welcome = prefs.needsRegisterWelcome.first()
                val vault = prefs.vaultTreeUri.first()
                splashTarget =
                    when {
                        tok.isNullOrBlank() -> SplashTarget.Auth
                        welcome -> SplashTarget.Welcome
                        vault.isNullOrBlank() -> SplashTarget.OnboardingViaAuth
                        else -> SplashTarget.Main
                    }
            }

            LaunchedEffect(animationDone, splashTarget) {
                if (!animationDone || splashTarget == null) return@LaunchedEffect
                when (splashTarget) {
                    SplashTarget.Auth ->
                        navController.navigate(AppRoutes.Auth) {
                            popUpTo(AppRoutes.Splash) { inclusive = true }
                        }
                    SplashTarget.Welcome ->
                        navController.navigate(AppRoutes.Welcome) {
                            popUpTo(AppRoutes.Splash) { inclusive = true }
                        }
                    SplashTarget.OnboardingViaAuth -> {
                        navController.navigate(AppRoutes.Auth) {
                            popUpTo(AppRoutes.Splash) { inclusive = true }
                        }
                        navController.navigate(AppRoutes.Onboarding)
                    }
                    SplashTarget.Main -> {
                        scope.launch {
                            ChatModelSelection.applyDefaultEssentialAi(prefs)
                            navController.navigate(AppRoutes.Main) {
                                popUpTo(AppRoutes.Splash) { inclusive = true }
                            }
                        }
                    }
                    null -> Unit
                }
            }

            SplashScreen(onAnimationComplete = { animationDone = true })
        }
        composable(AppRoutes.Auth) {
            AuthScreen(prefs = prefs) { isRegister ->
                if (isRegister) {
                    navController.navigate(AppRoutes.Welcome)
                } else {
                    scope.launch {
                        prefs.clearVaultSelection()
                        ChatModelSelection.applyDefaultEssentialAi(prefs)
                        navController.navigate(AppRoutes.Onboarding) {
                            popUpTo(AppRoutes.Auth) { inclusive = true }
                        }
                    }
                }
            }
        }
        composable(AppRoutes.Welcome) {
            PostRegisterWelcomeScreen(prefs = prefs) {
                navController.navigate(AppRoutes.Onboarding) {
                    popUpTo(AppRoutes.Welcome) { inclusive = true }
                }
            }
        }
        composable(AppRoutes.Onboarding) {
            OnboardingScreen(
                prefs = prefs,
                navController = navController,
                onVaultPickedToMain = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Auth) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.CreateVault) {
            CreateVaultScreen(
                prefs = prefs,
                onVaultReady = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Auth) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.Main) {
            EssentialMainNav(
                db = db,
                prefs = prefs,
                indexRepository = indexRepository,
                chatEngine = chatEngine,
                localEngine = localEngine,
                networkMonitor = networkMonitor,
                onLogout = {
                    navController.navigate(AppRoutes.Auth) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

@Composable
private fun EssentialMainNav(
    db: AppDatabase,
    prefs: VaultPreferencesRepository,
    indexRepository: IndexRepository,
    chatEngine: ChatEngine,
    localEngine: HybridLocalLlmEngine,
    networkMonitor: NetworkMonitor,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    var vaultRefreshPulse by remember { mutableIntStateOf(0) }
    var pendingNoteContext by remember { mutableStateOf<PendingNoteContext?>(null) }
    var pendingNoteContextPulse by remember { mutableIntStateOf(0) }
    val homeChatState = remember { HomeChatState() }
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 }
        },
        exitTransition = {
            fadeOut(tween(200, easing = EaseInOutCubic)) +
                slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 5 }
        },
        popEnterTransition = {
            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 5 }
        },
        popExitTransition = {
            fadeOut(tween(200, easing = EaseInOutCubic)) +
                slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 }
        },
    ) {
        composable("home") {
            MainHomeScreen(
                prefs = prefs,
                db = db,
                indexRepository = indexRepository,
                chatEngine = chatEngine,
                localEngine = localEngine,
                networkMonitor = networkMonitor,
                chatState = homeChatState,
                vaultRefreshPulse = vaultRefreshPulse,
                pendingNoteContext = pendingNoteContext,
                pendingNoteContextPulse = pendingNoteContextPulse,
                onPendingNoteContextConsumed = { pendingNoteContext = null },
                onOpenNote = { uri -> navController.openNoteEditor(uri) },
                onSettings = { navController.navigate("settings") },
            )
        }
        composable("graph") {
            GraphScreen(
                db = db,
                prefs = prefs,
                onOpenNote = { uri -> navController.openNoteEditor(uri) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "editor/{noteUri}",
            arguments =
                listOf(
                    navArgument("noteUri") { type = NavType.StringType },
                ),
            enterTransition = {
                fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth }
            },
            exitTransition = {
                fadeOut(tween(220, easing = EaseInOutCubic)) +
                    slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 }
            },
            popEnterTransition = {
                fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 }
            },
            popExitTransition = {
                fadeOut(tween(220, easing = EaseInOutCubic)) +
                    slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth }
            },
        ) { entry ->
            val encoded = entry.arguments?.getString("noteUri") ?: return@composable
            val uri = Uri.parse(java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString()))
            key(uri) {
                NoteEditorScreen(
                    prefs = prefs,
                    db = db,
                    indexRepository = indexRepository,
                    noteUri = uri,
                    onAskAi = { title, body ->
                        pendingNoteContext = PendingNoteContext(title, body, uri)
                        pendingNoteContextPulse++
                        navController.popBackStack()
                    },
                    onOpenNote = { linkedUri -> navController.openNoteEditor(linkedUri) },
                    onBack = { navController.popBackStack() },
                    onSaved = { vaultRefreshPulse++ },
                    onDeleted = { vaultRefreshPulse++ },
                )
            }
        }
        composable("chat") {
            ChatScreen(
                db = db,
                prefs = prefs,
                chatEngine = chatEngine,
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
                onChangeVault = { navController.navigate("change_vault") },
            )
        }
        composable("change_vault") {
            ChangeVaultScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                onCreateNewVault = { navController.navigate("create_vault_from_settings") },
                onVaultChanged = { vaultRefreshPulse++ },
            )
        }
        composable("create_vault_from_settings") {
            CreateVaultScreen(
                prefs = prefs,
                onVaultReady = {
                    vaultRefreshPulse++
                    navController.popBackStack("home", false)
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}


