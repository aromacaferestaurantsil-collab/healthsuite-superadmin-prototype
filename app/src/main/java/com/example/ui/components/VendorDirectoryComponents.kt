package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Vendor
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.PillActiveBg
import com.example.ui.theme.PillActiveText
import com.example.ui.theme.PillPendingBg
import com.example.ui.theme.PillPendingText
import com.example.ui.theme.PillPlannedBg
import com.example.ui.theme.PillPlannedText
import com.example.ui.theme.PillSuspendedBg
import com.example.ui.theme.PillSuspendedText
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextControl
import com.example.ui.theme.TextLinkDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextTableHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSubscriptionExpiry(expiresAtMillis: Long): String {
    if (expiresAtMillis <= 0L) return "Not scheduled"
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(expiresAtMillis))
}

@Composable
fun SubscriptionDaysPill(
    daysLeft: Int,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when {
        daysLeft <= 0 -> Triple(PillSuspendedBg, PillSuspendedText, "Expired")
        daysLeft <= 15 -> Triple(PillPendingBg, PillPendingText, "$daysLeft days left")
        else -> Triple(PillActiveBg, PillActiveText, "$daysLeft days left")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SubscriptionTypePill(
    plan: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (plan.trim()) {
        "Enterprise" -> PillActiveBg to PillActiveText
        "Growth" -> PillPlannedBg to PillPlannedText
        else -> PillPendingBg to PillPendingText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${plan.ifBlank { "Starter" }} Plan",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun VendorTable(
    vendors: List<Vendor>,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vendors.isEmpty()) {
        Text(
            text = stringResource(R.string.empty_vendors),
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .testTag("empty_vendors_text")
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWideTable = maxWidth >= 640.dp
        Column(
            verticalArrangement = Arrangement.spacedBy(if (isWideTable) 0.dp else 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isWideTable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "VENDOR · EMAIL · PHONE",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2.2f)
                    )
                    Text(
                        text = "ADDRESS & FACILITY",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "SUBSCRIPTION & DAYS LEFT",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "STATUS",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.9f)
                    )
                    Spacer(modifier = Modifier.width(74.dp))
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }

            vendors.forEach { vendor ->
                if (isWideTable) {
                    WideVendorRow(vendor = vendor, onOpenVendor = onOpenVendor)
                    HorizontalDivider(color = RowDivider, thickness = 1.dp)
                } else {
                    CompactVendorDetailCard(vendor = vendor, onOpenVendor = onOpenVendor)
                }
            }
        }
    }
}

@Composable
private fun WideVendorRow(
    vendor: Vendor,
    onOpenVendor: (Vendor) -> Unit
) {
    val displayCode = vendor.vendorCode.ifBlank { vendor.id }
    val daysLeft = vendor.remainingDays()
    val expiryFormatted = formatSubscriptionExpiry(vendor.subscriptionExpiresAtMillis)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenVendor(vendor) }
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag("vendor_row_$displayCode")
    ) {
        Column(modifier = Modifier.weight(2.2f).padding(end = 10.dp)) {
            Text(
                text = vendor.name,
                color = TextLinkDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            DetailIconLine(
                icon = Icons.Outlined.Email,
                text = vendor.adminEmail.ifBlank { "No email" }
            )
            DetailIconLine(
                icon = Icons.Outlined.Phone,
                text = vendor.phone.ifBlank { "No phone" }
            )
        }
        Column(modifier = Modifier.weight(1.8f).padding(end = 10.dp)) {
            Text(
                text = "$displayCode · ${vendor.type} · ${vendor.city}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            DetailIconLine(
                icon = Icons.Outlined.LocationOn,
                text = vendor.address.ifBlank { "Address not set" },
                maxLines = 2
            )
        }
        Column(modifier = Modifier.weight(1.5f).padding(end = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubscriptionTypePill(plan = vendor.plan)
                SubscriptionDaysPill(daysLeft = daysLeft)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Exp: $expiryFormatted · ${if (vendor.autoRenew) "Auto-renew" else "Manual"}",
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = "${vendor.activeSeats}/${vendor.purchasedLicenses} seats · ${vendor.assignedModulesCsv.ifBlank { "PharmaSuite" }}",
                color = TextSubtle,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(0.9f)) {
            StatusPill(label = vendor.status)
        }
        DetailsActionButton(
            vendorId = displayCode,
            onClick = { onOpenVendor(vendor) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactVendorDetailCard(
    vendor: Vendor,
    onOpenVendor: (Vendor) -> Unit
) {
    val displayCode = vendor.vendorCode.ifBlank { vendor.id }
    val daysLeft = vendor.remainingDays()
    val expiryFormatted = formatSubscriptionExpiry(vendor.subscriptionExpiresAtMillis)

    Surface(
        color = CanvasBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenVendor(vendor) }
            .testTag("vendor_row_$displayCode")
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = vendor.name,
                        color = TextLinkDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$displayCode · ${vendor.type} · ${vendor.city}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                StatusPill(label = vendor.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubscriptionTypePill(plan = vendor.plan)
                SubscriptionDaysPill(daysLeft = daysLeft)
            }

            Spacer(modifier = Modifier.height(9.dp))
            HorizontalDivider(color = RowDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailIconLine(
                    icon = Icons.Outlined.LocationOn,
                    text = vendor.address.ifBlank { "Address not set" },
                    maxLines = 2
                )
                DetailIconLine(
                    icon = Icons.Outlined.Phone,
                    text = vendor.phone.ifBlank { "Phone not set" }
                )
                DetailIconLine(
                    icon = Icons.Outlined.Email,
                    text = buildString {
                        append(vendor.adminEmail.ifBlank { "Admin email not set" })
                        if (vendor.billingEmail.isNotBlank() && !vendor.billingEmail.equals(vendor.adminEmail, ignoreCase = true)) {
                            append(" (Billing: ${vendor.billingEmail})")
                        }
                    },
                    maxLines = 2
                )
                DetailIconLine(
                    icon = Icons.Outlined.EventAvailable,
                    text = "Expires $expiryFormatted · ${if (vendor.autoRenew) "Auto-Renew ON" else "Manual Renewal"} · ${vendor.activeSeats}/${vendor.purchasedLicenses} seats"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Modules: ${vendor.assignedModulesCsv.ifBlank { "None" }}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                DetailsActionButton(
                    vendorId = displayCode,
                    onClick = { onOpenVendor(vendor) }
                )
            }
        }
    }
}

@Composable
private fun DetailIconLine(
    icon: ImageVector,
    text: String,
    maxLines: Int = 1
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryEmerald,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailsActionButton(
    vendorId: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, ControlBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = TextControl
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .testTag("vendor_details_button_$vendorId")
    ) {
        Text(
            text = stringResource(R.string.action_details),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
