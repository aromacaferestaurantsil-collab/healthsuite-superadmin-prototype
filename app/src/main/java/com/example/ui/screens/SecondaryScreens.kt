package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AnalyticsMetric
import com.example.model.AuditLogEntry
import com.example.model.PlatformModule
import com.example.model.PlatformRole
import com.example.model.PlatformSettingItem
import com.example.model.ReleaseEntry
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VendorTable
import com.example.ui.theme.CheckboxEmerald
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.OperationalGreen
import com.example.ui.theme.RowDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextControl
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle

@Composable
fun SubscriptionsScreen(
    vendors: List<Vendor>,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("subscriptions_screen")) {
        Text(
            text = "Subscription portfolio",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Illustrative subscription records",
            color = TextMuted,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        VendorTable(vendors = vendors, onOpenVendor = onOpenVendor)
    }
}

@Composable
fun ModulesScreen(
    modules: List<PlatformModule>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().testTag("modules_screen")) {
        val columns = when {
            maxWidth >= 820.dp -> 3
            maxWidth >= 520.dp -> 2
            else -> 1
        }
        val rows = modules.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { module ->
                        PanelCard(modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = module.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = module.description,
                                color = TextMuted,
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            StatusPill(label = module.stage.label)
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformUsersScreen(
    roles: List<PlatformRole>,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("platform_users_screen")) {
        Text(
            text = "Platform access directory",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Platform roles are separate from vendor tenant roles.",
            color = TextMuted,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        roles.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = item.title,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = item.role,
                    color = OperationalGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = RowDivider, thickness = 1.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        InfoNoticeBox(text = stringResource(R.string.notice_platform_users))
    }
}

@Composable
fun SupportDeskScreen(
    tickets: List<SupportTicket>,
    onResolveTicket: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("support_desk_screen")) {
        Text(
            text = "Support queue",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        tickets.forEach { ticket ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = ticket.title,
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ticket.id} · ${ticket.vendor} · ${ticket.priority}",
                        color = TextSubtle,
                        fontSize = 10.5.sp
                    )
                }
                if (ticket.status != "Resolved") {
                    OutlinedButton(
                        onClick = { onResolveTicket(ticket.id) },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ControlBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceWhite,
                            contentColor = TextControl
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("resolve_ticket_${ticket.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.action_resolve),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.status_resolved),
                        color = OperationalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = RowDivider, thickness = 1.dp)
        }
    }
}

@Composable
fun ReleasesScreen(
    releases: List<ReleaseEntry>,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("releases_screen")) {
        Text(
            text = "Release history",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        releases.forEach { release ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = release.title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = release.actionLabel,
                    color = OperationalGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = RowDivider, thickness = 1.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        InfoNoticeBox(text = stringResource(R.string.notice_releases))
    }
}

@Composable
fun AnalyticsScreen(
    metrics: List<AnalyticsMetric>,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("analytics_screen")) {
        Text(
            text = "Platform analytics",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 660.dp) 4 else 2
            val rows = metrics.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { metric ->
                            StatMetricCard(
                                label = metric.label,
                                value = metric.value,
                                footnote = metric.tag,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogScreen(
    logs: List<AuditLogEntry>,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("audit_log_screen")) {
        Text(
            text = "Session audit log",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (logs.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_audit_logs),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            logs.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = entry.message,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = entry.status,
                        color = OperationalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: List<PlatformSettingItem>,
    onToggleSetting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier.testTag("settings_screen")) {
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
                    .clickable { onToggleSetting(item.key) }
                    .padding(vertical = 6.dp)
                    .testTag("setting_row_${item.key}")
            ) {
                Checkbox(
                    checked = item.enabled,
                    onCheckedChange = { onToggleSetting(item.key) },
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
