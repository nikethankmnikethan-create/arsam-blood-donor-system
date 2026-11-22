package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.ArsamAiRepository
import com.example.data.ArsamRepository
import com.example.data.configureAppCheck
import com.example.i18n.AppLanguage
import com.example.i18n.ArsamStringsProvider
import com.example.model.AvailabilityStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthLandingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiscoverAndMapScreen
import com.example.ui.screens.EmergencyRequestsScreen
import com.example.ui.screens.HospitalsAndBanksScreen
import com.example.ui.screens.NotificationsAndSettingsScreen
import com.example.ui.screens.RoleOnboardingScreen
import com.example.ui.theme.ArsamTheme
import com.example.ui.theme.CrimsonPrimary
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.ArsamMainViewModel
import com.example.viewmodel.ArsamUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object ArsamRoutes {
    const val DASHBOARD = "dashboard"
    const val DISCOVER = "discover"
    const val REQUESTS = "requests"
    const val BANKS = "banks"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Initialize App Check with launch Intent BEFORE setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ArsamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArsamAppRoot()
                }
            }
        }
    }
}

@Composable
fun ArsamAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var language by remember { mutableStateOf(AppLanguage.EN) }
    val strings = remember(language) { ArsamStringsProvider.forLanguage(language) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthLandingScreen(
            language = language,
            strings = strings,
            onLanguageChange = { language = it },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedArsamShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty(),
                language = language,
                onLanguageChange = { language = it },
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedArsamShell(
    currentUserId: String,
    defaultDisplayName: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val mainVm: ArsamMainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                ArsamMainViewModel(ArsamRepository(db), currentUserId)
            }
        }
    )
    val aiVm: AiAssistantViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = viewModelFactory {
            initializer { AiAssistantViewModel(ArsamAiRepository()) }
        }
    )

    val profileState by mainVm.userProfile.collectAsStateWithLifecycle()
    val donorsState by mainVm.activeDonors.collectAsStateWithLifecycle()
    val communityRequestsState by mainVm.communityRequests.collectAsStateWithLifecycle()
    val myRequestsState by mainVm.myRequests.collectAsStateWithLifecycle()
    val safeContactsState by mainVm.safeContacts.collectAsStateWithLifecycle()
    val organizations by mainVm.allOrganizations.collectAsStateWithLifecycle()
    val inventory by mainVm.displayInventory.collectAsStateWithLifecycle()
    val notificationsState by mainVm.notifications.collectAsStateWithLifecycle()
    val donationHistoryState by mainVm.donationHistory.collectAsStateWithLifecycle()
    val certificatesState by mainVm.certificates.collectAsStateWithLifecycle()
    val districtGaps by mainVm.districtGapStats.collectAsStateWithLifecycle()
    val actionFeedback by mainVm.actionFeedback.collectAsStateWithLifecycle()
    val isBusy by mainVm.isBusy.collectAsStateWithLifecycle()

    val aiMessages by aiVm.messages.collectAsStateWithLifecycle()
    val aiStreamState by aiVm.streamState.collectAsStateWithLifecycle()
    val draftCheckState by aiVm.draftCheckState.collectAsStateWithLifecycle()

    val strings = remember(language) { ArsamStringsProvider.forLanguage(language) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionFeedback) {
        val fb = actionFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(fb.message)
        mainVm.clearFeedback()
    }

    // Sync saved user language once loaded
    val loadedProfile = (profileState as? ArsamUiState.Success)?.data
    LaunchedEffect(loadedProfile?.preferredLanguage) {
        val code = loadedProfile?.preferredLanguage
        if (!code.isNullOrBlank()) {
            val parsed = AppLanguage.fromCode(code)
            if (parsed != language) onLanguageChange(parsed)
        }
    }

    when (val pState = profileState) {
        is ArsamUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CrimsonPrimary)
            }
        }
        is ArsamUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Unable to load profile: ${pState.message}",
                    color = CrimsonPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is ArsamUiState.Success -> {
            val profile = pState.data
            if (profile == null || !profile.active) {
                RoleOnboardingScreen(
                    defaultName = defaultDisplayName,
                    language = language,
                    strings = strings,
                    isBusy = isBusy,
                    onSaveOnboarding = { name, role, district, city, alsoDonor, bloodGroup ->
                        mainVm.saveUserProfile(name, role, district, city, language.code)
                        if (alsoDonor) {
                            mainVm.saveDonorProfile(
                                displayName = name,
                                bloodGroup = bloodGroup,
                                district = district,
                                city = city,
                                lastDonationDate = null,
                                readinessConfirmed = false,
                                availabilityStatus = AvailabilityStatus.AVAILABLE_LATER,
                                contactPreference = "IN_APP_CONSENT"
                            )
                        }
                    }
                )
            } else {
                ArsamNavigationScaffold(
                    currentUserId = currentUserId,
                    userProfile = profile,
                    donors = (donorsState as? ArsamUiState.Success)?.data.orEmpty(),
                    communityRequests = (communityRequestsState as? ArsamUiState.Success)?.data.orEmpty(),
                    myRequests = (myRequestsState as? ArsamUiState.Success)?.data.orEmpty(),
                    safeContacts = (safeContactsState as? ArsamUiState.Success)?.data.orEmpty(),
                    organizations = organizations,
                    inventory = inventory,
                    notifications = (notificationsState as? ArsamUiState.Success)?.data.orEmpty(),
                    donationHistory = (donationHistoryState as? ArsamUiState.Success)?.data.orEmpty(),
                    certificates = (certificatesState as? ArsamUiState.Success)?.data.orEmpty(),
                    districtGaps = districtGaps,
                    aiMessages = aiMessages,
                    aiStreamState = aiStreamState,
                    draftCheckState = draftCheckState,
                    language = language,
                    strings = strings,
                    snackbarHostState = snackbarHostState,
                    mainVm = mainVm,
                    aiVm = aiVm,
                    onLanguageChange = { newLang ->
                        onLanguageChange(newLang)
                        mainVm.updateLanguage(newLang.code)
                    },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArsamNavigationScaffold(
    currentUserId: String,
    userProfile: UserProfile,
    donors: List<com.example.model.DonorProfile>,
    communityRequests: List<com.example.model.BloodRequest>,
    myRequests: List<com.example.model.BloodRequest>,
    safeContacts: List<com.example.model.DonorResponse>,
    organizations: List<com.example.model.Organization>,
    inventory: List<com.example.model.BloodInventoryItem>,
    notifications: List<com.example.model.NotificationItem>,
    donationHistory: List<com.example.model.DonationRecord>,
    certificates: List<com.example.model.CertificateRecord>,
    districtGaps: List<com.example.model.DistrictGapStat>,
    aiMessages: List<com.example.viewmodel.AiChatMessage>,
    aiStreamState: com.example.viewmodel.AiStreamState,
    draftCheckState: ArsamUiState<com.example.data.RequestCheckReport>?,
    language: AppLanguage,
    strings: com.example.i18n.ArsamStrings,
    snackbarHostState: SnackbarHostState,
    mainVm: ArsamMainViewModel,
    aiVm: AiAssistantViewModel,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ArsamRoutes.DASHBOARD
    val unreadNotifs = remember(notifications) { notifications.count { !it.readFlag } }
    val myDonorProfile = remember(donors, currentUserId) { donors.firstOrNull { it.userId == currentUserId } }
    val myOrganizations = remember(organizations, currentUserId) { organizations.filter { it.ownerUid == currentUserId } }

    val navItems = remember(strings) {
        listOf(
            NavDestinationItem(ArsamRoutes.DASHBOARD, strings.navDashboard, Icons.Outlined.Dashboard),
            NavDestinationItem(ArsamRoutes.DISCOVER, strings.navDiscover, Icons.Outlined.PersonSearch),
            NavDestinationItem(ArsamRoutes.REQUESTS, strings.navRequests, Icons.Outlined.Emergency),
            NavDestinationItem(ArsamRoutes.BANKS, strings.navBanks, Icons.Outlined.LocalHospital),
            NavDestinationItem(ArsamRoutes.ASSISTANT, strings.navAssistant, Icons.Outlined.AutoAwesome)
        )
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(ArsamRoutes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail {
                    navItems.forEach { item ->
                        NavigationRailItem(
                            selected = currentRoute == item.route,
                            onClick = { navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = { Text("ARSAM") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = CrimsonPrimary
                        ),
                        actions = {
                            IconButton(
                                onClick = { navigateTopLevel(ArsamRoutes.SETTINGS) },
                                modifier = Modifier.testTag("top_notifications_settings_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifs > 0) {
                                            Badge { Text(unreadNotifs.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Alerts and Settings"
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ArsamRoutes.DASHBOARD,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    composable(ArsamRoutes.DASHBOARD) {
                        DashboardScreen(
                            currentUserId = currentUserId,
                            userProfile = userProfile,
                            myDonorProfile = myDonorProfile,
                            communityRequests = communityRequests,
                            myRequests = myRequests,
                            safeContacts = safeContacts,
                            myOrganizations = myOrganizations,
                            donationHistory = donationHistory,
                            certificates = certificates,
                            strings = strings,
                            onUpdateLivePulse = { st, ready -> mainVm.updateLivePulse(st, ready) },
                            onRegisterDonorCard = { bg, ready ->
                                mainVm.saveDonorProfile(
                                    displayName = userProfile.displayName,
                                    bloodGroup = bg,
                                    district = userProfile.district,
                                    city = userProfile.city,
                                    lastDonationDate = null,
                                    readinessConfirmed = ready,
                                    availabilityStatus = AvailabilityStatus.AVAILABLE_NOW,
                                    contactPreference = "IN_APP_CONSENT"
                                )
                            },
                            onNavigateToRequests = { navigateTopLevel(ArsamRoutes.REQUESTS) },
                            onNavigateToFindDonors = { navigateTopLevel(ArsamRoutes.DISCOVER) },
                            onNavigateToBanks = { navigateTopLevel(ArsamRoutes.BANKS) },
                            onRespondSafeContact = { resp, consent, note ->
                                mainVm.respondToSafeContact(resp, consent, note)
                            },
                            onLogDonation = { fac, dist, bg, date, note ->
                                mainVm.logDonation(fac, dist, bg, date, note)
                            },
                            onVerifyTrustGate = { reqId, level, flag ->
                                mainVm.verifyRequestTrustGate(reqId, level, flag)
                            },
                            onSwitchRolePreview = { role ->
                                mainVm.saveUserProfile(
                                    userProfile.displayName,
                                    role,
                                    userProfile.district,
                                    userProfile.city,
                                    language.code
                                )
                            }
                        )
                    }

                    composable(ArsamRoutes.DISCOVER) {
                        DiscoverAndMapScreen(
                            userProfile = userProfile,
                            donors = donors,
                            requests = communityRequests,
                            organizations = organizations,
                            districtGaps = districtGaps,
                            strings = strings,
                            onSendSafeContact = { reqId, donor, msg ->
                                mainVm.requestSafeContact(reqId, userProfile.displayName, donor, msg)
                            },
                            onReportDonor = { donorId, reason ->
                                mainVm.submitAbuseReport(donorId, "DONOR", reason)
                            }
                        )
                    }

                    composable(ArsamRoutes.REQUESTS) {
                        EmergencyRequestsScreen(
                            currentUserId = currentUserId,
                            userProfile = userProfile,
                            myDonorProfile = myDonorProfile,
                            communityRequests = communityRequests,
                            myRequests = myRequests,
                            draftCheckState = draftCheckState,
                            language = language,
                            strings = strings,
                            onCreateRequest = { bg, units, hosp, dist, city, reqBy, prio, desc ->
                                mainVm.createEmergencyRequest(
                                    requesterName = userProfile.displayName,
                                    bloodGroup = bg,
                                    unitsNeeded = units,
                                    hospitalName = hosp,
                                    district = dist,
                                    city = city,
                                    requiredBy = reqBy,
                                    priority = prio,
                                    description = desc
                                )
                            },
                            onCheckDraftWithAi = { bg, units, hosp, dist, reqBy, desc, dupCount ->
                                aiVm.checkDraft(bg, units, hosp, dist, reqBy, desc, dupCount, language.nativeLabel)
                            },
                            onClearDraftCheck = { aiVm.clearDraftCheck() },
                            onAdvanceRescueChain = { reqId, next -> mainVm.advanceRescueChain(reqId, next) },
                            onUpdateStatus = { reqId, st -> mainVm.updateRequestStatus(reqId, st) },
                            onOfferHelp = { req, name, bg, msg -> mainVm.offerDonorHelp(req, name, bg, msg) },
                            onReportRequest = { reqId, reason -> mainVm.submitAbuseReport(reqId, "REQUEST", reason) }
                        )
                    }

                    composable(ArsamRoutes.BANKS) {
                        HospitalsAndBanksScreen(
                            currentUserId = currentUserId,
                            userProfile = userProfile,
                            organizations = organizations,
                            inventory = inventory,
                            strings = strings,
                            onRegisterOrganization = { name, type, dist, city, addr ->
                                mainVm.registerOrganization(name, type, dist, city, addr)
                            },
                            onAddInventory = { org, bg, comp, units, expiry ->
                                mainVm.addBloodInventory(org, bg, comp, units, expiry)
                            },
                            onEnquire = { org -> mainVm.recordEnquiry(org) }
                        )
                    }

                    composable(ArsamRoutes.ASSISTANT) {
                        AiAssistantScreen(
                            messages = aiMessages,
                            streamState = aiStreamState,
                            language = language,
                            strings = strings,
                            onSendQuestion = { q -> aiVm.sendQuestion(q, language.nativeLabel) }
                        )
                    }

                    composable(ArsamRoutes.SETTINGS) {
                        NotificationsAndSettingsScreen(
                            userProfile = userProfile,
                            notifications = notifications,
                            language = language,
                            strings = strings,
                            onLanguageChange = onLanguageChange,
                            onUpdateProfile = { name, role, dist, city, langCode ->
                                mainVm.saveUserProfile(name, role, dist, city, langCode)
                            },
                            onMarkNotificationRead = { id -> mainVm.markNotificationRead(id) },
                            onDeactivateAccount = {
                                mainVm.deactivateAccount { onSignOut() }
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
