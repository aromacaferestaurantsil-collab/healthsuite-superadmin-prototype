package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModuleGroupDoc
import com.example.model.PaymentGatewayConfig
import com.example.model.PaymentTransactionDoc
import com.example.model.PlatformModuleDoc
import com.example.model.PromoOfferDoc
import com.example.model.Vendor
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentCheckoutScreen(
    vendors: List<Vendor>,
    modules: List<PlatformModuleDoc>,
    moduleGroups: List<ModuleGroupDoc>,
    offers: List<PromoOfferDoc>,
    payments: List<PaymentTransactionDoc>,
    preselectedVendorId: String?,
    gatewayConfig: PaymentGatewayConfig,
    isPaymentBusy: Boolean,
    onSaveGatewayConfig: (PaymentGatewayConfig) -> Unit,
    onStartHttpsCheckout: (
        vendor: Vendor,
        itemTitle: String,
        scopeType: String,
        assignedModules: String,
        seats: Int,
        billingCycle: String,
        promoCode: String,
        subtotal: Double,
        discount: Double,
        finalAmount: Double,
        autoRenew: Boolean
    ) -> Unit,
    onStartUpiCheckout: (
        vendor: Vendor,
        itemTitle: String,
        scopeType: String,
        assignedModules: String,
        seats: Int,
        billingCycle: String,
        promoCode: String,
        subtotal: Double,
        discount: Double,
        finalAmount: Double,
        autoRenew: Boolean
    ) -> Unit,
    onVerifyHttpsPayment: (PaymentTransactionDoc, Vendor?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGatewaySettings by remember { mutableStateOf(false) }
    var gwUrl by remember(gatewayConfig.gatewayApiUrl) { mutableStateOf(gatewayConfig.gatewayApiUrl) }
    var gwToken by remember(gatewayConfig.gatewayBearerToken) { mutableStateOf(gatewayConfig.gatewayBearerToken) }
    var upiVpa by remember(gatewayConfig.upiVpa) { mutableStateOf(gatewayConfig.upiVpa) }
    var merchantName by remember(gatewayConfig.merchantName) { mutableStateOf(gatewayConfig.merchantName) }
    var pushUrl by remember(gatewayConfig.pushWebhookUrl) { mutableStateOf(gatewayConfig.pushWebhookUrl) }
    var pushToken by remember(gatewayConfig.pushWebhookToken) { mutableStateOf(gatewayConfig.pushWebhookToken) }

    var selectedVendorId by remember(vendors, preselectedVendorId) {
        mutableStateOf(preselectedVendorId ?: vendors.firstOrNull()?.id.orEmpty())
    }
    var purchaseType by remember { mutableStateOf("All Modules") } // "Module", "Module Group", "All Modules"
    var selectedModuleId by remember(modules) { mutableStateOf(modules.firstOrNull()?.id.orEmpty()) }
    var selectedGroupId by remember(moduleGroups) { mutableStateOf(moduleGroups.firstOrNull()?.id.orEmpty()) }
    var billingCycle by remember { mutableStateOf("Annual") }
    var seatsText by remember { mutableStateOf("1") }
    var selectedOfferCode by remember { mutableStateOf("") }
    var autoRenew by remember { mutableStateOf(true) }

    val selectedVendor = vendors.firstOrNull { it.id == selectedVendorId }
    val selectedModule = modules.firstOrNull { it.id == selectedModuleId }
    val selectedGroup = moduleGroups.firstOrNull { it.id == selectedGroupId }
    val seats = (seatsText.toIntOrNull() ?: 1).coerceIn(1, 500)
    val selectedOffer = offers.firstOrNull { it.isActive && it.code == selectedOfferCode }

    val allEligibleModules = modules.filter { it.licenseAllEligible }
    val itemTitle: String
    val assignedModulesCsv: String
    val unitBasePrice: Double

    when (purchaseType) {
        "Module" -> {
            itemTitle = selectedModule?.name ?: "Select Module"
            assignedModulesCsv = selectedModule?.name.orEmpty()
            unitBasePrice = if (billingCycle == "Annual") {
                selectedModule?.effectiveAnnualPrice() ?: 0.0
            } else {
                selectedModule?.effectiveMonthlyPrice() ?: 0.0
            }
        }
        "Module Group" -> {
            itemTitle = selectedGroup?.name ?: "Select Module Group"
            assignedModulesCsv = if (selectedGroup?.includesAllModules == true) {
                allEligibleModules.joinToString(",") { it.name }
            } else {
                selectedGroup?.moduleCodesCsv.orEmpty()
            }
            val monthly = selectedGroup?.effectiveBundlePrice() ?: 0.0
            unitBasePrice = if (billingCycle == "Annual") monthly * 10.0 else monthly
        }
        else -> {
            itemTitle = "All Modules Enterprise Licensing (${allEligibleModules.size} Modules)"
            assignedModulesCsv = allEligibleModules.joinToString(",") { it.name }
            val sumMonthly = allEligibleModules.sumOf { it.effectiveMonthlyPrice() } * 0.70
            unitBasePrice = if (billingCycle == "Annual") sumMonthly * 10.0 else sumMonthly
        }
    }

    val subtotal = ((unitBasePrice * seats) * 100.0).roundToLong() / 100.0
    val offerDiscountPercent = selectedOffer?.discountPercent ?: 0
    val discountAmount = ((subtotal * offerDiscountPercent / 100.0) * 100.0).roundToLong() / 100.0
    val finalPayable = ((subtotal - discountAmount).coerceAtLeast(0.0) * 100.0).roundToLong() / 100.0

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth().testTag("payment_checkout_screen")) {
        PanelCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Payment, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Online Payment Gateway", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Supports HTTPS Merchant Checkout (Razorpay/Stripe bridge) & Android UPI Intent", color = TextMuted, fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = { showGatewaySettings = !showGatewaySettings },
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("toggle_gateway_config_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showGatewaySettings) "Hide Keys" else "Gateway Keys", fontSize = 11.sp)
                }
            }

            if (showGatewaySettings) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoNoticeBox(text = "Credentials are encrypted at rest with AES-256-GCM inside Android Keystore. Never use cleartext HTTP for remote hosts.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = gwUrl, onValueChange = { gwUrl = it }, label = { Text("HTTPS Gateway API Base URL (/orders)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = gwToken, onValueChange = { gwToken = it }, label = { Text("Gateway Bearer Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = upiVpa, onValueChange = { upiVpa = it }, label = { Text("Merchant UPI VPA (e.g., healthsuite@icici)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("merchant_upi_input"))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = merchantName, onValueChange = { merchantName = it }, label = { Text("Merchant Payee Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = pushUrl, onValueChange = { pushUrl = it }, label = { Text("Optional HTTPS Push Notification Webhook URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = pushToken, onValueChange = { pushToken = it }, label = { Text("Push Webhook Bearer Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveGatewayConfig(
                            PaymentGatewayConfig(gwUrl, gwToken, upiVpa, merchantName, pushUrl, pushToken)
                        )
                        showGatewaySettings = false
                    },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("save_gateway_config_button")
                ) {
                    Text("Encrypt & Save Gateway Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Checkout Builder
        PanelCard {
            Text("Vendor Online Module Purchase", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("1. Select Vendor", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vendors.forEach { v ->
                    FilterChip(
                        selected = selectedVendorId == v.id,
                        onClick = { selectedVendorId = v.id },
                        label = { Text(v.name, fontSize = 11.sp) }
                    )
                }
            }
            selectedVendor?.let { v ->
                Text(
                    text = "Email: ${v.adminEmail} · Phone: ${v.phone} · Remaining: ${v.remainingDays()}d",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("2. Licensing Package", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All Modules", "Module Group", "Module").forEach { type ->
                    FilterChip(selected = purchaseType == type, onClick = { purchaseType = type }, label = { Text(type, fontSize = 11.sp) })
                }
            }

            if (purchaseType == "Module") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modules.forEach { m ->
                        FilterChip(selected = selectedModuleId == m.id, onClick = { selectedModuleId = m.id }, label = { Text("${m.name} (₹${m.effectiveMonthlyPrice().toInt()})", fontSize = 11.sp) })
                    }
                }
            } else if (purchaseType == "Module Group") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    moduleGroups.forEach { g ->
                        FilterChip(selected = selectedGroupId == g.id, onClick = { selectedGroupId = g.id }, label = { Text("${g.name} (₹${g.effectiveBundlePrice().toInt()})", fontSize = 11.sp) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1.2f)) {
                    listOf("Monthly", "Annual").forEach { cycle ->
                        FilterChip(selected = billingCycle == cycle, onClick = { billingCycle = cycle }, label = { Text(cycle, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(
                    value = seatsText,
                    onValueChange = { next -> if (next.all { it.isDigit() }) seatsText = next },
                    label = { Text("License Seats") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (offers.any { it.isActive }) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Apply Offer / Discount", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = selectedOfferCode.isEmpty(), onClick = { selectedOfferCode = "" }, label = { Text("No Promo", fontSize = 11.sp) })
                    offers.filter { it.isActive }.forEach { o ->
                        FilterChip(
                            selected = selectedOfferCode == o.code,
                            onClick = { selectedOfferCode = o.code },
                            label = { Text("${o.code} (-${o.discountPercent}%)", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("Enable Auto-Renewal on Subscription", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
            }

            HorizontalDivider(color = RowDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Subtotal ($seats seat${if (seats > 1) "s" else ""} · $billingCycle)", color = TextSecondary, fontSize = 12.sp)
                Text("INR ${subtotal.toInt()}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (discountAmount > 0.0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Offer Discount ($selectedOfferCode -$offerDiscountPercent%)", color = PrimaryEmerald, fontSize = 12.sp)
                    Text("- INR ${discountAmount.toInt()}", color = PrimaryEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Total Payable", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("INR ${finalPayable.toInt()}", color = PrimaryEmerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectedVendor?.let { v ->
                            onStartHttpsCheckout(
                                v, itemTitle, purchaseType, assignedModulesCsv, seats,
                                billingCycle, selectedOfferCode, subtotal, discountAmount, finalPayable, autoRenew
                            )
                        }
                    },
                    enabled = !isPaymentBusy && selectedVendor != null && finalPayable > 0.0,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("https_checkout_button")
                ) {
                    if (isPaymentBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Pay via HTTPS Gateway", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        selectedVendor?.let { v ->
                            onStartUpiCheckout(
                                v, itemTitle, purchaseType, assignedModulesCsv, seats,
                                billingCycle, selectedOfferCode, subtotal, discountAmount, finalPayable, autoRenew
                            )
                        }
                    },
                    enabled = !isPaymentBusy && selectedVendor != null && finalPayable > 0.0,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("upi_checkout_button")
                ) {
                    Text("Pay via UPI App Intent", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Orders & Transactions Ledger
        PanelCard {
            Text("Online Payment Transactions (${payments.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (payments.isEmpty()) {
                Text("No payment gateway transactions recorded yet.", color = TextMuted, fontSize = 12.sp)
            } else {
                payments.forEach { pay ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("${pay.vendorName} · ${pay.itemTitle}", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text("${pay.orderRef} · ${pay.currency} ${pay.finalAmount.toInt()} · ${pay.seats} seats · ${pay.gatewayMode}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(label = pay.status)
                            if (pay.status == "Pending" && pay.gatewayMode == "HTTPS Gateway") {
                                OutlinedButton(
                                    onClick = {
                                        val v = vendors.firstOrNull { it.id == pay.vendorId }
                                        onVerifyHttpsPayment(pay, v)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ControlBorder),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(14.dp))
                                    Text(" Verify", color = PrimaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = RowDivider, thickness = 1.dp)
                }
            }
        }
    }
}
