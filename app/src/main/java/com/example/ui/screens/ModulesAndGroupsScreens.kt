package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ModuleGroupDoc
import com.example.model.ModuleStage
import com.example.model.PlatformModuleDoc
import com.example.ui.components.InfoNoticeBox
import com.example.ui.components.PanelCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ControlBorder
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.RowDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModulesPricingScreen(
    modules: List<PlatformModuleDoc>,
    onUpdateModulePricing: (
        moduleId: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean,
        stage: String
    ) -> Unit,
    onCreateModule: (
        name: String,
        description: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean
    ) -> Unit,
    onNavigateToCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingModule by remember { mutableStateOf<PlatformModuleDoc?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth().testTag("modules_pricing_screen")) {
        PanelCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Module Catalog & Licensing Pricing", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Edit monthly/annual prices, apply direct module offers/discounts, and mark eligibility for All-Modules licensing.",
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("add_module_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add module", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 820.dp -> 3
                maxWidth >= 520.dp -> 2
                else -> 1
            }
            val rows = modules.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { mod ->
                            PanelCard(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(22.dp))
                                    StatusPill(label = mod.stage)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(mod.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(mod.description, color = TextMuted, fontSize = 11.5.sp, minLines = 2)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${mod.currency} ${mod.effectiveMonthlyPrice().toInt()}/mo",
                                        color = PrimaryEmerald,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (mod.discountPercent > 0) {
                                        Text(
                                            text = "${mod.monthlyPrice.toInt()}",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                        StatusPill(label = "${mod.discountPercent}% OFF")
                                    }
                                }
                                Text(
                                    text = "Annual: ${mod.currency} ${mod.effectiveAnnualPrice().toInt()}/yr · ${if (mod.licenseAllEligible) "All-Modules Eligible" else "Standalone Only"}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingModule = mod },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, ControlBorder),
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("edit_module_${mod.moduleCode}")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Price / Offer", color = TextPrimary, fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = onNavigateToCheckout,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, ControlBorder),
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text("License", color = PrimaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
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

    editingModule?.let { mod ->
        EditModulePriceDialog(
            module = mod,
            onSave = { monthly, annual, discount, allEligible, stage ->
                onUpdateModulePricing(mod.id, monthly, annual, discount, allEligible, stage)
                editingModule = null
            },
            onDismiss = { editingModule = null }
        )
    }

    if (showCreateDialog) {
        CreateModuleDialog(
            onCreate = { name, desc, monthly, annual, discount, allEligible ->
                onCreateModule(name, desc, monthly, annual, discount, allEligible)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleGroupsScreen(
    modules: List<PlatformModuleDoc>,
    groups: List<ModuleGroupDoc>,
    onCreateGroup: (
        name: String,
        description: String,
        selectedModules: List<String>,
        includesAllModules: Boolean,
        bundlePrice: Double,
        discountPercent: Int,
        maxSeatsDefault: Int
    ) -> Unit,
    onBuyGroupOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var includesAllModules by remember { mutableStateOf(false) }
    var selectedModuleNames by remember { mutableStateOf(setOf<String>()) }
    var bundlePriceText by remember { mutableStateOf("3999") }
    var discountText by remember { mutableStateOf("15") }
    var seatsText by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth().testTag("module_groups_screen")) {
        InfoNoticeBox(
            text = "Create custom Module Groups for bundled licensing, or enable \"All Modules for Licensing\" to package every active HealthSuite 360 module into a single license."
        )

        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Module Licensing Group", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group / Bundle Name (e.g., Diagnostics + Pharma Pack)") },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald, unfocusedBorderColor = InputBorder),
                modifier = Modifier.fillMaxWidth().testTag("group_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = groupDesc,
                onValueChange = { groupDesc = it },
                label = { Text("Bundle description") },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald, unfocusedBorderColor = InputBorder),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Include All Modules for Licensing", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Grants access to all active & future eligible modules", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = includesAllModules,
                    onCheckedChange = { includesAllModules = it },
                    modifier = Modifier.testTag("all_modules_group_switch")
                )
            }

            if (!includesAllModules) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Modules in Group", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modules.forEach { m ->
                        val picked = m.name in selectedModuleNames
                        FilterChip(
                            selected = picked,
                            onClick = {
                                selectedModuleNames = if (picked) selectedModuleNames - m.name else selectedModuleNames + m.name
                            },
                            label = { Text(m.name, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = bundlePriceText,
                    onValueChange = { bundlePriceText = it },
                    label = { Text("Price (INR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Offer %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = seatsText,
                    onValueChange = { seatsText = it },
                    label = { Text("Seats") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    onCreateGroup(
                        groupName,
                        groupDesc,
                        selectedModuleNames.toList(),
                        includesAllModules,
                        bundlePriceText.toDoubleOrNull() ?: 0.0,
                        discountText.toIntOrNull() ?: 0,
                        seatsText.toIntOrNull() ?: 1
                    )
                    groupName = ""
                    groupDesc = ""
                    selectedModuleNames = emptySet()
                },
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("create_module_group_button")
            ) {
                Text("Save Module Licensing Group", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        PanelCard {
            Text("Configured Licensing Groups (${groups.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            groups.forEach { grp ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(grp.name, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (grp.includesAllModules) "Scope: ALL MODULES (${modules.count { it.licenseAllEligible }} modules)"
                                else "Modules: ${grp.moduleCodesCsv}",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        if (grp.includesAllModules) {
                            StatusPill(label = "All Modules")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "INR ${grp.effectiveBundlePrice().toInt()}/mo (${grp.discountPercent}% off) · ${grp.maxSeatsDefault} seats default",
                            color = PrimaryEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = onBuyGroupOnline,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ControlBorder),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Sell Online", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
                HorizontalDivider(color = RowDivider, thickness = 1.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditModulePriceDialog(
    module: PlatformModuleDoc,
    onSave: (monthly: Double, annual: Double, discount: Int, allEligible: Boolean, stage: String) -> Unit,
    onDismiss: () -> Unit
) {
    var monthlyText by remember(module.id) { mutableStateOf(module.monthlyPrice.toInt().toString()) }
    var annualText by remember(module.id) { mutableStateOf(module.annualPrice.toInt().toString()) }
    var discountText by remember(module.id) { mutableStateOf(module.discountPercent.toString()) }
    var allEligible by remember(module.id) { mutableStateOf(module.licenseAllEligible) }
    var stage by remember(module.id) { mutableStateOf(module.stage) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit ${module.name}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("Monthly Price (${module.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = annualText,
                    onValueChange = { annualText = it },
                    label = { Text("Annual Price (${module.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Offer Discount % (0-95)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Release Stage", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModuleStage.entries.forEach { s ->
                        FilterChip(selected = stage == s.label, onClick = { stage = s.label }, label = { Text(s.label) })
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Included in All-Modules License", color = TextPrimary, fontSize = 12.sp)
                    Switch(checked = allEligible, onCheckedChange = { allEligible = it })
                }
                Button(
                    onClick = {
                        onSave(
                            monthlyText.toDoubleOrNull() ?: module.monthlyPrice,
                            annualText.toDoubleOrNull() ?: module.annualPrice,
                            discountText.toIntOrNull() ?: 0,
                            allEligible,
                            stage
                        )
                    },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Save Pricing Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreateModuleDialog(
    onCreate: (name: String, desc: String, monthly: Double, annual: Double, discount: Int, allEligible: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("1499") }
    var annual by remember { mutableStateOf("14990") }
    var discount by remember { mutableStateOf("0") }
    var allEligible by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Licensable Module", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Module Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Monthly Price (INR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = annual, onValueChange = { annual = it }, label = { Text("Annual Price (INR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Initial Discount %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("All-Modules Licensing Eligible", color = TextPrimary, fontSize = 12.sp)
                    Switch(checked = allEligible, onCheckedChange = { allEligible = it })
                }
                Button(
                    onClick = {
                        onCreate(
                            name, desc,
                            monthly.toDoubleOrNull() ?: 0.0,
                            annual.toDoubleOrNull() ?: 0.0,
                            discount.toIntOrNull() ?: 0,
                            allEligible
                        )
                    },
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Create Module", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
