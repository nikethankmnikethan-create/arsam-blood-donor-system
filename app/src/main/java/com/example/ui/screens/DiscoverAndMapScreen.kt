package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.i18n.ArsamStrings
import com.example.model.AvailabilityStatus
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DistrictGapStat
import com.example.model.DonorProfile
import com.example.model.Organization
import com.example.model.RegionalZones
import com.example.model.TrustLevel
import com.example.model.UserProfile
import com.example.ui.components.AbuseReportDialog
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.LivePulseBadge
import com.example.ui.components.PrivacyNoticeBanner
import com.example.ui.components.SafeContactDialog
import com.example.ui.components.TrustGateBadge
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.EmergencyCoral
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverAndMapScreen(
    userProfile: UserProfile,
    donors: List<DonorProfile>,
    requests: List<BloodRequest>,
    organizations: List<Organization>,
    districtGaps: List<DistrictGapStat>,
    strings: ArsamStrings,
    onSendSafeContact: (requestId: String, donor: DonorProfile, message: String) -> Unit,
    onReportDonor: (donorId: String, reason: String) -> Unit
) {
    val context = LocalContext.current
    var showMapMode by remember { mutableStateOf(false) }
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var availableNowOnly by remember { mutableStateOf(false) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }

    val userZone = remember(userProfile.district) { RegionalZones.findOrDefault(userProfile.district) }
    var userLatLng by remember(userZone) { mutableStateOf(userZone.approxLat to userZone.approxLng) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showLocationConsentDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            // Round to 2 decimal places (~1.1 km) to protect privacy
                            val rLat = kotlin.math.round(loc.latitude * 100.0) / 100.0
                            val rLng = kotlin.math.round(loc.longitude * 100.0) / 100.0
                            userLatLng = rLat to rLng
                        }
                    }
            } catch (_: SecurityException) {
            }
        }
    }

    var contactDonor by remember { mutableStateOf<DonorProfile?>(null) }
    var reportDonor by remember { mutableStateOf<DonorProfile?>(null) }

    val filteredDonors = remember(donors, selectedBloodGroup, availableNowOnly, verifiedOnly, selectedDistrict) {
        donors.filter { d ->
            (selectedBloodGroup == null || d.bloodGroup == selectedBloodGroup) &&
                (!availableNowOnly || d.availabilityStatus == AvailabilityStatus.AVAILABLE_NOW.name) &&
                (!verifiedOnly || d.trustLevel != TrustLevel.UNVERIFIED.name) &&
                (selectedDistrict == null || d.district.equals(selectedDistrict, ignoreCase = true))
        }
    }

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
                    Text(strings.findDonorsTitle, style = MaterialTheme.typography.headlineMedium)
                    OutlinedButton(
                        onClick = { showLocationConsentDialog = true },
                        modifier = Modifier.testTag("location_consent_btn")
                    ) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(if (locationGranted) "Approx GPS On" else "Use Location")
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !showMapMode,
                        onClick = { showMapMode = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("tab_donor_list")
                    ) { Text("Donor Directory (${filteredDonors.size})") }
                    SegmentedButton(
                        selected = showMapMode,
                        onClick = { showMapMode = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("tab_gap_map")
                    ) { Text("Zone & Gap Map") }
                }

                PrivacyNoticeBanner(text = strings.privacyNotice)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedBloodGroup == null,
                        onClick = { selectedBloodGroup = null },
                        label = { Text("All Groups") }
                    )
                    BloodGroup.entries.forEach { bg ->
                        FilterChip(
                            selected = selectedBloodGroup == bg.code,
                            onClick = { selectedBloodGroup = if (selectedBloodGroup == bg.code) null else bg.code },
                            label = { Text(bg.code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoftContainer,
                                selectedLabelColor = OnCrimsonContainer
                            )
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = availableNowOnly,
                        onClick = { availableNowOnly = !availableNowOnly },
                        label = { Text("Available Now") }
                    )
                    FilterChip(
                        selected = verifiedOnly,
                        onClick = { verifiedOnly = !verifiedOnly },
                        label = { Text("Trust Gate Verified") },
                        leadingIcon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedDistrict != null,
                        onClick = {
                            selectedDistrict = if (selectedDistrict == null) userProfile.district else null
                        },
                        label = { Text(selectedDistrict ?: "All Districts") }
                    )
                }
            }
        }

        if (showMapMode) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RegionalInteractiveMapCanvas(
                        donors = filteredDonors,
                        organizations = organizations,
                        requests = requests,
                        districtGaps = districtGaps
                    )

                    Text(strings.mapGapTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Aggregated regional demand vs. registered available donors. No personal patient or donor identities are shown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    districtGaps.take(4).forEach { stat ->
                        val recommendCamp = stat.gapScore > 0 || stat.availableDonors <= 1
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${stat.zone.district} (${stat.zone.city})", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Open Requests: ${stat.openRequests} • Available Donors: ${stat.availableDonors}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (recommendCamp) AmberContainer else MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = if (recommendCamp) "Camp Priority" else "Balanced",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (recommendCamp) WarningAmber else VerifiedEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text("Directions to Nearby Hospitals & Blood Banks", style = MaterialTheme.typography.titleMedium)
                    organizations.take(4).forEach { org ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(org.name, style = MaterialTheme.typography.labelLarge)
                                    Text("${org.district} • ${org.addressLine}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${org.approxLat},${org.approxLng}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                ) {
                                    Icon(Icons.Outlined.Directions, contentDescription = "Directions", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Navigate")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredDonors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(36.dp))
                            Text("No matching donors found in this filter", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Try clearing filters or submitting an Emergency Request to trigger ARSAM Rescue Chain waves.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredDonors, key = { it.id }) { donor ->
                    val approxDist = RegionalZones.approxDistanceKm(
                        userLatLng.first,
                        userLatLng.second,
                        donor.approxLat,
                        donor.approxLng
                    )
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BloodGroupBadge(donor.bloodGroup)
                                    Column {
                                        Text(donor.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${donor.city}, ${donor.district} • ~$approxDist km zone",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { reportDonor = donor }) {
                                    Icon(Icons.Outlined.Flag, contentDescription = "Report donor card", tint = WarningAmber)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LivePulseBadge(donor.availabilityStatus)
                                TrustGateBadge(donor.trustLevel)
                            }
                            Button(
                                onClick = { contactDonor = donor },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("safe_contact_btn_${donor.id}")
                            ) {
                                Text("Request Safe Contact")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocationConsentDialog) {
        AlertDialog(
            onDismissRequest = { showLocationConsentDialog = false },
            title = { Text("Consent-Based Approximate Location") },
            text = {
                Text(
                    "ARSAM uses approximate location only to estimate zone distance to hospitals and donor clusters. Exact residential coordinates are rounded and never published."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationConsentDialog = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Allow Approximate Location") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationConsentDialog = false }) { Text("Not Now") }
            }
        )
    }

    if (contactDonor != null) {
        val target = contactDonor!!
        val activeReqId = requests.firstOrNull { it.requesterId == userProfile.userId }?.id
            ?: requests.firstOrNull()?.id
            ?: ""
        if (activeReqId.isBlank()) {
            AlertDialog(
                onDismissRequest = { contactDonor = null },
                title = { Text("Create an Emergency Request First") },
                text = { Text("ARSAM Safe Contact links donor requests to an active emergency request for Trust Gate accountability.") },
                confirmButton = {
                    Button(onClick = { contactDonor = null }) { Text("OK") }
                }
            )
        } else {
            SafeContactDialog(
                donorName = target.displayName,
                bloodGroup = target.bloodGroup,
                onDismiss = { contactDonor = null },
                onSend = { msg ->
                    onSendSafeContact(activeReqId, target, msg)
                    contactDonor = null
                }
            )
        }
    }

    if (reportDonor != null) {
        val target = reportDonor!!
        AbuseReportDialog(
            targetLabel = target.displayName,
            onDismiss = { reportDonor = null },
            onSubmit = { reason ->
                onReportDonor(target.id, reason)
                reportDonor = null
            }
        )
    }
}

@Composable
private fun RegionalInteractiveMapCanvas(
    donors: List<DonorProfile>,
    organizations: List<Organization>,
    requests: List<BloodRequest>,
    districtGaps: List<DistrictGapStat>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Regional Live Zone & Gap Map (Offline-Safe Fallback Canvas)",
                style = MaterialTheme.typography.titleMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val minLat = 8.0
                    val maxLat = 16.0
                    val minLng = 74.0
                    val maxLng = 78.5

                    fun project(lat: Double, lng: Double): Offset {
                        val xNorm = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
                        val yNorm = (1.0 - (lat - minLat) / (maxLat - minLat)).toFloat().coerceIn(0.05f, 0.95f)
                        return Offset(xNorm * size.width, yNorm * size.height)
                    }

                    // Subtle grid
                    val stepX = size.width / 4f
                    val stepY = size.height / 4f
                    for (i in 1..3) {
                        drawLine(Color(0xFFCBD5E1), Offset(stepX * i, 0f), Offset(stepX * i, size.height), strokeWidth = 1f)
                        drawLine(Color(0xFFCBD5E1), Offset(0f, stepY * i), Offset(size.width, stepY * i), strokeWidth = 1f)
                    }

                    // Gap heat circles
                    districtGaps.forEach { gap ->
                        val p = project(gap.zone.approxLat, gap.zone.approxLng)
                        if (gap.openRequests > 0) {
                            drawCircle(
                                color = EmergencyCoral.copy(alpha = 0.16f),
                                radius = (26f + gap.openRequests * 8f).coerceAtMost(60f),
                                center = p
                            )
                        }
                    }

                    // Donor zone rings (approximate only)
                    donors.forEach { d ->
                        val p = project(d.approxLat, d.approxLng)
                        drawCircle(
                            color = CrimsonPrimary.copy(alpha = 0.25f),
                            radius = 18f,
                            center = p,
                            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                        )
                        drawCircle(color = CrimsonPrimary, radius = 6f, center = p)
                    }

                    // Hospitals & Blood Banks
                    organizations.forEach { org ->
                        val p = project(org.approxLat, org.approxLng)
                        drawCircle(color = InfoBlue, radius = 8f, center = p)
                    }

                    // Emergency Requests
                    requests.forEach { req ->
                        val p = project(req.approxLat, req.approxLng)
                        drawCircle(color = EmergencyCoral, radius = 9f, center = p)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MapLegendDot(CrimsonPrimary, "Donor Zone")
                MapLegendDot(InfoBlue, "Hospital / Bank")
                MapLegendDot(EmergencyCoral, "Emergency Need")
            }
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(999.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
