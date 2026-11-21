package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AdminPage
import com.example.model.VendorStatus
import com.example.ui.components.PrototypeBanner
import com.example.ui.components.RegisterVendorDialog
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.components.ToastBanner
import com.example.ui.components.VendorProfileSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PlatformUsersScreen
import com.example.ui.screens.ReleasesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.SupportDeskScreen
import com.example.ui.screens.VendorsScreen
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
import com.example.ui.viewmodel.SuperAdminUiState
import com.example.ui.viewmodel.SuperAdminViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthSuite360Theme {
                HealthSuiteSuperAdminApp()
            }
        }
    }
}

@Composable
fun HealthSuiteSuperAdminApp(
    viewModel: SuperAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csvContent = viewModel.buildVendorsCsv()
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

    BackHandler(
        enabled = uiState.selectedVendor != null ||
            uiState.isAddVendorModalOpen ||
            drawerState.isOpen ||
            uiState.currentPage != AdminPage.Overview
    ) {
        when {
            uiState.selectedVendor != null -> viewModel.closeVendorProfile()
            uiState.isAddVendorModalOpen -> viewModel.closeAddVendorModal()
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            uiState.currentPage != AdminPage.Overview -> viewModel.selectPage(AdminPage.Overview)
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
                        modifier = Modifier.width(256.dp)
                    ) {
                        SidebarDrawerContent(
                            currentPage = uiState.currentPage,
                            onSelectPage = { page -> viewModel.selectPage(page) }
                        )
                    }
                }
            ) {
                MainAdminShell(
                    uiState = uiState,
                    showMenuButton = false,
                    onOpenMenu = {},
                    viewModel = viewModel,
                    onExportCsv = { csvExportLauncher.launch("vendors-demo.csv") }
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = SidebarBg,
                        drawerContentColor = Color.White,
                        modifier = Modifier.width(256.dp)
                    ) {
                        SidebarDrawerContent(
                            currentPage = uiState.currentPage,
                            onSelectPage = { page ->
                                viewModel.selectPage(page)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                MainAdminShell(
                    uiState = uiState,
                    showMenuButton = true,
                    onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                    viewModel = viewModel,
                    onExportCsv = { csvExportLauncher.launch("vendors-demo.csv") }
                )
            }
        }

        // Vendor Profile Sheet
        uiState.selectedVendor?.let { vendor ->
            VendorProfileSheet(
                vendor = vendor,
                onDismiss = { viewModel.closeVendorProfile() },
                onApproveActivate = { id ->
                    viewModel.updateVendorStatus(id, VendorStatus.Active)
                },
                onSuspendVendor = { id ->
                    viewModel.updateVendorStatus(id, VendorStatus.Suspended)
                }
            )
        }

        // Register Vendor Modal
        if (uiState.isAddVendorModalOpen) {
            RegisterVendorDialog(
                name = uiState.newVendorName,
                email = uiState.newVendorEmail,
                onNameChange = viewModel::updateNewVendorName,
                onEmailChange = viewModel::updateNewVendorEmail,
                onSubmit = viewModel::submitNewVendor,
                onDismiss = viewModel::closeAddVendorModal
            )
        }

        // Toast Notification
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            uiState.toastMessage?.let { msg ->
                ToastBanner(message = msg)
            }
        }
    }
}

@Composable
private fun MainAdminShell(
    uiState: SuperAdminUiState,
    showMenuButton: Boolean,
    onOpenMenu: () -> Unit,
    viewModel: SuperAdminViewModel,
    onExportCsv: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        // Header Bar
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
                                    append(uiState.currentPage.title)
                                }
                            },
                            fontSize = 12.5.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.header_user_label),
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                HorizontalDivider(color = HeaderBorder, thickness = 1.dp)
            }
        }

        // Scrollable Body + Footer
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            val horizontalPad = if (showMenuButton) 16.dp else 28.dp

            Spacer(modifier = Modifier.height(16.dp))
            PrototypeBanner(modifier = Modifier.padding(horizontal = horizontalPad))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPad, vertical = 20.dp)
            ) {
                // Page Heading
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
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
                            text = uiState.currentPage.heading,
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

                    if (uiState.currentPage == AdminPage.Vendors) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { viewModel.openAddVendorModal() },
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

                Spacer(modifier = Modifier.height(18.dp))

                // Page Content
                when (uiState.currentPage) {
                    AdminPage.Overview -> OverviewScreen(
                        vendors = uiState.vendors,
                        tickets = uiState.tickets,
                        platformHealth = uiState.platformHealth,
                        onOpenVendor = viewModel::openVendorProfile
                    )
                    AdminPage.Vendors -> VendorsScreen(
                        visibleVendors = uiState.visibleVendors,
                        searchQuery = uiState.searchQuery,
                        statusFilter = uiState.statusFilter,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onStatusFilterChange = viewModel::updateStatusFilter,
                        onExportCsv = onExportCsv,
                        onOpenVendor = viewModel::openVendorProfile
                    )
                    AdminPage.Subscriptions -> SubscriptionsScreen(
                        vendors = uiState.vendors,
                        onOpenVendor = viewModel::openVendorProfile
                    )
                    AdminPage.Modules -> ModulesScreen(modules = uiState.modules)
                    AdminPage.PlatformUsers -> PlatformUsersScreen(roles = uiState.platformRoles)
                    AdminPage.SupportDesk -> SupportDeskScreen(
                        tickets = uiState.tickets,
                        onResolveTicket = viewModel::resolveTicket
                    )
                    AdminPage.Releases -> ReleasesScreen(releases = uiState.releases)
                    AdminPage.Analytics -> AnalyticsScreen(metrics = uiState.analyticsMetrics)
                    AdminPage.AuditLog -> AuditLogScreen(logs = uiState.auditLogs)
                    AdminPage.Settings -> SettingsScreen(
                        settings = uiState.settings,
                        onToggleSetting = viewModel::toggleSetting
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
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
