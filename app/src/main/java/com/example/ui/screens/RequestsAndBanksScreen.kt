package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.RequestCheckReport
import com.example.i18n.AppLanguage
import com.example.i18n.ArsamStrings
import com.example.model.BloodGroup
import com.example.model.BloodInventoryItem
import com.example.model.BloodRequest
import com.example.model.ComponentType
import com.example.model.DonorProfile
import com.example.model.Organization
import com.example.model.RegionalZones
import com.example.model.RequestPriority
import com.example.model.RequestStatus
import com.example.model.RescueStage
import com.example.model.StockStatus
import com.example.model.UserProfile
import com.example.ui.components.AbuseReportDialog
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.FreshnessBadge
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.TrustGateBadge
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ArsamUiState

@Composable
fun EmergencyRequestsScreen(
    currentUserId: String,
    userProfile: UserProfile,
    myDonorProfile: DonorProfile?,
    communityRequests: List<BloodRequest>,
    myRequests: List<BloodRequest>,
    draftCheckState: ArsamUiState<RequestCheckReport>?,
    language: AppLanguage,
    strings: ArsamStrings,
    onCreateRequest: (
        bloodGroup: BloodGroup,
        units: Int,
        hospital: String,
        district: String,
        city: String,
        requiredBy: String,
        priority: RequestPriority,
        description: String
    ) -> Unit,
    onCheckDraftWithAi: (
        bloodGroup: String,
        units: String,
        hospital: String,
        district: String,
        requiredBy: String,
        description: String,
        duplicateCount: Int
    ) -> Unit,
    onClearDraftCheck: () -> Unit,
    onAdvanceRescueChain: (String, RescueStage) -> Unit,
    onUpdateStatus: (String, RequestStatus) -> Unit,
    onOfferHelp: (BloodRequest, String, String, String) -> Unit,
    onReportRequest: (String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var statusConfirmTarget by remember { mutableStateOf<Pair<BloodRequest, RequestStatus>?>(null) }
    var helpTarget by remember { mutableStateOf<BloodRequest?>(null) }
    var reportTarget by remember { mutableStateOf<BloodRequest?>(null) }

    val combined = remember(communityRequests, myRequests) {
        (myRequests + communityRequests).distinctBy { it.id }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onClearDraftCheck()
                    showCreateDialog = true
                },
                containerColor = CrimsonPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_request")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = strings.emergencyRequestCta)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Emergency Requests & Rescue Chain", style = MaterialTheme.typography.headlineMedium)
                    MedicalDisclaimerBanner(text = strings.medicalDisclaimer)
                }
            }

            if (combined.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No active emergency requests", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to publish a verified emergency blood request and activate ARSAM Rescue Chain.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(combined, key = { it.id }) { req ->
                    val isMine = req.requesterId == currentUserId
                    val isClosed = req.status == RequestStatus.FULFILLED.name || req.status == RequestStatus.CANCELLED.name
                    val stage = RescueStage.fromName(req.rescueStage)
                    Card(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BloodGroupBadge(req.bloodGroup)
                                    Column {
                                        Text(req.hospitalName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${req.unitsNeeded} unit(s) • ${req.district} • By ${req.requiredBy}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!isMine) {
                                    IconButton(onClick = { reportTarget = req }) {
                                        Icon(Icons.Outlined.Flag, contentDescription = "Report request", tint = WarningAmber)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TrustGateBadge(req.trustLevel)
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isClosed) EmeraldContainer else CrimsonSoftContainer
                                ) {
                                    Text(
                                        text = "${req.priority} • ${req.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isClosed) VerifiedEmerald else OnCrimsonContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(req.description, style = MaterialTheme.typography.bodyMedium)

                            Text(
                                "Rescue Chain: ${stage.label} • Responses: ${req.responseCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (isMine && !isClosed) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (stage != RescueStage.ESCALATED) {
                                        val nextStage = if (stage == RescueStage.NEARBY_BATCH) {
                                            RescueStage.EXPANDED_DISTRICT
                                        } else {
                                            RescueStage.ESCALATED
                                        }
                                        OutlinedButton(
                                            onClick = { onAdvanceRescueChain(req.id, nextStage) },
                                            modifier = Modifier.weight(1f).testTag("expand_rescue_${req.id}")
                                        ) {
                                            Text("Expand Wave")
                                        }
                                    }
                                    Button(
                                        onClick = { statusConfirmTarget = req to RequestStatus.FULFILLED },
                                        colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald),
                                        modifier = Modifier.weight(1f).testTag("fulfill_req_${req.id}")
                                    ) {
                                        Text("Fulfill")
                                    }
                                    TextButton(
                                        onClick = { statusConfirmTarget = req to RequestStatus.CANCELLED },
                                        modifier = Modifier.testTag("cancel_req_${req.id}")
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            } else if (!isMine && !isClosed) {
                                Button(
                                    onClick = { helpTarget = req },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("offer_help_${req.id}")
                                ) {
                                    Text("Respond via Safe Contact")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEmergencyRequestDialog(
            userProfile = userProfile,
            existingRequests = communityRequests,
            draftCheckState = draftCheckState,
            onDismiss = { showCreateDialog = false },
            onCheckWithAi = onCheckDraftWithAi,
            onSubmit = { bg, units, hosp, dist, city, reqBy, prio, desc ->
                onCreateRequest(bg, units, hosp, dist, city, reqBy, prio, desc)
                showCreateDialog = false
            }
        )
    }

    if (statusConfirmTarget != null) {
        val (req, newStatus) = statusConfirmTarget!!
        ConfirmationDialog(
            title = "${newStatus.label} Emergency Request?",
            message = "Marking ${req.hospitalName} (${req.bloodGroup}) as ${newStatus.label} stops Rescue Chain alerts permanently.",
            confirmText = "Confirm ${newStatus.label}",
            onConfirm = {
                onUpdateStatus(req.id, newStatus)
                statusConfirmTarget = null
            },
            onDismiss = { statusConfirmTarget = null }
        )
    }

    if (helpTarget != null) {
        val target = helpTarget!!
        var msg by remember { mutableStateOf("I am available to coordinate for ${target.bloodGroup} at ${target.hospitalName}.") }
        AlertDialog(
            onDismissRequest = { helpTarget = null },
            title = { Text("Respond to ${target.hospitalName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eligibility and compatibility will be verified by the blood bank staff.")
                    OutlinedTextField(
                        value = msg,
                        onValueChange = { msg = it },
                        label = { Text("Safe Contact message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onOfferHelp(
                            target,
                            userProfile.displayName,
                            myDonorProfile?.bloodGroup ?: target.bloodGroup,
                            msg
                        )
                        helpTarget = null
                    },
                    enabled = msg.trim().length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Send Response") }
            },
            dismissButton = {
                TextButton(onClick = { helpTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (reportTarget != null) {
        val target = reportTarget!!
        AbuseReportDialog(
            targetLabel = "${target.hospitalName} (${target.bloodGroup})",
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                onReportRequest(target.id, reason)
                reportTarget = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateEmergencyRequestDialog(
    userProfile: UserProfile,
    existingRequests: List<BloodRequest>,
    draftCheckState: ArsamUiState<RequestCheckReport>?,
    onDismiss: () -> Unit,
    onCheckWithAi: (String, String, String, String, String, String, Int) -> Unit,
    onSubmit: (BloodGroup, Int, String, String, String, String, RequestPriority, String) -> Unit
) {
    var selectedBg by remember { mutableStateOf(BloodGroup.O_POS) }
    var unitsText by remember { mutableStateOf("2") }
    var hospitalName by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf(RegionalZones.findOrDefault(userProfile.district)) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var requiredBy by remember { mutableStateOf("Today 18:00") }
    var priority by remember { mutableStateOf(RequestPriority.CRITICAL) }
    var description by remember { mutableStateOf("") }

    val duplicateCount = remember(selectedBg, selectedZone, hospitalName, existingRequests) {
        val fp = "${selectedBg.code}|${selectedZone.district.lowercase()}|${hospitalName.trim().lowercase()}"
        existingRequests.count { it.duplicateFingerprint == fp }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Emergency Blood Request") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Blood Group", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BloodGroup.entries.forEach { bg ->
                        FilterChip(
                            selected = selectedBg == bg,
                            onClick = { selectedBg = bg },
                            label = { Text(bg.code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoftContainer,
                                selectedLabelColor = OnCrimsonContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Units needed (1–20)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("req_units_input")
                )

                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("req_hospital_input")
                )

                ExposedDropdownMenuBox(
                    expanded = zoneExpanded,
                    onExpandedChange = { zoneExpanded = !zoneExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedZone.district} (${selectedZone.city})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = zoneExpanded,
                        onDismissRequest = { zoneExpanded = false }
                    ) {
                        RegionalZones.presets.forEach { z ->
                            DropdownMenuItem(
                                text = { Text("${z.district} (${z.city})") },
                                onClick = {
                                    selectedZone = z
                                    zoneExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = requiredBy,
                    onValueChange = { requiredBy = it },
                    label = { Text("Required Date & Time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RequestPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Non-PII request context (ward / blood bank desk)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("req_description_input")
                )

                if (duplicateCount > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = AmberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = WarningAmber)
                            Text(
                                "Duplicate Alert: $duplicateCount active request(s) already match ${selectedBg.code} at ${hospitalName.ifBlank { selectedZone.district }}.",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarningAmber
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onCheckWithAi(
                            selectedBg.code,
                            unitsText,
                            hospitalName,
                            selectedZone.district,
                            requiredBy,
                            description,
                            duplicateCount
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ai_check_draft_btn")
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Check Missing Info with Gemini AI")
                }

                when (draftCheckState) {
                    is ArsamUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    is ArsamUiState.Success -> {
                        val report = draftCheckState.data
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("AI Draft Review (Non-Medical)", style = MaterialTheme.typography.labelLarge)
                                if (report.missingFields.isNotEmpty()) {
                                    Text("Missing/Unclear: ${report.missingFields.joinToString(", ")}", style = MaterialTheme.typography.labelMedium)
                                }
                                Text(report.suspiciousOrDuplicateHint, style = MaterialTheme.typography.bodyMedium)
                                Text(report.summaryTip, style = MaterialTheme.typography.labelMedium, color = VerifiedEmerald)
                            }
                        }
                    }
                    is ArsamUiState.Error -> {
                        Text(draftCheckState.message, style = MaterialTheme.typography.labelMedium, color = CrimsonPrimary)
                    }
                    null -> Unit
                }
            }
        },
        confirmButton = {
            val unitsInt = unitsText.toIntOrNull() ?: 0
            Button(
                onClick = {
                    onSubmit(
                        selectedBg,
                        unitsInt,
                        hospitalName,
                        selectedZone.district,
                        selectedZone.city,
                        requiredBy,
                        priority,
                        description
                    )
                },
                enabled = unitsInt in 1..20 && hospitalName.trim().length >= 2 && description.trim().length >= 4 && requiredBy.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("submit_emergency_request_btn")
            ) { Text("Publish Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HospitalsAndBanksScreen(
    currentUserId: String,
    userProfile: UserProfile,
    organizations: List<Organization>,
    inventory: List<BloodInventoryItem>,
    strings: ArsamStrings,
    onRegisterOrganization: (name: String, orgType: String, district: String, city: String, addressLine: String) -> Unit,
    onAddInventory: (Organization, BloodGroup, ComponentType, Int, String?) -> Unit,
    onEnquire: (Organization) -> Unit
) {
    val context = LocalContext.current
    var showRegisterOrgDialog by remember { mutableStateOf(false) }
    var addInventoryOrg by remember { mutableStateOf<Organization?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hospitals & Blood Banks", style = MaterialTheme.typography.headlineMedium)
                    Button(
                        onClick = { showRegisterOrgDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.testTag("register_org_btn")
                    ) {
                        Text("Register Facility")
                    }
                }
                MedicalDisclaimerBanner(text = strings.stockDisclaimer)
            }
        }

        items(organizations, key = { it.id }) { org ->
            val orgStock = inventory.filter { it.orgId == org.id }
            val isOwner = org.ownerUid == currentUserId
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.LocalHospital, contentDescription = null, tint = CrimsonPrimary)
                            Column {
                                Text(org.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${org.orgType.replace('_', ' ')} • ${org.city}, ${org.district}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TrustGateBadge(org.trustLevel)
                    }

                    Text(org.addressLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)

                    if (orgStock.isNotEmpty()) {
                        Text(strings.bloodFreshnessTitle, style = MaterialTheme.typography.labelLarge)
                        orgStock.forEach { item ->
                            val status = StockStatus.fromName(item.stockStatus)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${item.bloodGroup} ${ComponentType.fromName(item.componentType).label}: ${item.unitsAvailable} units (${status.label})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FreshnessBadge(updatedAt = item.updatedAt, isSample = item.sampleRecord)
                            }
                            if (!item.expiryDateNote.isNullOrBlank()) {
                                Text(
                                    item.expiryDateNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onEnquire(org) },
                            modifier = Modifier.weight(1f).testTag("enquire_org_${org.id}")
                        ) {
                            Text("Enquire (${org.enquiryCount})")
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${org.approxLat},${org.approxLng}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Directions")
                        }
                    }

                    if (isOwner) {
                        Button(
                            onClick = { addInventoryOrg = org },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("update_stock_${org.id}")
                        ) {
                            Text("Add / Update Blood Stock")
                        }
                    }
                }
            }
        }
    }

    if (showRegisterOrgDialog) {
        var orgName by remember { mutableStateOf("") }
        var orgType by remember { mutableStateOf("BLOOD_BANK") }
        var addr by remember { mutableStateOf("") }
        val zone = RegionalZones.findOrDefault(userProfile.district)

        AlertDialog(
            onDismissRequest = { showRegisterOrgDialog = false },
            title = { Text("Register Hospital / Blood Bank") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = orgType == "HOSPITAL",
                            onClick = { orgType = "HOSPITAL" },
                            label = { Text("Hospital") }
                        )
                        FilterChip(
                            selected = orgType == "BLOOD_BANK",
                            onClick = { orgType = "BLOOD_BANK" },
                            label = { Text("Blood Bank") }
                        )
                    }
                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text("Facility Name") },
                        modifier = Modifier.fillMaxWidth().testTag("org_name_input")
                    )
                    OutlinedTextField(
                        value = addr,
                        onValueChange = { addr = it },
                        label = { Text("Area / Street Address") },
                        modifier = Modifier.fillMaxWidth().testTag("org_addr_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRegisterOrganization(orgName, orgType, zone.district, zone.city, addr)
                        showRegisterOrgDialog = false
                    },
                    enabled = orgName.trim().length >= 2 && addr.trim().length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_register_org_btn")
                ) { Text("Register") }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterOrgDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (addInventoryOrg != null) {
        val org = addInventoryOrg!!
        var bg by remember { mutableStateOf(BloodGroup.O_POS) }
        var comp by remember { mutableStateOf(ComponentType.PRBC) }
        var units by remember { mutableStateOf("10") }
        var expiryNote by remember { mutableStateOf("Checked today") }

        AlertDialog(
            onDismissRequest = { addInventoryOrg = null },
            title = { Text("Update Stock: ${org.name}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BloodGroup.entries.forEach { item ->
                            FilterChip(
                                selected = bg == item,
                                onClick = { bg = item },
                                label = { Text(item.code) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ComponentType.entries.forEach { c ->
                            FilterChip(
                                selected = comp == c,
                                onClick = { comp = c },
                                label = { Text(c.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Available units") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expiryNote,
                        onValueChange = { expiryNote = it },
                        label = { Text("Freshness / expiry note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val u = units.toIntOrNull() ?: 0
                Button(
                    onClick = {
                        onAddInventory(org, bg, comp, u, expiryNote)
                        addInventoryOrg = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save Stock") }
            },
            dismissButton = {
                TextButton(onClick = { addInventoryOrg = null }) { Text("Cancel") }
            }
        )
    }
}
