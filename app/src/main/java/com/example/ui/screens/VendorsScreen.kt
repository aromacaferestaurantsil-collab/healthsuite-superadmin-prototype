package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Vendor
import com.example.ui.components.PanelCard
import com.example.ui.components.VendorTable
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextControl
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VendorsScreen(
    visibleVendors: List<Vendor>,
    searchQuery: String,
    statusFilter: String,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onExportCsv: () -> Unit,
    onOpenVendor: (Vendor) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterOptions = listOf("All", "Active", "Pending", "Suspended")
    var dropdownExpanded by remember { mutableStateOf(false) }

    PanelCard(modifier = modifier.testTag("vendors_screen")) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Vendor directory",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Review registrations and manage tenant access",
                    color = TextMuted,
                    fontSize = 11.5.sp
                )
            }
            OutlinedButton(
                onClick = onExportCsv,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, ControlBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceWhite,
                    contentColor = TextControl
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("export_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_export_csv),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 460.dp
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VendorSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatusFilterDropdown(
                        selected = statusFilter,
                        options = filterOptions,
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        onSelect = {
                            onStatusFilterChange(it)
                            dropdownExpanded = false
                        },
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VendorSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f)
                    )
                    StatusFilterDropdown(
                        selected = statusFilter,
                        options = filterOptions,
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        onSelect = {
                            onStatusFilterChange(it)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        VendorTable(
            vendors = visibleVendors,
            onOpenVendor = onOpenVendor
        )
    }
}

@Composable
private fun VendorSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                color = TextMuted,
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = RoundedCornerShape(7.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryEmerald,
            unfocusedBorderColor = ControlBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = modifier.testTag("vendor_search_input")
    )
}

@Composable
private fun StatusFilterDropdown(
    selected: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    fillWidth: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(1.dp, ControlBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextSecondary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 120.dp, minHeight = 52.dp)
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .testTag("vendor_status_filter_button")
        ) {
            Text(
                text = selected,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = { onSelect(option) },
                    modifier = Modifier.testTag("filter_option_${option.lowercase()}")
                )
            }
        }
    }
}
