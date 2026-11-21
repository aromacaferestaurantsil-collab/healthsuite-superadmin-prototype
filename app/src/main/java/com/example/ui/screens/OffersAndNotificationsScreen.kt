package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PromoOfferDoc
import com.example.model.Vendor
import com.example.model.VendorNotificationDoc
import com.example.notifications.VendorNotificationHelper
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OffersAndNotificationsScreen(
    vendors: List<Vendor>,
    offers: List<PromoOfferDoc>,
    notifications: List<VendorNotificationDoc>,
    hasPushWebhook: Boolean,
    onCreateOffer: (code: String, title: String, desc: String, discountPercent: Int, targetScope: String, validDays: Int) -> Unit,
    onToggleOffer: (PromoOfferDoc) -> Unit,
    onSendPushNotification: (targets: List<Vendor>, category: String, title: String, message: String, alsoOpenEmail: Boolean) -> Unit,
    onScanExpiringSubscriptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notifPermissionGranted by remember {
        mutableStateOf(VendorNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted
    }

    // Create Offer form
    var offerCode by remember { mutableStateOf("") }
    var offerTitle by remember { mutableStateOf("") }
    var offerPercent by remember { mutableStateOf("20") }
    var offerScope by remember { mutableStateOf("All Modules") }
    var offerDays by remember { mutableStateOf("14") }

    // Push composer state
    var selectedCategory by remember { mutableStateOf("Offer") }
    var targetAllVendors by remember { mutableStateOf(true) }
    var selectedVendorId by remember(vendors) { mutableStateOf(vendors.firstOrNull()?.id.orEmpty()) }
    var pushTitle by remember { mutableStateOf("Limited-Time Module Discount Offer") }
    var pushMessage by remember { mutableStateOf("Hello {vendor}, enjoy 20% OFF with code HEALTH20. Remaining: {days} · Auto-renew: {autorenew}.") }
    var alsoLaunchEmail by remember { mutableStateOf(false) }

    val expiringCount = vendors.count { it.remainingDays() <= 15 }
    val manualRenewCount = vendors.count { !it.autoRenew }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth().testTag("offers_notifications_screen")) {
        if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PanelCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Enable Android Push Notifications", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        Text("Grant notification permission so alert campaigns post live Android system notifications.", color = TextMuted, fontSize = 11.5.sp)
                    }
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Grant", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Subscription Remaining & Auto-Renewal Scanner
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subscription Remaining & Auto-Renewal Alerts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$expiringCount vendor(s) expiring within 15 days · $manualRenewCount vendor(s) with Auto-Renew OFF.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onScanExpiringSubscriptions()
                },
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("scan_renewals_push_button")
            ) {
                Text("Scan Expiring & Auto-Renewals -> Push Notifications", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Promotional Offers & Discounts Manager
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Offers & Discounts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = offerCode,
                    onValueChange = { offerCode = it },
                    label = { Text("Offer Code") },
                    placeholder = { Text("SAVE25") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("offer_code_input")
                )
                OutlinedTextField(
                    value = offerPercent,
                    onValueChange = { offerPercent = it },
                    label = { Text("Discount %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.7f)
                )
                OutlinedTextField(
                    value = offerDays,
                    onValueChange = { offerDays = it },
                    label = { Text("Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = offerTitle,
                onValueChange = { offerTitle = it },
                label = { Text("Offer Title") },
                placeholder = { Text("Festival Bundle Discount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("offer_title_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All Modules", "Module Group", "Single Module", "Renewal").forEach { scope ->
                    FilterChip(selected = offerScope == scope, onClick = { offerScope = scope }, label = { Text(scope, fontSize = 11.sp) })
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onCreateOffer(
                        offerCode,
                        offerTitle,
                        "Save ${offerPercent}% on $offerScope licensing",
                        offerPercent.toIntOrNull() ?: 10,
                        offerScope,
                        offerDays.toIntOrNull() ?: 14
                    )
                    offerCode = ""
                    offerTitle = ""
                },
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("create_offer_button")
            ) {
                Text("Create Offer / Discount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            offers.forEach { offer ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("${offer.code} · ${offer.discountPercent}% OFF", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${offer.title} (${offer.targetScope})", color = TextSecondary, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                selectedCategory = "Offer"
                                pushTitle = "${offer.discountPercent}% Off: ${offer.title}"
                                pushMessage = "Hello {vendor}, use promo code ${offer.code} for ${offer.discountPercent}% OFF (${offer.targetScope}). Days left: {days}."
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ControlBorder),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Load Alert", color = PrimaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = offer.isActive, onCheckedChange = { onToggleOffer(offer) })
                    }
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
        }

        // Push Notification Composer
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Push Offer / Renewal Alert to Vendors", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            InfoNoticeBox(
                text = if (hasPushWebhook) "HTTPS Push Webhook configured: alerts post both locally and to your remote push relay."
                else "Alerts fire real Android notifications, persist to Firestore, and can optionally launch Email. Configure an HTTPS Push Webhook in Buy Modules -> Gateway Settings for remote device push."
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Offer", "Subscription Remaining", "Auto Renewal", "License Security").forEach { cat ->
                    FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 11.sp) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Broadcast to All Vendors (${vendors.size})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(checked = targetAllVendors, onCheckedChange = { targetAllVendors = it })
            }
            if (!targetAllVendors) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vendors.forEach { v ->
                        FilterChip(
                            selected = selectedVendorId == v.id,
                            onClick = { selectedVendorId = v.id },
                            label = { Text(v.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pushTitle,
                onValueChange = { pushTitle = it },
                label = { Text("Notification Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pushMessage,
                onValueChange = { pushMessage = it },
                label = { Text("Message ({vendor}, {days}, {autorenew} supported)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (!targetAllVendors) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Also compose Email via Vendor Email ID", color = TextSecondary, fontSize = 12.sp)
                    Switch(checked = alsoLaunchEmail, onCheckedChange = { alsoLaunchEmail = it })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val targets = if (targetAllVendors) vendors else vendors.filter { it.id == selectedVendorId }
                    onSendPushNotification(targets, selectedCategory, pushTitle, pushMessage, alsoLaunchEmail)
                },
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("send_vendor_push_button")
            ) {
                Text("Send Push Notification", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        PanelCard {
            Text("Vendor Notification Delivery Log (${notifications.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            notifications.take(15).forEach { log ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${log.vendorName} · ${log.category}", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        StatusPill(label = log.status)
                    }
                    Text(log.title, color = PrimaryEmerald, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(log.message, color = TextSecondary, fontSize = 11.sp)
                    Text("Channel: ${log.deliveryChannel} · ${log.recipientEmail.ifBlank { log.recipientPhone }}", color = TextMuted, fontSize = 10.sp)
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
        }
    }
}
