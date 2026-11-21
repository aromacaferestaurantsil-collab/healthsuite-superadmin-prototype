package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.security.AdminIdentityPolicy
import com.example.security.BiometricAuthHelper
import com.example.security.KeystoreVault
import com.example.security.TotpSecurityManager
import com.example.ui.theme.BrandMarkTeal
import com.example.ui.theme.BrandMint
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DangerText
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrototypeBannerBg
import com.example.ui.theme.PrototypeBannerBorder
import com.example.ui.theme.PrototypeBannerText
import com.example.ui.theme.SidebarBg
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context as Activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthGateScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SidebarBg, Color(0xFF17273E), CanvasBg)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp)
            .testTag("auth_gate_screen")
    ) {
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Artwork Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_console_banner_1795257171470),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x55111C2D), Color(0xE6111C2D))
                                )
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandMarkTeal)
                        ) {
                            Text(
                                text = "H",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                        append("HealthSuite ")
                                    }
                                    withStyle(SpanStyle(color = BrandMint, fontWeight = FontWeight.Bold)) {
                                        append("360")
                                    }
                                },
                                fontSize = 20.sp
                            )
                            Text(
                                text = "SUPER ADMIN SECURITY GATE",
                                color = Color(0xFFB5C7DC),
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Step 1 of 2 · Identity Sign-In",
                        color = PrimaryEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in to Platform Console",
                        color = TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Access is strictly isolated to your authenticated Google account and protected by Two-Step Verification.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrototypeBannerBg)
                            .border(1.dp, PrototypeBannerBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        SecurityHighlightRow("Registered Super Admin: ${AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL}")
                        SecurityHighlightRow("Live Google Sign-In + 2FA (no credentials stored)")
                        SecurityHighlightRow("Each user backs up strictly to their own Google Drive")
                    }

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DangerBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = DangerText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMessage = "Sign-in was cancelled. Select a Google account to continue."
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityHighlightRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = PrototypeBannerText,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = PrototypeBannerText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TwoStepVerificationScreen(
    userId: String,
    userEmail: String,
    vault: KeystoreVault,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val secretKeyPref = "totp_secret_$userId"
    val enrolledPref = "totp_enrolled_$userId"

    val existingSecret = remember(userId) { vault.getEncryptedString(secretKeyPref) }
    val isAlreadyEnrolled = remember(userId) {
        vault.getBoolean(enrolledPref, false) && !existingSecret.isNullOrBlank()
    }
    val activeSecret = remember(userId, existingSecret) {
        if (!existingSecret.isNullOrBlank()) {
            existingSecret
        } else {
            TotpSecurityManager.generateSecret().also { generated ->
                vault.putEncryptedString(secretKeyPref, generated)
            }
        }
    }

    val qrBitmap = remember(activeSecret, userEmail) {
        val uri = TotpSecurityManager.buildOtpAuthUri(
            accountEmail = userEmail.ifBlank { userId },
            secret = activeSecret
        )
        TotpSecurityManager.generateQrBitmap(uri, 420)
    }

    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableIntStateOf(TotpSecurityManager.remainingSecondsInWindow()) }
    val biometricAvailable = remember(context) { BiometricAuthHelper.isDeviceAuthAvailable(context) }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = TotpSecurityManager.remainingSecondsInWindow()
            delay(1000L)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp)
            .testTag("two_step_verification_screen")
    ) {
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrototypeBannerBg)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "STEP 2 OF 2 · TWO-STEP VERIFICATION",
                            color = PrimaryEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isAlreadyEnrolled) "Verify Authenticator Code" else "Enroll 2-Step Authenticator",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Signed in as ${userEmail.ifBlank { userId }}. Complete hardware-backed 2FA to unlock the Super Admin console.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )

                if (!isAlreadyEnrolled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = CanvasBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Scan with Google Authenticator, Authy, or 1Password",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Two-factor authenticator setup QR code",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = activeSecret.chunked(4).joinToString(" "),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("HealthSuite 360 2FA Secret", activeSecret).apply {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                description.extras = PersistableBundle().apply {
                                                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                                                }
                                            }
                                        }
                                        clipboard.setPrimaryClip(clip)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy 2FA setup key",
                                        tint = PrimaryEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "6-digit Authenticator Code",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Rotates in ${secondsLeft}s",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { next ->
                        if (next.length <= 6 && next.all { it.isDigit() }) {
                            codeInput = next
                            errorMessage = null
                        }
                    },
                    singleLine = true,
                    placeholder = { Text("000000", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = InputBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("totp_code_input")
                )

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = DangerText,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (TotpSecurityManager.verifyCode(activeSecret, codeInput)) {
                            vault.putBoolean(enrolledPref, true)
                            onVerified()
                        } else {
                            errorMessage = "Invalid or expired 6-digit code. Check your authenticator app clock."
                        }
                    },
                    enabled = codeInput.length == 6,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("verify_totp_button")
                ) {
                    Text(
                        text = if (isAlreadyEnrolled) "Verify & Unlock Console" else "Confirm & Enroll 2FA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (biometricAvailable && activity != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            BiometricAuthHelper.promptVerification(
                                activity = activity,
                                title = "HealthSuite 360 · 2-Step Verification",
                                subtitle = "Verify fingerprint, face, or device PIN",
                                onSuccess = {
                                    vault.putBoolean(enrolledPref, true)
                                    onVerified()
                                },
                                onError = { err -> errorMessage = err }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ControlBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("biometric_verify_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify with Device Biometric / PIN",
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    color = CardBorder,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("switch_account_button")
                ) {
                    Text(
                        text = "Sign out & switch Google account",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
