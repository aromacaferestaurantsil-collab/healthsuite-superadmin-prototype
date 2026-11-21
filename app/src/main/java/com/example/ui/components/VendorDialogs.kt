package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Vendor
import com.example.model.VendorStatus
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DangerBorder
import com.example.ui.theme.DangerText
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VendorProfileSheet(
    vendor: Vendor,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onEditContact: (Vendor) -> Unit,
    onOpenCheckout: (Vendor) -> Unit,
    onToggleAutoRenew: (Vendor) -> Unit,
    onApproveActivate: (String) -> Unit,
    onSuspendVendor: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val daysRemaining = vendor.remainingDays()
    val expiryDate = formatSubscriptionExpiry(vendor.subscriptionExpiresAtMillis)
    val isOverAllocated = vendor.activeSeats > vendor.purchasedLicenses

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        modifier = Modifier.testTag("vendor_profile_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.vendor_profile_overline),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_vendor_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_panel),
                        tint = TextPrimary
                    )
                }
            }

            Text(
                text = vendor.name,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${vendor.vendorCode.ifBlank { vendor.id }} · ${vendor.type} · ${vendor.city}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(label = vendor.status)
                SubscriptionTypePill(plan = vendor.plan)
                SubscriptionDaysPill(daysLeft = daysRemaining)
                if (isOverAllocated) {
                    StatusPill(label = "Unauthorized Sharing")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Contact Actions (Call / Email)
            if (vendor.phone.isNotBlank() || vendor.adminEmail.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vendor.phone.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val telDigits = vendor.phone.filter { it.isDigit() || it == '+' }
                                if (telDigits.isNotBlank()) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telDigits"))
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, ControlBorder),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call ${vendor.phone}", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (vendor.adminEmail.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(vendor.adminEmail)}"))
                                    )
                                }
                            },
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, ControlBorder),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email Admin", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 1: Contact & Location
            ProfileDetailSection(
                title = "CONTACT & ADDRESS",
                rows = listOf(
                    "Organisation Name" to vendor.name,
                    "Facility Address" to vendor.address.ifBlank { "Not set" },
                    "City / Facility Type" to "${vendor.city.ifBlank { "Not specified" }} · ${vendor.type}",
                    "Phone Number" to vendor.phone.ifBlank { "Not set" },
                    "Admin Email ID" to vendor.adminEmail.ifBlank { "Not set" },
                    "Billing Email ID" to vendor.billingEmail.ifBlank { vendor.adminEmail.ifBlank { "Not set" } }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Subscription & Licensing
            ProfileDetailSection(
                title = "SUBSCRIPTION & LICENSING",
                rows = listOf(
                    "Vendor ID" to vendor.vendorCode.ifBlank { vendor.id },
                    "Subscription Type" to "${vendor.plan} Plan",
                    "Subscription Days Left" to if (daysRemaining > 0) "$daysRemaining days left" else "Expired",
                    "Expiry Date" to expiryDate,
                    "Assigned Modules" to vendor.assignedModulesCsv.ifBlank { "None" },
                    "License Seats" to "${vendor.activeSeats} active / ${vendor.purchasedLicenses} purchased"
                ),
                highlightKey = if (isOverAllocated) "License Seats" else if (daysRemaining <= 15) "Subscription Days Left" else null
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CanvasBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Renewal Subscription", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (vendor.autoRenew) "Automatically renews before expiry" else "Manual renewal required",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = vendor.autoRenew,
                    onCheckedChange = { onToggleAutoRenew(vendor) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onOpenCheckout(vendor) },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buy Modules", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onEditContact(vendor) },
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("edit_vendor_details_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Details", color = TextPrimary, fontSize = 12.sp)
                }

                if (vendor.statusEnum != VendorStatus.Active) {
                    Button(
                        onClick = { onApproveActivate(vendor.id) },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("approve_vendor_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_approve_activate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (vendor.statusEnum != VendorStatus.Suspended) {
                    OutlinedButton(
                        onClick = { onSuspendVendor(vendor.id) },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, DangerBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DangerBg,
                            contentColor = DangerText
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("suspend_vendor_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_suspend_vendor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailSection(
    title: String,
    rows: List<Pair<String, String>>,
    highlightKey: String? = null
) {
    Surface(
        color = CanvasBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                color = PrimaryEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            rows.forEachIndexed { idx, (key, value) ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(text = key, color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        color = if (highlightKey == key) DangerText else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(color = RowDivider, thickness = 1.dp)
                }
            }
        }
    }
}
