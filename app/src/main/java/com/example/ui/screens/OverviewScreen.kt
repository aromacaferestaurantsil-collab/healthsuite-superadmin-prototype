package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlatformHealthService
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.model.VendorStatus
import com.example.ui.components.PanelCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.VendorTable
import com.example.ui.theme.OperationalGreen
import com.example.ui.theme.RowDivider
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OverviewScreen(
    vendors: List<Vendor>,
    tickets: List<SupportTicket>,
    platformHealth: List<PlatformHealthService>,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = listOf(
        "Total vendors" to vendors.size.toString(),
        "Active vendors" to vendors.count { it.status == VendorStatus.Active }.toString(),
        "Pending approval" to vendors.count { it.status == VendorStatus.Pending }.toString(),
        "Open tickets" to tickets.count { it.status != "Resolved" }.toString()
    )

    Column(modifier = modifier.fillMaxWidth().testTag("overview_screen")) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isFourColumns = maxWidth >= 700.dp
            if (isFourColumns) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stats.forEach { (label, value) ->
                        StatMetricCard(
                            label = label,
                            value = value,
                            footnote = "Sample data",
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
                            footnote = "Sample data",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = stats[1].first,
                            value = stats[1].second,
                            footnote = "Sample data",
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
                            footnote = "Sample data",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = stats[3].first,
                            value = stats[3].second,
                            footnote = "Sample data",
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
            text = "Platform health · simulated",
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
