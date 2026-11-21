package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.Vendor
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterVendorDialog(
    name: String,
    city: String,
    type: String,
    plan: String,
    autoRenew: Boolean,
    adminEmail: String,
    billingEmail: String,
    phone: String,
    address: String,
    isSubmitting: Boolean,
    onFieldChange: (
        name: String?,
        city: String?,
        type: String?,
        plan: String?,
        autoRenew: Boolean?,
        adminEmail: String?,
        billingEmail: String?,
        phone: String?,
        address: String?
    ) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val facilityTypes = listOf("Clinic", "Hospital", "Pharmacy", "Laboratory")
    val subscriptionPlans = listOf("Starter", "Growth", "Enterprise")
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_vendor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.modal_register_vendor),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_register_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close_panel),
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                VendorInputField(
                    label = stringResource(R.string.label_org_name),
                    value = name,
                    onValueChange = { onFieldChange(it, null, null, null, null, null, null, null, null) },
                    testTag = "vendor_name_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Facility type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    facilityTypes.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { onFieldChange(null, null, option, null, null, null, null, null, null) },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Subscription type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subscriptionPlans.forEach { option ->
                        FilterChip(
                            selected = plan == option,
                            onClick = { onFieldChange(null, null, null, option, null, null, null, null, null) },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                VendorInputField(
                    label = stringResource(R.string.label_vendor_city),
                    value = city,
                    onValueChange = { onFieldChange(null, it, null, null, null, null, null, null, null) },
                    testTag = "vendor_city_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VendorInputField(
                    label = stringResource(R.string.label_admin_email),
                    value = adminEmail,
                    keyboardType = KeyboardType.Email,
                    onValueChange = { onFieldChange(null, null, null, null, null, it, null, null, null) },
                    testTag = "vendor_email_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VendorInputField(
                    label = stringResource(R.string.label_billing_email),
                    value = billingEmail,
                    keyboardType = KeyboardType.Email,
                    onValueChange = { onFieldChange(null, null, null, null, null, null, it, null, null) },
                    testTag = "vendor_billing_email_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VendorInputField(
                    label = stringResource(R.string.label_vendor_phone),
                    value = phone,
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { onFieldChange(null, null, null, null, null, null, null, it, null) },
                    testTag = "vendor_phone_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VendorInputField(
                    label = stringResource(R.string.label_vendor_address),
                    value = address,
                    singleLine = false,
                    onValueChange = { onFieldChange(null, null, null, null, null, null, null, null, it) },
                    testTag = "vendor_address_input"
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Renew Subscription", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = autoRenew,
                        onCheckedChange = { onFieldChange(null, null, null, null, it, null, null, null, null) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("submit_register_vendor_button")
                ) {
                    Text(
                        text = if (isSubmitting) "Saving to Firestore..." else stringResource(R.string.action_add_to_queue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditVendorContactDialog(
    vendor: Vendor,
    isSaving: Boolean,
    onSave: (
        name: String,
        type: String,
        plan: String,
        city: String,
        adminEmail: String,
        billingEmail: String,
        phone: String,
        address: String,
        autoRenew: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val facilityTypes = listOf("Clinic", "Hospital", "Pharmacy", "Laboratory")
    val subscriptionPlans = listOf("Starter", "Growth", "Enterprise")

    var name by remember(vendor.id) { mutableStateOf(vendor.name) }
    var type by remember(vendor.id) { mutableStateOf(vendor.type) }
    var plan by remember(vendor.id) { mutableStateOf(vendor.plan) }
    var city by remember(vendor.id) { mutableStateOf(vendor.city) }
    var adminEmail by remember(vendor.id) { mutableStateOf(vendor.adminEmail) }
    var billingEmail by remember(vendor.id) { mutableStateOf(vendor.billingEmail) }
    var phone by remember(vendor.id) { mutableStateOf(vendor.phone) }
    var address by remember(vendor.id) { mutableStateOf(vendor.address) }
    var autoRenew by remember(vendor.id) { mutableStateOf(vendor.autoRenew) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Vendor Details", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_panel))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                VendorInputField("Organisation Name", name, { name = it })
                Spacer(modifier = Modifier.height(10.dp))

                Text("Facility Type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    facilityTypes.forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option, fontSize = 11.sp) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Subscription Type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subscriptionPlans.forEach { option ->
                        FilterChip(selected = plan == option, onClick = { plan = option }, label = { Text(option, fontSize = 11.sp) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                VendorInputField("City", city, { city = it })
                Spacer(modifier = Modifier.height(10.dp))
                VendorInputField("Admin Email ID", adminEmail, { adminEmail = it }, KeyboardType.Email)
                Spacer(modifier = Modifier.height(10.dp))
                VendorInputField("Billing Email ID", billingEmail, { billingEmail = it }, KeyboardType.Email)
                Spacer(modifier = Modifier.height(10.dp))
                VendorInputField("Phone Number", phone, { phone = it }, KeyboardType.Phone)
                Spacer(modifier = Modifier.height(10.dp))
                VendorInputField("Address", address, { address = it }, singleLine = false)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Renew Subscription", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onSave(name, type, plan, city, adminEmail, billingEmail, phone, address, autoRenew) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save Vendor Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VendorInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    testTag: String? = null
) {
    Column {
        Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            maxLines = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(7.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = InputBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
    }
}
