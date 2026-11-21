package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Vendor
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.InfoNoticeBg
import com.example.ui.theme.InfoNoticeText
import com.example.ui.theme.PillActiveBg
import com.example.ui.theme.PillActiveText
import com.example.ui.theme.PillPendingBg
import com.example.ui.theme.PillPendingText
import com.example.ui.theme.PillPlannedBg
import com.example.ui.theme.PillPlannedText
import com.example.ui.theme.PillSuspendedBg
import com.example.ui.theme.PillSuspendedText
import com.example.ui.theme.PrototypeBannerBg
import com.example.ui.theme.PrototypeBannerBorder
import com.example.ui.theme.PrototypeBannerText
import com.example.ui.theme.RowDivider
import com.example.ui.theme.StatFootnoteGreen
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextControl
import com.example.ui.theme.TextLinkDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextTableHeader
import com.example.ui.theme.ToastBg

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun PrototypeBanner(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PrototypeBannerBg)
            .border(1.dp, PrototypeBannerBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
            .testTag("prototype_banner")
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = stringResource(R.string.cd_prototype_notice),
            tint = PrototypeBannerText,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.banner_prototype_prefix))
                }
                append(stringResource(R.string.banner_prototype_body))
            },
            color = PrototypeBannerText,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun InfoNoticeBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(InfoNoticeBg)
            .padding(12.dp)
    ) {
        Text(
            text = text,
            color = InfoNoticeText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    footnote: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = footnote,
                color = StatFootnoteGreen,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (label) {
        "Active", "Live" -> PillActiveBg to PillActiveText
        "Pending", "Beta" -> PillPendingBg to PillPendingText
        "Suspended" -> PillSuspendedBg to PillSuspendedText
        else -> PillPlannedBg to PillPlannedText
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
        val isWideTable = maxWidth >= 540.dp
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isWideTable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "VENDOR",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2.2f)
                    )
                    Text(
                        text = "TYPE / CITY",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "PLAN",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "STATUS",
                        color = TextTableHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(74.dp))
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }

            vendors.forEach { vendor ->
                if (isWideTable) {
                    WideVendorRow(vendor = vendor, onOpenVendor = onOpenVendor)
                } else {
                    CompactVendorRow(vendor = vendor, onOpenVendor = onOpenVendor)
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun WideVendorRow(
    vendor: Vendor,
    onOpenVendor: (Vendor) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenVendor(vendor) }
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("vendor_row_${vendor.id}")
    ) {
        Column(modifier = Modifier.weight(2.2f)) {
            Text(
                text = vendor.name,
                color = TextLinkDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = vendor.id,
                color = TextSubtle,
                fontSize = 10.sp
            )
        }
        Column(modifier = Modifier.weight(1.4f)) {
            Text(
                text = vendor.type,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = vendor.city,
                color = TextSubtle,
                fontSize = 10.sp
            )
        }
        Text(
            text = vendor.plan,
            color = TextSecondary,
            fontSize = 11.5.sp,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f)) {
            StatusPill(label = vendor.status.label)
        }
        DetailsActionButton(
            vendorId = vendor.id,
            onClick = { onOpenVendor(vendor) }
        )
    }
}

@Composable
private fun CompactVendorRow(
    vendor: Vendor,
    onOpenVendor: (Vendor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenVendor(vendor) }
            .padding(vertical = 12.dp)
            .testTag("vendor_row_${vendor.id}")
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = vendor.name,
                    color = TextLinkDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${vendor.id} · ${vendor.type} · ${vendor.city}",
                    color = TextSubtle,
                    fontSize = 11.sp
                )
            }
            StatusPill(label = vendor.status.label)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Plan: ${vendor.plan}",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            DetailsActionButton(
                vendorId = vendor.id,
                onClick = { onOpenVendor(vendor) }
            )
        }
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

@Composable
fun ToastBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ToastBg,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 6.dp,
        modifier = modifier.testTag("toast_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.cd_toast_check),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
