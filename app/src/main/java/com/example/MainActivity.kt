package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppPreferences
import com.example.data.local.LocalSecurityPrefs
import com.example.data.local.ThemePreference
import com.example.data.remote.KhataRepository
import com.example.ui.MainShell
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BrandSplashScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.auth.PinLockScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.theme.KhataMitraTheme
import com.example.ui.viewmodel.KhataViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { AppPreferences(context.applicationContext) }
            val loadedPrefs by prefs.prefsFlow.collectAsStateWithLifecycle(initialValue = null)
            val localPrefs = loadedPrefs ?: LocalSecurityPrefs()

            val darkTheme = when (localPrefs.themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            KhataMitraTheme(darkTheme = darkTheme) {
                AppContent(
                    prefs = prefs,
                    localPrefs = localPrefs,
                    prefsReady = loadedPrefs != null
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    prefs: AppPreferences,
    localPrefs: LocalSecurityPrefs,
    prefsReady: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var minSplashDone by remember { mutableStateOf(false) }
    var pinUnlocked by remember { mutableStateOf(false) }
    var autoSignInAttempted by remember { mutableStateOf(false) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val showSplash = !minSplashDone || !prefsReady

    LaunchedEffect(showSplash, localPrefs.onboardingCompleted, currentUser) {
        if (!showSplash && localPrefs.onboardingCompleted && currentUser == null && !autoSignInAttempted) {
            autoSignInAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    when {
        showSplash -> {
            BrandSplashScreen(onFinished = { minSplashDone = true })
        }
        !localPrefs.onboardingCompleted && currentUser == null -> {
            OnboardingScreen(
                selectedLanguage = localPrefs.language,
                onSelectLanguage = { lang -> scope.launch { prefs.setLanguage(lang) } },
                onComplete = { scope.launch { prefs.setOnboardingCompleted(true) } }
            )
        }
        currentUser == null -> {
            pinUnlocked = false
            AuthScreen(onAuthSuccess = {})
        }
        localPrefs.pinEnabled && !pinUnlocked -> {
            PinLockScreen(
                isSetupMode = false,
                biometricEnabled = localPrefs.biometricEnabled,
                verifyPin = { entered -> prefs.verifyPin(entered, localPrefs.pinHash) },
                onPinSetupComplete = {},
                onUnlocked = { pinUnlocked = true },
                onForgotPinSignOut = {
                    scope.launch {
                        prefs.disablePin()
                        signOutUser(credentialManager, scope)
                    }
                }
            )
        }
        else -> {
            val user = currentUser!!
            val uid = user.uid
            key(uid) {
                val khataViewModel: KhataViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val repository = KhataRepository(app)
                            KhataViewModel(
                                repository = repository,
                                prefs = prefs,
                                currentUserId = uid,
                                userDisplayName = user.displayName ?: "Merchant",
                                userEmail = user.email
                            )
                        }
                    }
                )
                MainShell(viewModel = khataViewModel)
            }
        }
    }
}
