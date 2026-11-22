package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.i18n.AppLanguage
import com.example.i18n.ArsamStrings
import com.example.model.AvailabilityStatus
import com.example.model.BloodGroup
import com.example.model.RegionalZones
import com.example.model.UserRole
import com.example.ui.components.LanguageSwitcherRow
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.PrivacyNoticeBanner
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.OnCrimsonContainer

@Composable
fun AuthLandingScreen(
    language: AppLanguage,
    strings: ArsamStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showAboutExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF5F5), MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Bloodtype,
                                contentDescription = "ARSAM logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = CrimsonPrimary
                    )
                }
                Text(
                    text = "v1.0 Clinical",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LanguageSwitcherRow(
                selected = language,
                onSelect = onLanguageChange
            )

            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "ARSAM community blood coordination illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = strings.heroSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Primary Google Sign-In CTA
            Button(
                onClick = {
                    authError = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            authError = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = strings.signInWithGoogle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (authError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonSoftContainer,
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnCrimsonContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = { showAboutExpanded = !showAboutExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("about_arsam_toggle")
            ) {
                Text(if (showAboutExpanded) "Hide ${strings.aboutArsamTitle}" else strings.aboutArsamTitle)
            }

            AnimatedVisibility(visible = showAboutExpanded) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.aboutArsamBody, style = MaterialTheme.typography.bodyMedium)
                        FeatureRow(Icons.Outlined.Hub, "ARSAM Rescue Chain", "Progressive nearby-to-district alert waves that stop automatically when fulfilled.")
                        FeatureRow(Icons.Outlined.Lock, "ARSAM Safe Contact", "Consent-gated coordination; phone numbers and exact addresses stay private.")
                        FeatureRow(Icons.Outlined.VerifiedUser, "ARSAM Trust Gate", "Tiered verification for requests, hospitals, and blood banks with duplicate checks.")
                        FeatureRow(Icons.Outlined.Map, "Community Gap Map", "Aggregated regional demand vs. donor density to plan voluntary donation camps.")
                    }
                }
            }

            MedicalDisclaimerBanner(text = strings.medicalDisclaimer)
            PrivacyNoticeBanner(text = strings.privacyNotice)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoleOnboardingScreen(
    defaultName: String,
    language: AppLanguage,
    strings: ArsamStrings,
    isBusy: Boolean,
    onSaveOnboarding: (
        displayName: String,
        role: UserRole,
        district: String,
        city: String,
        alsoRegisterDonor: Boolean,
        bloodGroup: BloodGroup
    ) -> Unit
) {
    var displayName by remember(defaultName) { mutableStateOf(defaultName.ifBlank { "ARSAM Member" }) }
    var selectedRole by remember { mutableStateOf(UserRole.DONOR) }
    var selectedZone by remember { mutableStateOf(RegionalZones.presets.first()) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var selectedBloodGroup by remember { mutableStateOf(BloodGroup.O_POS) }
    var alsoRegisterDonor by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Complete Your ARSAM Profile", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Choose your primary coordination role and district. You can switch roles or update availability anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
            )

            Text("Select Primary Role", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(UserRole.DONOR, UserRole.REQUESTER, UserRole.HOSPITAL, UserRole.BLOOD_BANK).forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = {
                            selectedRole = role
                            if (role == UserRole.DONOR) alsoRegisterDonor = true
                        },
                        label = { Text(role.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonSoftContainer,
                            selectedLabelColor = OnCrimsonContainer
                        ),
                        modifier = Modifier.testTag("onboarding_role_${role.name.lowercase()}")
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = zoneExpanded,
                onExpandedChange = { zoneExpanded = !zoneExpanded }
            ) {
                OutlinedTextField(
                    value = "${selectedZone.district} (${selectedZone.city})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District & City") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = zoneExpanded,
                    onDismissRequest = { zoneExpanded = false }
                ) {
                    RegionalZones.presets.forEach { zone ->
                        DropdownMenuItem(
                            text = { Text("${zone.district} (${zone.city})") },
                            onClick = {
                                selectedZone = zone
                                zoneExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedRole == UserRole.DONOR || alsoRegisterDonor) {
                Text("Blood Group (for Donor Registry)", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BloodGroup.entries.forEach { bg ->
                        FilterChip(
                            selected = selectedBloodGroup == bg,
                            onClick = { selectedBloodGroup = bg },
                            label = { Text(bg.code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoftContainer,
                                selectedLabelColor = OnCrimsonContainer
                            ),
                            modifier = Modifier.testTag("onboarding_bg_${bg.code}")
                        )
                    }
                }
            }

            MedicalDisclaimerBanner(text = strings.medicalDisclaimer)

            Button(
                onClick = {
                    onSaveOnboarding(
                        displayName,
                        selectedRole,
                        selectedZone.district,
                        selectedZone.city,
                        selectedRole == UserRole.DONOR || alsoRegisterDonor,
                        selectedBloodGroup
                    )
                },
                enabled = !isBusy && displayName.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Enter ARSAM (${AvailabilityStatus.AVAILABLE_LATER.label})")
                }
            }
        }
    }
}
