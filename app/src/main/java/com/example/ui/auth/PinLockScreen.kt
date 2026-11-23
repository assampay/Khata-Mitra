package com.example.ui.auth

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PinLockScreen(
    isSetupMode: Boolean,
    biometricEnabled: Boolean,
    verifyPin: (String) -> Boolean,
    onPinSetupComplete: (String) -> Unit,
    onUnlocked: () -> Unit,
    onForgotPinSignOut: () -> Unit,
    onCancelSetup: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var firstSetupPin by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && biometricEnabled && !isSetupMode) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Unlock KhataMitra")
                .setSubtitle("Verify your identity to access business ledger")
                .setNegativeButton("Use PIN", executor) { _, _ -> }
                .build()
            prompt.authenticate(
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onUnlocked()
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!isSetupMode && biometricEnabled) {
            triggerBiometric()
        }
    }

    fun onDigit(d: String) {
        if (enteredPin.length >= 4) return
        errorMsg = null
        val updated = enteredPin + d
        enteredPin = updated
        if (updated.length == 4) {
            if (isSetupMode) {
                if (firstSetupPin == null) {
                    firstSetupPin = updated
                    enteredPin = ""
                } else if (firstSetupPin == updated) {
                    onPinSetupComplete(updated)
                } else {
                    errorMsg = "PINs did not match. Try again."
                    firstSetupPin = null
                    enteredPin = ""
                }
            } else {
                if (verifyPin(updated)) {
                    onUnlocked()
                } else {
                    errorMsg = "Incorrect PIN. Please try again."
                    enteredPin = ""
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Security Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when {
                        isSetupMode && firstSetupPin == null -> "Set 4-Digit Khata PIN"
                        isSetupMode -> "Confirm Your 4-Digit PIN"
                        else -> "Enter KhataMitra PIN"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg ?: "Protect your business cashflow & customer records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMsg != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            PinKeyButton(label = digit, onClick = { onDigit(digit) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (!isSetupMode && biometricEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { triggerBiometric() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    PinKeyButton(label = "0", onClick = { onDigit("0") })

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                            }
                            .testTag("pin_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace"
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSetupMode && onCancelSetup != null) {
                    TextButton(onClick = onCancelSetup) {
                        Text("Cancel")
                    }
                } else {
                    TextButton(onClick = onForgotPinSignOut) {
                        Text("Forgot PIN? Sign out & Re-verify")
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
