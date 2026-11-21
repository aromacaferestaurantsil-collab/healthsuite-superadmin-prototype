package com.example

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FirestoreAdminRepository
import com.example.data.FirestoreCommerceRepository
import com.example.integrations.GoogleDriveUploader
import com.example.integrations.PaymentGatewayClient
import com.example.integrations.WhatsAppBridgeClient
import com.example.model.AdminPage
import com.example.model.VendorStatus
import com.example.security.KeystoreVault
import com.example.ui.components.EditVendorContactDialog
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.PrototypeBanner
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.RegisterVendorDialog
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.components.ToastBanner
import com.example.ui.components.VendorProfileSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.LicenseGuardScreen
import com.example.ui.screens.ModuleGroupsScreen
import com.example.ui.screens.ModulesPricingScreen
import com.example.ui.screens.OffersAndNotificationsScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PaymentCheckoutScreen
import com.example.ui.screens.PlatformUsersScreen
import com.example.ui.screens.ReleasesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.SupportDeskScreen
import com.example.ui.screens.TwoStepVerificationScreen
import com.example.ui.screens.VendorsScreen
import com.example.ui.screens.WhatsAppScreen
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.screens.signOutUser
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.HeaderBorder
import com.example.ui.theme.HealthSuite360Theme
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SidebarBg
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextHeading
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.AdminControlState
import com.example.ui.viewmodel.SuperAdminViewModel
import com.example.ui.viewmodel.WorkspaceData
import com.example.ui.viewmodel.WorkspaceUiState
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthSuite360Theme {
                AppRootNavigation()
            }
        }
    }
}

@Composable
private fun AppRootNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val vault = remember(context) { KeystoreVault(context.applicationContext) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isTwoStepVerified by rememberSaveable(currentUser?.uid) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            currentUser = user
            if (user == null) {
                isTwoStepVerified = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthGateScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else if (!isTwoStepVerified) {
        TwoStepVerificationScreen(
            userId = signedInUser.uid,
            userEmail = signedInUser.email.orEmpty(),
            vault = vault,
            onVerified = { isTwoStepVerified = true },
            onSignOut = {
                signOutUser(credentialManager, scope) {
                    isTwoStepVerified = false
                    currentUser = null
                }
            }
        )
    } else {
        key(signedInUser.uid) {
            val viewModel: SuperAdminViewModel = viewModel(
                key = signedInUser.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val adminRepo = FirestoreAdminRepository(db)
                        SuperAdminViewModel(
                            repository = adminRepo,
                            commerceRepository = FirestoreCommerceRepository(db, adminRepo),
                            vault = KeystoreVault(app),
                            driveUploader = GoogleDriveUploader(),
                            whatsAppClient = WhatsAppBridgeClient(),
                            paymentGatewayClient = PaymentGatewayClient(),
                            currentUserId = signedInUser.uid,
                            currentUserEmail = signedInUser.email.orEmpty(),
                            isEmailVerified = signedInUser.isEmailVerified
                        )
                    }
                }
            )
            HealthSuiteSuperAdminApp(
                viewModel = viewModel,
                userEmail = signedInUser.email.orEmpty(),
                onLockTwoStep = { isTwoStepVerified = false },
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        isTwoStepVerified = false
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
fun HealthSuiteSuperAdminApp(
    viewModel: SuperAdminViewModel,
    userEmail: String,
    onLockTwoStep: () -> Unit,
    onSignOut: () -> Unit
) {
    val controlState by viewModel.controlState.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    val workspaceData = (workspaceState as? WorkspaceUiState.Success)?.data ?: WorkspaceData()
    val visibleVendors = remember(
        workspaceData.vendors,
        controlState.searchQuery,
        controlState.statusFilter,
        controlState.planFilter
    ) {
        viewModel.filterVendors(workspaceData.vendors)
    }

    // Enforce FLAG_SECURE when enabled in settings
    val flagSecureEnabled = workspaceData.settings.firstOrNull { it.key == "flag_secure" }?.enabled ?: true
    DisposableEffect(activity, flagSecureEnabled) {
        if (activity != null) {
            if (flagSecureEnabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csvContent = viewModel.buildVendorsCsv(visibleVendors)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.flash("Exported vendors-demo.csv")
            }.onFailure {
                viewModel.flash("Unable to export CSV")
            }
        }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && activity != null) {
            runCatching {
                Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data)
            }.onSuccess { authResult ->
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    viewModel.completeDriveUploadWithToken(token, workspaceData.vendors)
                } else {
                    viewModel.onDriveConsentCancelled("Google Drive access token unavailable.")
                }
            }.onFailure { err ->
                viewModel.onDriveConsentCancelled(err.localizedMessage ?: "Drive consent failed.")
            }
        } else {
            viewModel.onDriveConsentCancelled()
        }
    }

    val triggerDriveUpload: () -> Unit = {
        if (activity != null) {
            viewModel.startDriveUpload(
                activity = activity,
                vendors = workspaceData.vendors,
                onNeedConsent = { pendingIntent ->
                    driveConsentLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent).build()
                    )
                }
            )
        } else {
            viewModel.flash("Activity context required for Google Drive authorization")
        }
    }

    val upiPaymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleUpiActivityResult(result.data)
    }

    val launchUpiPaymentIntent: (android.content.Intent) -> Unit = { intent ->
        runCatching {
            upiPaymentLauncher.launch(intent)
        }.onFailure {
            viewModel.flash("No UPI payment app found on this device")
        }
    }

    BackHandler(
        enabled = controlState.isQrScannerOpen ||
            controlState.editingVendor != null ||
            controlState.selectedVendor != null ||
            controlState.isAddVendorModalOpen ||
            drawerState.isOpen ||
            controlState.currentPage != AdminPage.Overview
    ) {
        when {
            controlState.isQrScannerOpen -> viewModel.closeQrScanner()
            controlState.editingVendor != null -> viewModel.closeEditVendorContact()
            controlState.selectedVendor != null -> viewModel.closeVendorProfile()
            controlState.isAddVendorModalOpen -> viewModel.closeAddVendorModal()
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            controlState.currentPage != AdminPage.Overview -> viewModel.selectPage(AdminPage.Overview)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 880.dp

        if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = SidebarBg,
                        drawerContentColor = Color.White,
                        modifier = Modifier.width(260.dp)
                    ) {
                        SidebarDrawerContent(
                            currentPage = controlState.currentPage,
                            userEmail = userEmail,
                            isSuperAdmin = controlState.isSuperAdmin,
                            onSelectPage = viewModel::selectPage,
                            onSignOut = onSignOut
                        )
                    }
                }
            ) {
                MainAdminShell(
                    controlState = controlState,
                    workspaceState = workspaceState,
                    visibleVendors = visibleVendors,
                    userEmail = userEmail,
                    showMenuButton = false,
                    onOpenMenu = {},
                    viewModel = viewModel,
                    onExportCsv = { csvExportLauncher.launch("vendors-demo.csv") },
                    onUploadToDrive = triggerDriveUpload,
                    onLaunchUpiIntent = launchUpiPaymentIntent,
                    onLockTwoStep = onLockTwoStep
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = SidebarBg,
                        drawerContentColor = Color.White,
                        modifier = Modifier.width(260.dp)
                    ) {
                        SidebarDrawerContent(
                            currentPage = controlState.currentPage,
                            userEmail = userEmail,
                            isSuperAdmin = controlState.isSuperAdmin,
                            onSelectPage = { page ->
                                viewModel.selectPage(page)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            ) {
                MainAdminShell(
                    controlState = controlState,
                    workspaceState = workspaceState,
                    visibleVendors = visibleVendors,
                    userEmail = userEmail,
                    showMenuButton = true,
                    onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                    viewModel = viewModel,
                    onExportCsv = { csvExportLauncher.launch("vendors-demo.csv") },
                    onUploadToDrive = triggerDriveUpload,
                    onLaunchUpiIntent = launchUpiPaymentIntent,
                    onLockTwoStep = onLockTwoStep
                )
            }
        }

        controlState.selectedVendor?.let { vendor ->
            VendorProfileSheet(
                vendor = vendor,
                isMutating = controlState.isMutating,
                onDismiss = viewModel::closeVendorProfile,
                onEditContact = viewModel::openEditVendorContact,
                onOpenCheckout = { v -> viewModel.openVendorCheckout(v.id) },
                onToggleAutoRenew = viewModel::toggleVendorAutoRenew,
                onApproveActivate = { id ->
                    viewModel.updateVendorStatus(id, VendorStatus.Active)
                },
                onSuspendVendor = { id ->
                    viewModel.updateVendorStatus(id, VendorStatus.Suspended)
                }
            )
        }

        controlState.editingVendor?.let { vendor ->
            EditVendorContactDialog(
                vendor = vendor,
                isSaving = controlState.isMutating,
                onSave = { name, type, plan, city, adminEmail, billingEmail, phone, address, autoRenew ->
                    viewModel.saveVendorContactDetails(
                        vendorId = vendor.id,
                        name = name,
                        type = type,
                        plan = plan,
                        city = city,
                        adminEmail = adminEmail,
                        billingEmail = billingEmail,
                        phone = phone,
                        address = address,
                        autoRenew = autoRenew
                    )
                },
                onDismiss = viewModel::closeEditVendorContact
            )
        }

        if (controlState.isAddVendorModalOpen) {
            RegisterVendorDialog(
                name = controlState.newVendorName,
                city = controlState.newVendorCity,
                type = controlState.newVendorType,
                plan = controlState.newVendorPlan,
                autoRenew = controlState.newVendorAutoRenew,
                adminEmail = controlState.newVendorAdminEmail,
                billingEmail = controlState.newVendorBillingEmail,
                phone = controlState.newVendorPhone,
                address = controlState.newVendorAddress,
                isSubmitting = controlState.isMutating,
                onFieldChange = viewModel::updateNewVendorField,
                onSubmit = { viewModel.submitNewVendor(workspaceData.vendors.size) },
                onDismiss = viewModel::closeAddVendorModal
            )
        }

        if (controlState.isQrScannerOpen) {
            QrScannerDialog(
                onQrScanned = viewModel::handleScannedQr,
                onDismiss = viewModel::closeQrScanner
            )
        }

        AnimatedVisibility(
            visible = controlState.toastMessage != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            controlState.toastMessage?.let { msg ->
                ToastBanner(message = msg)
            }
        }
    }
}

@Composable
private fun MainAdminShell(
    controlState: AdminControlState,
    workspaceState: WorkspaceUiState,
    visibleVendors: List<com.example.model.Vendor>,
    userEmail: String,
    showMenuButton: Boolean,
    onOpenMenu: () -> Unit,
    viewModel: SuperAdminViewModel,
    onExportCsv: () -> Unit,
    onUploadToDrive: () -> Unit,
    onLaunchUpiIntent: (android.content.Intent) -> Unit,
    onLockTwoStep: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        Surface(
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .defaultMinSize(minHeight = 60.dp)
                        .padding(horizontal = if (showMenuButton) 8.dp else 24.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showMenuButton) {
                            IconButton(
                                onClick = onOpenMenu,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("open_navigation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.cd_open_navigation),
                                    tint = TextHeading
                                )
                            }
                        }
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextMuted)) {
                                    append("HealthSuite 360  /  ")
                                }
                                withStyle(SpanStyle(color = TextHeading, fontWeight = FontWeight.Bold)) {
                                    append(controlState.currentPage.title)
                                }
                            },
                            fontSize = 12.5.sp
                        )
                    }
                    Text(
                        text = userEmail.substringBefore("@").ifBlank { stringResource(R.string.header_user_label) },
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                HorizontalDivider(color = HeaderBorder, thickness = 1.dp)
            }
        }

        val horizontalPad = if (showMenuButton) 16.dp else 28.dp

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "banner") {
                Spacer(modifier = Modifier.height(16.dp))
                PrototypeBanner(modifier = Modifier.padding(horizontal = horizontalPad))
            }

            item(key = "heading") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.control_centre_overline),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = controlState.currentPage.heading,
                            color = TextPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.control_centre_subtitle),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (controlState.currentPage == AdminPage.Vendors) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = viewModel::openAddVendorModal,
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryEmerald,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("add_vendor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_add_vendor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "page_body") {
                Box(modifier = Modifier.padding(horizontal = horizontalPad)) {
                    when (workspaceState) {
                        WorkspaceUiState.Loading -> {
                            PanelCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryEmerald,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Syncing secured workspace...", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }

                        is WorkspaceUiState.Error -> {
                            PanelCard {
                                InfoNoticeBox(text = workspaceState.message)
                            }
                        }

                        is WorkspaceUiState.Success -> {
                            val data = workspaceState.data
                            AnimatedContent(
                                targetState = controlState.currentPage,
                                transitionSpec = {
                                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                                },
                                label = "admin_page_transition"
                            ) { page ->
                                when (page) {
                                    AdminPage.Overview -> OverviewScreen(
                                        vendors = data.vendors,
                                        tickets = data.tickets,
                                        platformHealth = controlState.platformHealth,
                                        userEmail = userEmail,
                                        isDriveUploading = controlState.isDriveUploading,
                                        lastDriveUpload = controlState.lastDriveUpload,
                                        onUploadToDrive = onUploadToDrive,
                                        onOpenVendor = viewModel::openVendorProfile
                                    )
                                    AdminPage.Vendors -> VendorsScreen(
                                        visibleVendors = visibleVendors,
                                        totalVendorCount = data.vendors.size,
                                        searchQuery = controlState.searchQuery,
                                        statusFilter = controlState.statusFilter,
                                        planFilter = controlState.planFilter,
                                        userEmail = userEmail,
                                        isDriveUploading = controlState.isDriveUploading,
                                        lastDriveUpload = controlState.lastDriveUpload,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onStatusFilterChange = viewModel::updateStatusFilter,
                                        onPlanFilterChange = viewModel::updatePlanFilter,
                                        onExportCsv = onExportCsv,
                                        onUploadToDrive = onUploadToDrive,
                                        onOpenVendor = viewModel::openVendorProfile
                                    )
                                    AdminPage.Modules -> ModulesPricingScreen(
                                        modules = data.modules,
                                        onUpdateModulePricing = viewModel::updateModulePricing,
                                        onCreateModule = viewModel::createModule,
                                        onNavigateToCheckout = { viewModel.selectPage(AdminPage.PaymentCheckout) }
                                    )
                                    AdminPage.ModuleGroups -> ModuleGroupsScreen(
                                        modules = data.modules,
                                        groups = data.moduleGroups,
                                        onCreateGroup = viewModel::createModuleGroup,
                                        onBuyGroupOnline = { viewModel.selectPage(AdminPage.PaymentCheckout) }
                                    )
                                    AdminPage.Offers -> OffersAndNotificationsScreen(
                                        vendors = data.vendors,
                                        offers = data.offers,
                                        notifications = data.notifications,
                                        hasPushWebhook = controlState.paymentGatewayConfig.pushWebhookUrl.isNotBlank(),
                                        onCreateOffer = viewModel::createPromoOffer,
                                        onToggleOffer = viewModel::toggleOffer,
                                        onSendPushNotification = { targets, category, title, message, alsoOpenEmail ->
                                            viewModel.sendVendorPushNotification(
                                                context = context,
                                                targetVendors = targets,
                                                category = category,
                                                title = title,
                                                message = message,
                                                alsoOpenEmail = alsoOpenEmail
                                            )
                                        },
                                        onScanExpiringSubscriptions = {
                                            viewModel.scanExpiringAndAutoRenewalsAndNotify(
                                                context = context,
                                                vendors = data.vendors,
                                                activeOffers = data.offers
                                            )
                                        }
                                    )
                                    AdminPage.PaymentCheckout -> PaymentCheckoutScreen(
                                        vendors = data.vendors,
                                        modules = data.modules,
                                        moduleGroups = data.moduleGroups,
                                        offers = data.offers,
                                        payments = data.payments,
                                        preselectedVendorId = controlState.checkoutPreselectedVendorId,
                                        gatewayConfig = controlState.paymentGatewayConfig,
                                        isPaymentBusy = controlState.isPaymentBusy,
                                        onSaveGatewayConfig = viewModel::savePaymentGatewayConfig,
                                        onStartHttpsCheckout = { vendor, itemTitle, scopeType, assignedModules, seats, billingCycle, promoCode, subtotal, discount, finalAmount, autoRenew ->
                                            viewModel.initiateHttpsGatewayCheckout(
                                                context = context,
                                                vendor = vendor,
                                                itemTitle = itemTitle,
                                                scopeType = scopeType,
                                                assignedScopeModules = assignedModules,
                                                seats = seats,
                                                billingCycle = billingCycle,
                                                promoCode = promoCode,
                                                subtotalAmount = subtotal,
                                                discountAmount = discount,
                                                finalAmount = finalAmount,
                                                autoRenew = autoRenew
                                            )
                                        },
                                        onStartUpiCheckout = { vendor, itemTitle, scopeType, assignedModules, seats, billingCycle, promoCode, subtotal, discount, finalAmount, autoRenew ->
                                            viewModel.prepareUpiCheckout(
                                                vendor = vendor,
                                                itemTitle = itemTitle,
                                                scopeType = scopeType,
                                                assignedScopeModules = assignedModules,
                                                seats = seats,
                                                billingCycle = billingCycle,
                                                promoCode = promoCode,
                                                subtotalAmount = subtotal,
                                                discountAmount = discount,
                                                finalAmount = finalAmount,
                                                autoRenew = autoRenew,
                                                onLaunchUpiIntent = onLaunchUpiIntent
                                            )
                                        },
                                        onVerifyHttpsPayment = { pay, vendor ->
                                            viewModel.verifyHttpsGatewayPayment(
                                                payment = pay,
                                                vendor = vendor,
                                                allModules = data.modules,
                                                moduleGroups = data.moduleGroups
                                            )
                                        }
                                    )
                                    AdminPage.LicenseGuard -> LicenseGuardScreen(
                                        licenses = data.licenses,
                                        onRegisterDeviceCheckIn = viewModel::recordLicenseDeviceCheckIn,
                                        onEnforceSinglePrimaryDevice = viewModel::enforceSinglePrimaryDevice,
                                        onRevokeAndSuspendVendor = viewModel::revokeLicenseAndSuspendVendor,
                                        onSendSecurityWarning = { lic ->
                                            val target = data.vendors.firstOrNull {
                                                it.id == lic.vendorId || it.name == lic.vendorName
                                            }
                                            if (target != null) {
                                                viewModel.sendVendorPushNotification(
                                                    context = context,
                                                    targetVendors = listOf(target),
                                                    category = "License Security",
                                                    title = "Unauthorized Multi-Device License Alert",
                                                    message = "License ${lic.licenseKey} (${lic.purchasedSeats} seat) was detected on ${lic.activeDeviceCount} devices across ${lic.distinctIpCount} IPs. Extra devices will be locked.",
                                                    alsoOpenEmail = false
                                                )
                                            } else {
                                                viewModel.flash("Vendor record not found for security alert")
                                            }
                                        }
                                    )
                                    AdminPage.Subscriptions -> SubscriptionsScreen(
                                        vendors = data.vendors,
                                        onToggleAutoRenew = viewModel::toggleVendorAutoRenew,
                                        onOpenVendorCheckout = { v -> viewModel.openVendorCheckout(v.id) },
                                        onOpenVendor = viewModel::openVendorProfile
                                    )
                                    AdminPage.PlatformUsers -> PlatformUsersScreen(
                                        roles = controlState.platformRoles,
                                        currentUserEmail = userEmail,
                                        isSuperAdmin = controlState.isSuperAdmin
                                    )
                                    AdminPage.SupportDesk -> SupportDeskScreen(
                                        tickets = data.tickets,
                                        onResolveTicket = viewModel::resolveTicket
                                    )
                                    AdminPage.Releases -> ReleasesScreen(releases = controlState.releases)
                                    AdminPage.Analytics -> AnalyticsScreen(metrics = controlState.analyticsMetrics)
                                    AdminPage.AuditLog -> AuditLogScreen(logs = data.auditLogs)
                                    AdminPage.WhatsAppLink -> WhatsAppScreen(
                                        bridgeUrl = controlState.whatsAppBridgeUrl,
                                        bridgeToken = controlState.whatsAppBridgeToken,
                                        bridgeStatus = controlState.whatsAppStatus,
                                        onSaveConfig = viewModel::updateWhatsAppBridgeConfig,
                                        onRefreshBridge = viewModel::refreshWhatsAppBridgeStatus,
                                        onOpenQrScanner = viewModel::openQrScanner,
                                        onSendMessage = { phone, message, direct ->
                                            viewModel.sendWhatsAppMessage(context, phone, message, direct)
                                        }
                                    )
                                    AdminPage.Settings -> SettingsScreen(
                                        settings = data.settings,
                                        pwaPortalUrl = controlState.pwaPortalUrl,
                                        isCheckingPwa = controlState.isCheckingPwa,
                                        pwaReport = controlState.pwaReport,
                                        onToggleSetting = viewModel::toggleSetting,
                                        onLockSessionNow = onLockTwoStep,
                                        onPinHomeShortcut = { viewModel.pinHomeScreenShortcut(context) },
                                        onInspectPwaPortal = viewModel::inspectPwaPortal,
                                        onOpenWebPortal = { url -> viewModel.openSafePwaPortal(context, url) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = HeaderBorder, thickness = 1.dp)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.footer_title),
                        color = TextSubtle,
                        fontSize = 10.sp
                    )
                    Text(
                        text = stringResource(R.string.footer_disclaimer),
                        color = TextSubtle,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
