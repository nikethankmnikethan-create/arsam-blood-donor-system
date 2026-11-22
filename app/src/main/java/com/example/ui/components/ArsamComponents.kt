package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.model.AvailabilityStatus
import com.example.model.TrustLevel
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber
import com.google.firebase.Timestamp

@Composable
fun BloodGroupBadge(
    bloodGroup: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CrimsonPrimary,
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bloodGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TrustGateBadge(
    trustLevelRaw: String,
    modifier: Modifier = Modifier
) {
    val level = TrustLevel.fromName(trustLevelRaw)
    val (bg, fg) = when (level) {
        TrustLevel.HOSPITAL_AUTHORIZED -> EmeraldContainer to VerifiedEmerald
        TrustLevel.HOSPITAL_CONFIRMED -> BlueContainer to InfoBlue
        TrustLevel.IDENTITY_VERIFIED -> AmberContainer to WarningAmber
        TrustLevel.UNVERIFIED -> SlateContainer to SlateSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        contentColor = fg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = level.label,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = level.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LivePulseBadge(
    statusRaw: String,
    modifier: Modifier = Modifier
) {
    val status = AvailabilityStatus.fromName(statusRaw)
    val (bg, fg) = when (status) {
        AvailabilityStatus.AVAILABLE_NOW -> EmeraldContainer to VerifiedEmerald
        AvailabilityStatus.AVAILABLE_LATER -> AmberContainer to WarningAmber
        AvailabilityStatus.UNAVAILABLE -> SlateContainer to SlateSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        contentColor = fg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(fg, CircleShape)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FreshnessBadge(
    updatedAt: Timestamp?,
    isSample: Boolean = false
) {
    val (label, bg, fg) = remember(updatedAt, isSample) {
        if (isSample || updatedAt == null) {
            Triple("Sample Directory", BlueContainer, InfoBlue)
        } else {
            val hours = (System.currentTimeMillis() - updatedAt.toDate().time) / 3600_000L
            when {
                hours <= 6 -> Triple("Fresh (${hours}h ago)", EmeraldContainer, VerifiedEmerald)
                hours <= 24 -> Triple("Updated ${hours}h ago", AmberContainer, WarningAmber)
                else -> Triple("Stale (${hours}h ago)", CrimsonSoftContainer, OnCrimsonContainer)
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        contentColor = fg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MedicalDisclaimerBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CrimsonSoftContainer,
        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.HealthAndSafety,
                contentDescription = "Medical safety notice",
                tint = CrimsonPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = OnCrimsonContainer
            )
        }
    }
}

@Composable
fun PrivacyNoticeBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PrivacyTip,
                contentDescription = "Privacy protection notice",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSwitcherRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppLanguage.entries.forEach { lang ->
            FilterChip(
                selected = lang == selected,
                onClick = { onSelect(lang) },
                label = { Text(lang.nativeLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrimsonSoftContainer,
                    selectedLabelColor = OnCrimsonContainer
                ),
                modifier = Modifier.testTag("lang_chip_${lang.code}")
            )
        }
    }
}

@Composable
fun DonorQrIdentityCard(
    donorName: String,
    bloodGroup: String,
    district: String,
    qrToken: String,
    trustLevel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deterministic visual QR-style identity matrix from qrToken
            Canvas(
                modifier = Modifier
                    .size(84.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                val grid = 9
                val cell = size.width / grid
                val seed = qrToken.hashCode()
                for (r in 0 until grid) {
                    for (c in 0 until grid) {
                        val isCornerFinder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                        val filled = isCornerFinder || (((seed ushr ((r * 3 + c) % 24)) xor (r * 7 + c * 13)) and 1 == 1)
                        if (filled) {
                            drawRect(
                                color = InkPrimary,
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell * 0.9f, cell * 0.9f)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("QR Donor Identity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(donorName, style = MaterialTheme.typography.titleMedium)
                Text("$bloodGroup • $district", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("ID: $qrToken", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                TrustGateBadge(trustLevelRaw = trustLevel)
            }
        }
    }
}

@Composable
fun SafeContactDialog(
    donorName: String,
    bloodGroup: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("Urgent $bloodGroup requirement nearby. Requesting consent to coordinate safely via ARSAM.") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = CrimsonPrimary) },
        title = { Text("Safe Contact: $donorName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Phone numbers remain hidden until the donor explicitly grants consent.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Coordination note (no private phone required)") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("safe_contact_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(message) },
                enabled = message.trim().length >= 4,
                modifier = Modifier.testTag("confirm_safe_contact_button")
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AbuseReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = WarningAmber) },
        title = { Text("Report: $targetLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Flag suspicious, duplicate, or abusive activity for Trust Gate review.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (min 4 characters)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("report_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason) },
                enabled = reason.trim().length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Keep Active") }
        }
    )
}
