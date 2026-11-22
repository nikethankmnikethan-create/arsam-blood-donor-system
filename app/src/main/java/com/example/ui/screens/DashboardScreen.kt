package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.ArsamStrings
import com.example.model.AvailabilityStatus
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.CertificateRecord
import com.example.model.DonationRecord
import com.example.model.DonorProfile
import com.example.model.DonorResponse
import com.example.model.Organization
import com.example.model.TrustLevel
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.DonorQrIdentityCard
import com.example.ui.components.LivePulseBadge
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.TrustGateBadge
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    userProfile: UserProfile,
    myDonorProfile: DonorProfile?,
    communityRequests: List<BloodRequest>,
    myRequests: List<BloodRequest>,
    safeContacts: List<DonorResponse>,
    myOrganizations: List<Organization>,
    donationHistory: List<DonationRecord>,
    certificates: List<CertificateRecord>,
    strings: ArsamStrings,
    onUpdateLivePulse: (AvailabilityStatus, Boolean) -> Unit,
    onRegisterDonorCard: (BloodGroup, Boolean) -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToFindDonors: () -> Unit,
    onNavigateToBanks: () -> Unit,
    onRespondSafeContact: (DonorResponse, Boolean, String?) -> Unit,
    onLogDonation: (String, String, BloodGroup, String, String?) -> Unit,
    onVerifyTrustGate: (String, TrustLevel, Boolean) -> Unit,
    onSwitchRolePreview: (UserRole) -> Unit
) {
    val activeRole = UserRole.fromName(userProfile.role)
    var showQrSheet by remember { mutableStateOf(false) }
    var showLogDonationDialog by remember { mutableStateOf(false) }
    var consentTarget by remember { mutableStateOf<DonorResponse?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Welcome, ${userProfile.displayName}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${activeRole.label} • ${userProfile.district}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TrustGateBadge(trustLevelRaw = userProfile.trustLevel)
                        }

                        Text("Switch Workspace Role", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(UserRole.DONOR, UserRole.REQUESTER, UserRole.HOSPITAL, UserRole.BLOOD_BANK).forEach { r ->
                                FilterChip(
                                    selected = r == activeRole,
                                    onClick = { onSwitchRolePreview(r) },
                                    label = { Text(r.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonSoftContainer,
                                        selectedLabelColor = OnCrimsonContainer
                                    ),
                                    modifier = Modifier.testTag("dash_role_${r.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Quick Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Open Needs",
                        value = communityRequests.size.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRequests
                    )
                    MetricTile(
                        title = "Safe Contacts",
                        value = safeContacts.size.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                    MetricTile(
                        title = "Donations",
                        value = donationHistory.size.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { showLogDonationDialog = true }
                    )
                }

                MedicalDisclaimerBanner(text = strings.medicalDisclaimer)
            }
        }

        // Donor Live Pulse & Donor Care Section
        if (activeRole == UserRole.DONOR || myDonorProfile != null) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.livePulseTitle, style = MaterialTheme.typography.titleMedium)
                                if (myDonorProfile != null) {
                                    LivePulseBadge(statusRaw = myDonorProfile.availabilityStatus)
                                }
                            }

                            if (myDonorProfile == null) {
                                var quickBg by remember { mutableStateOf(BloodGroup.O_POS) }
                                var readiness by remember { mutableStateOf(false) }
                                Text(
                                    "Activate your donor card so nearby verified requests can reach you via Safe Contact.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BloodGroup.entries.forEach { bg ->
                                        FilterChip(
                                            selected = quickBg == bg,
                                            onClick = { quickBg = bg },
                                            label = { Text(bg.code) }
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = readiness, onCheckedChange = { readiness = it })
                                    Text(
                                        "I acknowledge medical eligibility must be confirmed by blood bank staff.",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Button(
                                    onClick = { onRegisterDonorCard(quickBg, readiness) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("activate_donor_card_btn")
                                ) {
                                    Text("Activate Donor Registry Card")
                                }
                            } else {
                                var readinessChecked by remember(myDonorProfile.readinessConfirmed) {
                                    mutableStateOf(myDonorProfile.readinessConfirmed)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = readinessChecked,
                                        onCheckedChange = { readinessChecked = it }
                                    )
                                    Text(
                                        "Self-reported readiness confirmed (final screening at blood bank).",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AvailabilityStatus.entries.forEach { st ->
                                        val selected = myDonorProfile.availabilityStatus == st.name
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onUpdateLivePulse(st, readinessChecked) },
                                            label = {
                                                Text(
                                                    when (st) {
                                                        AvailabilityStatus.AVAILABLE_NOW -> strings.availableNow
                                                        AvailabilityStatus.AVAILABLE_LATER -> strings.availableLater
                                                        AvailabilityStatus.UNAVAILABLE -> strings.unavailable
                                                    }
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldContainer,
                                                selectedLabelColor = VerifiedEmerald
                                            ),
                                            modifier = Modifier.testTag("pulse_${st.name.lowercase()}")
                                        )
                                    }
                                }
                                Text(
                                    "Status auto-expires after 6h (Available Now) or 24h (Available Later) to keep registry fresh.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showQrSheet = !showQrSheet },
                                        modifier = Modifier.weight(1f).testTag("toggle_qr_btn")
                                    ) {
                                        Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(if (showQrSheet) "Hide QR ID" else "QR Donor ID")
                                    }
                                    OutlinedButton(
                                        onClick = { showLogDonationDialog = true },
                                        modifier = Modifier.weight(1f).testTag("open_log_donation_btn")
                                    ) {
                                        Icon(Icons.Outlined.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text("Log Donation")
                                    }
                                }
                                if (showQrSheet) {
                                    DonorQrIdentityCard(
                                        donorName = myDonorProfile.displayName,
                                        bloodGroup = myDonorProfile.bloodGroup,
                                        district = myDonorProfile.district,
                                        qrToken = myDonorProfile.qrToken,
                                        trustLevel = myDonorProfile.trustLevel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Safe Contact Inbox (Consent Gate)
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Text(strings.safeContactTitle, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (safeContacts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "No active Safe Contact threads yet. Contact requests appear here with donor consent controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(safeContacts.take(5), key = { it.id }) { resp ->
                Card(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${resp.donorDisplayName} (${resp.donorBloodGroup}) ↔ ${resp.requesterName}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (resp.donorConsented) EmeraldContainer else CrimsonSoftContainer
                            ) {
                                Text(
                                    text = resp.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (resp.donorConsented) VerifiedEmerald else OnCrimsonContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(resp.message, style = MaterialTheme.typography.bodyMedium)
                        if (resp.donorConsented && !resp.sharedContactNote.isNullOrBlank()) {
                            Text(
                                "Consented coordination note: ${resp.sharedContactNote}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VerifiedEmerald
                            )
                        }
                        if (resp.donorId == currentUserId && !resp.donorConsented && resp.status != "DECLINED") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { consentTarget = resp },
                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald),
                                    modifier = Modifier.testTag("grant_consent_${resp.id}")
                                ) {
                                    Text("Grant Consent")
                                }
                                OutlinedButton(
                                    onClick = { onRespondSafeContact(resp, false, null) },
                                    modifier = Modifier.testTag("decline_consent_${resp.id}")
                                ) {
                                    Text("Decline")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Requester / Hospital / Trust Gate Action Shortcuts
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Quick Coordination Actions", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToRequests,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("dash_emergency_cta")
                    ) {
                        Icon(Icons.Outlined.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Emergency")
                    }
                    OutlinedButton(
                        onClick = onNavigateToFindDonors,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("dash_discover_cta")
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Find Donors")
                    }
                }
                OutlinedButton(
                    onClick = onNavigateToBanks,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dash_banks_cta")
                ) {
                    Icon(Icons.Outlined.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Hospitals, Blood Banks & Freshness (${myOrganizations.size} managed)")
                }
            }
        }

        // Donor Care Timeline & Certificates
        if (donationHistory.isNotEmpty() || certificates.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(strings.donorCareTitle, style = MaterialTheme.typography.titleMedium)
                        donationHistory.take(3).forEach { rec ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
                                Text("${rec.donatedOn} • ${rec.facilityName} (${rec.bloodGroup})", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        certificates.take(2).forEach { cert ->
                            Text(
                                "Certificate ${cert.certificateCode} — ${cert.issuedBy} (${cert.issuedOn})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Trust Gate Review Queue Preview (with Admin action controls)
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = CrimsonPrimary)
                        Text(strings.trustGateTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Requests start as Unverified. Only authorized administrators can elevate verification levels or flag suspicious entries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    communityRequests.take(2).forEach { req ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                BloodGroupBadge(req.bloodGroup)
                                Column {
                                    Text(req.hospitalName, style = MaterialTheme.typography.labelLarge)
                                    TrustGateBadge(req.trustLevel)
                                }
                            }
                            TextButton(
                                onClick = { onVerifyTrustGate(req.id, TrustLevel.HOSPITAL_CONFIRMED, false) }
                            ) {
                                Text("Verify")
                            }
                        }
                    }
                }
            }
        }
    }

    if (consentTarget != null) {
        val target = consentTarget!!
        var sharedNote by remember { mutableStateOf("Contact me via the hospital blood bank desk between 9 AM - 6 PM.") }
        AlertDialog(
            onDismissRequest = { consentTarget = null },
            title = { Text("Grant Safe Contact Consent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Share a coordination note with ${target.requesterName}. Only share what you are comfortable sharing.")
                    OutlinedTextField(
                        value = sharedNote,
                        onValueChange = { sharedNote = it },
                        label = { Text("Coordination note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondSafeContact(target, true, sharedNote)
                        consentTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald)
                ) { Text("Confirm Consent") }
            },
            dismissButton = {
                TextButton(onClick = { consentTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showLogDonationDialog) {
        var facility by remember { mutableStateOf("") }
        var dateLabel by remember { mutableStateOf("2026-11-22") }
        var bg by remember { mutableStateOf(BloodGroup.fromCode(myDonorProfile?.bloodGroup)) }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLogDonationDialog = false },
            title = { Text("Log Voluntary Donation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = facility,
                        onValueChange = { facility = it },
                        label = { Text("Blood Bank / Hospital Name") },
                        modifier = Modifier.fillMaxWidth().testTag("log_donation_facility_input")
                    )
                    OutlinedTextField(
                        value = dateLabel,
                        onValueChange = { dateLabel = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Post-donation feedback (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogDonation(facility, userProfile.district, bg, dateLabel, notes)
                        showLogDonationDialog = false
                    },
                    enabled = facility.trim().length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_log_donation_btn")
                ) { Text("Save & Issue Certificate") }
            },
            dismissButton = {
                TextButton(onClick = { showLogDonationDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = CrimsonPrimary)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
