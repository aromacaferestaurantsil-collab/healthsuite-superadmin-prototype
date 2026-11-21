package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.CardBorder
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
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrototypeBannerBg
import com.example.ui.theme.PrototypeBannerBorder
import com.example.ui.theme.PrototypeBannerText
import com.example.ui.theme.StatFootnoteGreen
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.ToastBg

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
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
            imageVector = Icons.Outlined.VerifiedUser,
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
            .clip(RoundedCornerShape(8.dp))
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
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (icon != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(PrototypeBannerBg)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when {
        label in setOf("Active", "Live", "Connected", "Compliant", "Paid", "Sent", "All Modules") ->
            PillActiveBg to PillActiveText
        label in setOf("Pending", "Beta", "Warning") || label.contains("days left") ->
            PillPendingBg to PillPendingText
        label in setOf("Suspended", "Offline", "Unauthorized Sharing", "Revoked", "Failed", "Expired") ->
            PillSuspendedBg to PillSuspendedText
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
