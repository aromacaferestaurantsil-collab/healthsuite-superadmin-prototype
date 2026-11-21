package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WhatsAppBridgeStatus
import com.example.security.TotpSecurityManager
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppScreen(
    bridgeUrl: String,
    bridgeToken: String,
    bridgeStatus: WhatsAppBridgeStatus,
    onSaveConfig: (url: String, token: String) -> Unit,
    onRefreshBridge: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onSendMessage: (phone: String, message: String, useDirectFallback: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlInput by rememberSaveable(bridgeUrl) { mutableStateOf(bridgeUrl) }
    var tokenInput by rememberSaveable(bridgeToken) { mutableStateOf(bridgeToken) }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var messageInput by rememberSaveable {
        mutableStateOf("HealthSuite 360 alert: Please review your tenant verification status.")
    }

    val linkQrBitmap = remember(bridgeStatus.qrPayload) {
        bridgeStatus.qrPayload?.takeIf { it.isNotBlank() }?.let { payload ->
            TotpSecurityManager.generateQrBitmap(payload, 440)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth().testTag("whatsapp_screen")
    ) {
        InfoNoticeBox(
            text = "WhatsApp Link-Device QR pairing uses an external HTTPS multi-device bridge (e.g., Baileys / whatsapp-web.js exposing /status, /qr, /pair, /send). Without a bridge, you can still dispatch messages directly via WhatsApp using wa.me links."
        )

        PanelCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp Link-Device Bridge",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusPill(label = if (bridgeStatus.isConnected) "Connected" else "Offline")
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bridgeStatus.statusMessage,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Bridge HTTPS URL", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = true,
                placeholder = { Text("https://wa-bridge.yourdomain.com", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = InputBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_bridge_url_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Bridge Bearer Token (Optional · Encrypted in Keystore)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("Bearer API token", color = TextMuted) },
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = InputBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_bridge_token_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSaveConfig(urlInput, tokenInput)
                        onRefreshBridge()
                    },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("save_whatsapp_bridge_button")
                ) {
                    Text("Save & Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRefreshBridge,
                    enabled = !bridgeStatus.isLoading,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("refresh_whatsapp_qr_button")
                ) {
                    if (bridgeStatus.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fetch Link QR", color = TextPrimary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenQrScanner,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("scan_whatsapp_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR with Camera", color = TextPrimary, fontSize = 12.sp)
                }
            }

            if (linkQrBitmap != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = CanvasBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Open WhatsApp -> Linked Devices -> Link a Device",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(
                            bitmap = linkQrBitmap.asImageBitmap(),
                            contentDescription = "WhatsApp Link Device QR Code",
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        PanelCard {
            Text(
                text = "Dispatch Vendor Notification",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Send via your linked WhatsApp bridge or launch WhatsApp directly on this device.",
                color = TextMuted,
                fontSize = 11.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Recipient Phone (with country code)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                singleLine = true,
                placeholder = { Text("+919876543210", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = InputBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Message", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = InputBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_message_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSendMessage(phoneInput, messageInput, false) },
                    enabled = bridgeUrl.isNotBlank() && bridgeStatus.isConnected,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("send_via_bridge_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send via Linked Bridge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSendMessage(phoneInput, messageInput, true) },
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ControlBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("open_whatsapp_direct_button")
                ) {
                    Text("Open in WhatsApp (wa.me)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
