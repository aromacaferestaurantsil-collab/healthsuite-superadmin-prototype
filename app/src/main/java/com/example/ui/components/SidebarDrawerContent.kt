package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.ui.text.style.TextOverflow
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
    userEmail: String,
    isSuperAdmin: Boolean,
    onSelectPage: (AdminPage) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SidebarRoleCardBg)
                .padding(horizontal = 11.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = stringResource(R.string.cd_role_badge),
                    tint = DemoBadgeMint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSuperAdmin) stringResource(R.string.role_super_admin) else "Workspace User",
                    color = SidebarText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isSuperAdmin) "PRIMARY" else "OWN DRIVE",
                    color = DemoBadgeMint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (userEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    color = SidebarSubtitleText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            color = SidebarDivider,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable(onClick = onSignOut)
                .padding(horizontal = 8.dp)
                .testTag("sign_out_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign out",
                tint = SidebarMutedText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sign out",
                color = SidebarText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.cd_lock_badge),
                tint = SidebarFooterText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sidebar_footer_access),
                color = SidebarFooterText,
                fontSize = 10.5.sp
            )
        }
    }
}

private fun pageIcons(page: AdminPage): Pair<ImageVector, ImageVector> = when (page) {
    AdminPage.Overview -> Icons.Filled.Dashboard to Icons.Outlined.Dashboard
    AdminPage.Vendors -> Icons.Filled.Business to Icons.Outlined.Business
    AdminPage.Modules -> Icons.Filled.Inventory2 to Icons.Outlined.Inventory2
    AdminPage.ModuleGroups -> Icons.Filled.Layers to Icons.Outlined.Layers
    AdminPage.Offers -> Icons.Filled.LocalOffer to Icons.Outlined.LocalOffer
    AdminPage.PaymentCheckout -> Icons.Filled.ShoppingCart to Icons.Outlined.ShoppingCart
    AdminPage.LicenseGuard -> Icons.Filled.Security to Icons.Outlined.Security
    AdminPage.Subscriptions -> Icons.Filled.CreditCard to Icons.Outlined.CreditCard
    AdminPage.PlatformUsers -> Icons.Filled.Group to Icons.Outlined.Group
    AdminPage.SupportDesk -> Icons.Filled.HeadsetMic to Icons.Outlined.HeadsetMic
    AdminPage.Releases -> Icons.AutoMirrored.Filled.Assignment to Icons.AutoMirrored.Outlined.Assignment
    AdminPage.Analytics -> Icons.Filled.Assessment to Icons.Outlined.Assessment
    AdminPage.AuditLog -> Icons.Filled.History to Icons.Outlined.History
    AdminPage.WhatsAppLink -> Icons.Filled.QrCode2 to Icons.Outlined.QrCode2
    AdminPage.Settings -> Icons.Filled.Settings to Icons.Outlined.Settings
}
