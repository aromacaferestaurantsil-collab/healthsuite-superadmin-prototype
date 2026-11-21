package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AdminPage
import com.example.ui.theme.BrandMarkTeal
import com.example.ui.theme.BrandMint
import com.example.ui.theme.DemoBadgeMint
import com.example.ui.theme.SidebarActiveItemBg
import com.example.ui.theme.SidebarBg
import com.example.ui.theme.SidebarDivider
import com.example.ui.theme.SidebarFooterText
import com.example.ui.theme.SidebarMutedText
import com.example.ui.theme.SidebarRoleCardBg
import com.example.ui.theme.SidebarSubtitleText
import com.example.ui.theme.SidebarText

@Composable
fun SidebarDrawerContent(
    currentPage: AdminPage,
    onSelectPage: (AdminPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(256.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 20.dp)
    ) {
        // Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandMarkTeal)
            ) {
                Text(
                    text = stringResource(R.string.brand_mark),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.brand_title_prefix))
                        }
                        withStyle(SpanStyle(color = BrandMint, fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.brand_title_suffix))
                        }
                    },
                    fontSize = 16.sp
                )
                Text(
                    text = stringResource(R.string.brand_subtitle),
                    color = SidebarSubtitleText,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Super Admin Role Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SidebarRoleCardBg)
                .padding(horizontal = 11.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = stringResource(R.string.cd_role_badge),
                tint = SidebarText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.role_super_admin),
                color = SidebarText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.badge_demo),
                color = DemoBadgeMint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            AdminPage.entries.forEach { page ->
                val isSelected = page == currentPage
                val (activeIcon, inactiveIcon) = pageIcons(page)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) SidebarActiveItemBg else Color.Transparent)
                        .clickable { onSelectPage(page) }
                        .padding(horizontal = 11.dp)
                        .testTag("nav_${page.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (isSelected) activeIcon else inactiveIcon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else SidebarMutedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = page.title,
                        color = if (isSelected) Color.White else SidebarMutedText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(
            color = SidebarDivider,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.cd_lock_badge),
                tint = SidebarFooterText,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sidebar_footer_access),
                color = SidebarFooterText,
                fontSize = 11.sp
            )
        }
    }
}

private fun pageIcons(page: AdminPage): Pair<ImageVector, ImageVector> = when (page) {
    AdminPage.Overview -> Icons.Filled.Dashboard to Icons.Outlined.Dashboard
    AdminPage.Vendors -> Icons.Filled.Business to Icons.Outlined.Business
    AdminPage.Subscriptions -> Icons.Filled.CreditCard to Icons.Outlined.CreditCard
    AdminPage.Modules -> Icons.Filled.Inventory2 to Icons.Outlined.Inventory2
    AdminPage.PlatformUsers -> Icons.Filled.Group to Icons.Outlined.Group
    AdminPage.SupportDesk -> Icons.Filled.HeadsetMic to Icons.Outlined.HeadsetMic
    AdminPage.Releases -> Icons.Filled.Assignment to Icons.Outlined.Assignment
    AdminPage.Analytics -> Icons.Filled.Assessment to Icons.Outlined.Assessment
    AdminPage.AuditLog -> Icons.Filled.History to Icons.Outlined.History
    AdminPage.Settings -> Icons.Filled.Settings to Icons.Outlined.Settings
}
