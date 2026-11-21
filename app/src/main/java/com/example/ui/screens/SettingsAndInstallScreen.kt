package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.integrations.PwaReadinessReport
import com.example.model.PlatformSettingDoc
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CheckboxEmerald
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: List<PlatformSettingDoc>,
    pwaPortalUrl: String,
    isCheckingPwa: Boolean,
    pwaReport: PwaReadinessReport?,
    onToggleSetting: (PlatformSettingDoc) -> Unit,
    onLockSessionNow: () -> Unit,
    onPinHomeShortcut: () -> Unit,
    onInspectPwaPortal: (String) -> Unit,
    onOpenWebPortal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var portalInput by remember(pwaPortalUrl) { mutableStateOf(pwaPortalUrl) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.testTag("settings_screen")
    ) {
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.InstallMobile,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Home Screen Shortcut & PWA Install Capabilities",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pin HealthSuite 360 to your Android Home Screen via deep link (healthsuite360://console), or inspect an HTTPS web portal's Web App Manifest & Service Worker before installing it as a PWA in the browser.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPinHomeShortcut,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, ControlBorder),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("pin_home_shortcut_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pin App Shortcut to Home Screen",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = RowDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            OutlinedTextField(
                value = portalInput,
                onValueChange = { portalInput = it },
                label = { Text("HTTPS Web / Vendor Portal URL (for PWA Check)") },
                placeholder = { Text("https://portal.healthsuite360.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pwa_portal_url_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onInspectPwaPortal(portalInput) },
                    enabled = !isCheckingPwa && portalInput.isNotBlank(),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("check_pwa_button")
                ) {
                    if (isCheckingPwa) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checking Manifest...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check PWA Installability", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = { onOpenWebPortal(portalInput) },
                    enabled = portalInput.isNotBlank(),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("open_pwa_portal_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Browser (Install PWA)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (pwaReport != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pwaReport.appName.ifBlank { pwaReport.portalUrl },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    StatusPill(label = if (pwaReport.isInstallable) "Active" else "Pending")
                }
                Spacer(modifier = Modifier.height(6.dp))
                InfoNoticeBox(
                    text = buildString {
                        append(pwaReport.summary)
                        append(" | HTTPS: ${if (pwaReport.httpsVerified) "OK" else "No"}")
                        append(" | Display: ${pwaReport.displayMode}")
                        append(" | Icons: ${pwaReport.iconCount}")
                        append(" | Service Worker: ${if (pwaReport.hasServiceWorker) "Detected" else "Not found"}")
                    }
                )
            }
        }

        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Two-Step Verification & Vault",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Authenticator secrets, payment keys, and WhatsApp bridge credentials are encrypted at rest with AES-256-GCM in the Android Keystore.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLockSessionNow,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, ControlBorder),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("lock_session_2fa_button")
            ) {
                Text(
                    text = "Lock Console & Require 2-Step Verification",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PanelCard {
            Text(
                text = "Platform settings",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            settings.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable { onToggleSetting(item) }
                        .padding(vertical = 6.dp)
                        .testTag("setting_row_${item.key}")
                ) {
                    Checkbox(
                        checked = item.enabled,
                        onCheckedChange = { onToggleSetting(item) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CheckboxEmerald,
                            checkmarkColor = Color.White,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        color = TextPrimary,
                        fontSize = 12.5.sp
                    )
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            InfoNoticeBox(text = stringResource(R.string.notice_settings))
        }
    }
}
