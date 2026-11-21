package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.VendorLicenseDoc
import com.example.security.LicenseSecurityEngine
import com.example.ui.components.PanelCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DangerBorder
import com.example.ui.theme.DangerText
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LicenseGuardScreen(
    licenses: List<VendorLicenseDoc>,
    onRegisterDeviceCheckIn: (VendorLicenseDoc, deviceId: String, extraIpJump: Boolean) -> Unit,
    onEnforceSinglePrimaryDevice: (VendorLicenseDoc) -> Unit,
    onRevokeAndSuspendVendor: (VendorLicenseDoc) -> Unit,
    onSendSecurityWarning: (VendorLicenseDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    var checkInTarget by remember { mutableStateOf<VendorLicenseDoc?>(null) }

    val singleLicenseAbusers = licenses.filter { it.isSingleLicenseAbuse }
    val allViolations = licenses.filter {
        val eval = LicenseSecurityEngine.evaluateDoc(it)
        eval.status != "Compliant"
    }
    val totalActiveDevices = licenses.sumOf { it.activeDeviceCount }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth().testTag("license_guard_screen")) {
        if (singleLicenseAbusers.isNotEmpty()) {
            Surface(
                color = DangerBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DangerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Filled.GppBad, contentDescription = null, tint = DangerText, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${singleLicenseAbusers.size} Single-License Unauthorized Sharing Detected",
                            color = DangerText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vendor(s) purchased 1 license seat but active across multiple devices/IPs: ${singleLicenseAbusers.joinToString { it.vendorName }}.",
                            color = DangerText,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatMetricCard(
                label = "1-License Abusers",
                value = singleLicenseAbusers.size.toString(),
                footnote = "1 seat, >1 device",
                icon = Icons.Filled.Warning,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                label = "Policy Flags",
                value = allViolations.size.toString(),
                footnote = "$totalActiveDevices active seats",
                icon = Icons.Filled.Devices,
                modifier = Modifier.weight(1f)
            )
        }

        licenses.forEach { lic ->
            val eval = LicenseSecurityEngine.evaluateDoc(lic)
            PanelCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(lic.vendorName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${lic.licenseKey} · ${lic.scopeName}", color = TextSecondary, fontSize = 11.5.sp)
                    }
                    StatusPill(label = eval.status)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (eval.status == "Unauthorized Sharing") DangerBg else Color(0xFFF3F6F9))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Purchased: ${lic.purchasedSeats} license(s) · Active devices: ${lic.activeDeviceCount} · Distinct IPs: ${lic.distinctIpCount}",
                            color = if (eval.status == "Unauthorized Sharing") DangerText else TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${eval.riskSummary} Fingerprints: ${lic.activeDevicesCsv}",
                            color = if (eval.status == "Unauthorized Sharing") DangerText else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { checkInTarget = lic },
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, ControlBorder),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify / Log Seat Check-In", color = TextPrimary, fontSize = 11.sp)
                    }

                    if (lic.activeDeviceCount > 1 && lic.violationStatus != "Revoked") {
                        Button(
                            onClick = { onEnforceSinglePrimaryDevice(lic) },
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("lock_primary_${lic.vendorId}")
                        ) {
                            Icon(Icons.Filled.LockReset, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revoke Extra Devices", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (eval.status == "Unauthorized Sharing") {
                        OutlinedButton(
                            onClick = { onSendSecurityWarning(lic) },
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, ControlBorder),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Push Warning", color = TextPrimary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRevokeAndSuspendVendor(lic) },
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, DangerBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DangerBg, contentColor = DangerText),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("suspend_sharer_${lic.vendorId}")
                        ) {
                            Text("Revoke & Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    checkInTarget?.let { lic ->
        var deviceId by remember(lic.id) { mutableStateOf("term_${System.currentTimeMillis() % 1000}") }
        var extraIp by remember(lic.id) { mutableStateOf(true) }
        Dialog(onDismissRequest = { checkInTarget = null }) {
            Surface(color = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Device Heartbeat Check-In", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { checkInTarget = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Text("License: ${lic.licenseKey} (${lic.purchasedSeats} seat max)", color = TextSecondary, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("Device Hardware Fingerprint ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("New / External IP Address Detected", color = TextPrimary, fontSize = 12.sp)
                        Switch(checked = extraIp, onCheckedChange = { extraIp = it })
                    }
                    Button(
                        onClick = {
                            onRegisterDeviceCheckIn(lic, deviceId, extraIp)
                            checkInTarget = null
                        },
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Evaluate & Record Activation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
