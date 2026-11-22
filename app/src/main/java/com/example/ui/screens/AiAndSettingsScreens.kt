package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.i18n.ArsamStrings
import com.example.model.NotificationItem
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.LanguageSwitcherRow
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.PrivacyNoticeBanner
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoftContainer
import com.example.ui.theme.OnCrimsonContainer
import com.example.viewmodel.AiChatMessage
import com.example.viewmodel.AiStreamState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    messages: List<AiChatMessage>,
    streamState: AiStreamState,
    language: AppLanguage,
    strings: ArsamStrings,
    onSendQuestion: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val quickPrompts = remember(language) {
        when (language) {
            AppLanguage.KN -> listOf(
                "ARSAM ರೆಸ್ಕ್ಯೂ ಚೈನ್ ಹೇಗೆ ಕೆಲಸ ಮಾಡುತ್ತದೆ?",
                "ರಕ್ತದಾನದ ಸಾಮಾನ್ಯ ಪ್ರಕ್ರಿಯೆ ಏನು?",
                "ಸೇಫ್ ಕಾಂಟ್ಯಾಕ್ಟ್ ಗೌಪ್ಯತೆ ಹೇಗೆ?"
            )
            AppLanguage.ML -> listOf(
                "ARSAM റെസ്ക്യൂ ചെയിൻ എങ്ങനെ പ്രവർത്തിക്കുന്നു?",
                "രക്തദാനത്തിന്റെ പൊതുവായ ഘട്ടങ്ങൾ എന്താണ്?",
                "സേഫ് കോൺടാക്റ്റ് സ്വകാര്യത എങ്ങനെ?"
            )
            AppLanguage.EN -> listOf(
                "How does ARSAM Rescue Chain work?",
                "What happens during voluntary blood donation?",
                "How does Safe Contact protect donor privacy?"
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = CrimsonPrimary)
                Text(strings.aiAssistantTitle, style = MaterialTheme.typography.headlineMedium)
            }

            MedicalDisclaimerBanner(
                text = "Non-medical Gemini AI Guide (${language.nativeLabel}). Never diagnoses, approves donors, or decides transfusion compatibility. Do not enter personal phone numbers or patient names."
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { q ->
                    FilterChip(
                        selected = false,
                        onClick = { onSendQuestion(q) },
                        label = { Text(q) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty() && streamState is AiStreamState.Idle) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Text(
                                "Ask anything about using ARSAM, blood donation education, or checking emergency request clarity in English, ಕನ್ನಡ, or മലയാളം.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (msg.fromUser) CrimsonPrimary else MaterialTheme.colorScheme.surface,
                            border = if (msg.fromUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.widthIn(max = 520.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.fromUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                when (streamState) {
                    is AiStreamState.Loading -> {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Gemini is preparing guidance...", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    is AiStreamState.Streaming -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = streamState.partialText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    is AiStreamState.Error -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CrimsonSoftContainer
                            ) {
                                Text(
                                    text = streamState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnCrimsonContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    is AiStreamState.Idle -> Unit
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask ARSAM AI Guide...") },
                    modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        onSendQuestion(input)
                        input = ""
                    },
                    enabled = input.trim().isNotEmpty() && streamState !is AiStreamState.Loading,
                    modifier = Modifier.size(48.dp).testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send question",
                        tint = CrimsonPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsAndSettingsScreen(
    userProfile: UserProfile,
    notifications: List<NotificationItem>,
    language: AppLanguage,
    strings: ArsamStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    onUpdateProfile: (String, UserRole, String, String, String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onDeactivateAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userProfile.displayName) { mutableStateOf(userProfile.displayName) }
    var showDeactivateConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CrimsonPrimary)
                Text(strings.notificationsTitle, style = MaterialTheme.typography.titleLarge)
            }

            if (notifications.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        "No alerts yet. Rescue Chain and Safe Contact updates appear here in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                notifications.take(6).forEach { notif ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.readFlag) MaterialTheme.colorScheme.surface else CrimsonSoftContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, style = MaterialTheme.typography.labelLarge)
                                Text(notif.body, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!notif.readFlag) {
                                TextButton(onClick = { onMarkNotificationRead(notif.id) }) {
                                    Text("Mark Read")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                    LanguageSwitcherRow(selected = language, onSelect = onLanguageChange)
                    Text(
                        strings.translationReviewNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onUpdateProfile(
                                displayName,
                                UserRole.fromName(userProfile.role),
                                userProfile.district,
                                userProfile.city,
                                language.code
                            )
                        },
                        enabled = displayName.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_settings_btn")
                    ) {
                        Text("Save Profile Changes")
                    }
                }
            }

            PrivacyNoticeBanner(text = strings.privacyNotice)

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(strings.signOutLabel)
            }

            TextButton(
                onClick = { showDeactivateConfirm = true },
                modifier = Modifier.fillMaxWidth().testTag("deactivate_account_btn")
            ) {
                Text("Deactivate ARSAM Account", color = CrimsonPrimary)
            }
        }
    }

    if (showDeactivateConfirm) {
        ConfirmationDialog(
            title = "Deactivate Account?",
            message = "Your donor card will be hidden from discovery and marked Unavailable. You can re-activate by signing in later.",
            confirmText = "Deactivate",
            onConfirm = {
                showDeactivateConfirm = false
                onDeactivateAccount()
            },
            onDismiss = { showDeactivateConfirm = false }
        )
    }
}
