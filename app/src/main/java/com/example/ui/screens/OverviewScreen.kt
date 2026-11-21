package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DriveUploadResult
import com.example.model.PlatformHealthService
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.model.VendorStatus
import com.example.security.NetworkUrlValidator
import com.example.ui.components.PanelCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.VendorTable
import com.example.ui.theme.BrandMint
import com.example.ui.theme.CardBorder
import com.example.ui.theme.OperationalGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrototypeBannerBg
import com.example.ui.theme.PrototypeBannerBorder
import com.example.ui.theme.PrototypeBannerText
import com.example.ui.theme.RowDivider
import com.example.ui.theme.SidebarBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    vendors: List<Vendor>,
    tickets: List<SupportTicket>,
    platformHealth: List<PlatformHealthService>,
    userEmail: String,
    isDriveUploading: Boolean,
    lastDriveUpload: DriveUploadResult?,
    onUploadToDrive: () -> Unit,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats = listOf(
        Triple("Total vendors", vendors.size.toString(), Icons.Outlined.Business),
        Triple("Active vendors", vendors.count { it.statusEnum == VendorStatus.Active }.toString(), Icons.Outlined.CheckCircle),
        Triple("Pending approval", vendors.count { it.statusEnum == VendorStatus.Pending }.toString(), Icons.Outlined.PendingActions),
        Triple("Open tickets", tickets.count { it.status != "Resolved" }.toString(), Icons.Outlined.SupportAgent)
    )

    Column(modifier = modifier.fillMaxWidth().testTag("overview_screen")) {
        // Hero Security & Cloud Drive Command Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_console_banner_1795257171470),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SidebarBg.copy(alpha = 0.94f),
                                    SidebarBg.copy(alpha = 0.80f),
                                    Color(0xCC167B65)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = BrandMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2-STEP VERIFIED SESSION",
                            color = BrandMint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Platform Security & Cloud Backup",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time Firestore tenant isolation active. Export encrypted CSV backups directly to your own Google Drive (${userEmail.ifBlank { "signed-in account" }}).",
                        color = Color(0xFFD6E3F0),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUploadToDrive,
                            enabled = !isDriveUploading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryEmerald,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("overview_drive_upload_button")
                        ) {
                            if (isDriveUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading to Drive...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup to Google Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val safeDriveUrl = lastDriveUpload?.webViewLink?.let { NetworkUrlValidator.parseSafeHttpsUrl(it) }
                        if (safeDriveUrl != null) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(safeDriveUrl.toString()))
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BrandMint.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = BrandMint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Last Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (lastDriveUpload != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrototypeBannerBg.copy(alpha = 0.92f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Last Drive backup: ${lastDriveUpload.fileName} (${lastDriveUpload.ownerEmail.ifBlank { userEmail }})",
                                color = PrototypeBannerText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isFourColumns = maxWidth >= 700.dp
            if (isFourColumns) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stats.forEach { (label, value, icon) ->
                        StatMetricCard(
                            label = label,
                            value = value,
                            footnote = "Firestore live",
                            icon = icon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatMetricCard(
                            label = stats[0].first,
                            value = stats[0].second,
                            footnote = "Firestore live",
                            icon = stats[0].third,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = stats[1].first,
                            value = stats[1].second,
                            footnote = "Firestore live",
                            icon = stats[1].third,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatMetricCard(
                            label = stats[2].first,
                            value = stats[2].second,
                            footnote = "Firestore live",
                            icon = stats[2].third,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = stats[3].first,
                            value = stats[3].second,
                            footnote = "Firestore live",
                            icon = stats[3].third,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTwoColumns = maxWidth >= 760.dp
            if (isTwoColumns) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecentVendorsPanel(
                        vendors = vendors.take(4),
                        onOpenVendor = onOpenVendor,
                        modifier = Modifier.weight(1.4f)
                    )
                    PlatformHealthPanel(
                        platformHealth = platformHealth,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RecentVendorsPanel(
                        vendors = vendors.take(4),
                        onOpenVendor = onOpenVendor
                    )
                    PlatformHealthPanel(platformHealth = platformHealth)
                }
            }
        }
    }
}

@Composable
private fun RecentVendorsPanel(
    vendors: List<Vendor>,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        Text(
            text = "Recent vendors",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        VendorTable(vendors = vendors, onOpenVendor = onOpenVendor)
    }
}

@Composable
private fun PlatformHealthPanel(
    platformHealth: List<PlatformHealthService>,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        Text(
            text = "Platform health · live status",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        platformHealth.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = item.name,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = item.status,
                    color = OperationalGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = RowDivider, thickness = 1.dp)
        }
    }
}
