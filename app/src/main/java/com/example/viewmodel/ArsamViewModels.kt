package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArsamAiRepository
import com.example.data.ArsamRepository
import com.example.data.RequestCheckReport
import com.example.model.AbuseReport
import com.example.model.AvailabilityStatus
import com.example.model.BloodGroup
import com.example.model.BloodInventoryItem
import com.example.model.BloodRequest
import com.example.model.CertificateRecord
import com.example.model.ComponentType
import com.example.model.DemoSampleDirectory
import com.example.model.DistrictGapStat
import com.example.model.DonationRecord
import com.example.model.DonorProfile
import com.example.model.DonorResponse
import com.example.model.NotificationItem
import com.example.model.Organization
import com.example.model.RegionalZones
import com.example.model.RequestPriority
import com.example.model.RequestStatus
import com.example.model.RescueStage
import com.example.model.TrustLevel
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ArsamUiState<out T> {
    data object Loading : ArsamUiState<Nothing>
    data class Success<T>(val data: T) : ArsamUiState<T>
    data class Error(val message: String) : ArsamUiState<Nothing>
}

data class ActionFeedback(
    val message: String,
    val isError: Boolean = false
)

class ArsamMainViewModel(
    private val repository: ArsamRepository,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "ArsamMainVM"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<ArsamUiState<T>> =
        this.map<T, ArsamUiState<T>> { ArsamUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(ArsamUiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ArsamUiState.Loading
            )

    val userProfile: StateFlow<ArsamUiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val activeDonors: StateFlow<ArsamUiState<List<DonorProfile>>> =
        repository.observeActiveDonors(currentUserId).toUiStateFlow("donors")

    val communityRequests: StateFlow<ArsamUiState<List<BloodRequest>>> =
        repository.observeCommunityRequests(currentUserId).toUiStateFlow("communityRequests")

    val myRequests: StateFlow<ArsamUiState<List<BloodRequest>>> =
        repository.observeMyRequests(currentUserId).toUiStateFlow("myRequests")

    val safeContacts: StateFlow<ArsamUiState<List<DonorResponse>>> =
        repository.observeSafeContacts(currentUserId).toUiStateFlow("safeContacts")

    val hospitals: StateFlow<ArsamUiState<List<Organization>>> =
        repository.observeHospitals(currentUserId).toUiStateFlow("hospitals")

    val bloodBanks: StateFlow<ArsamUiState<List<Organization>>> =
        repository.observeBloodBanks(currentUserId).toUiStateFlow("bloodBanks")

    val inventory: StateFlow<ArsamUiState<List<BloodInventoryItem>>> =
        repository.observeInventory(currentUserId).toUiStateFlow("inventory")

    val notifications: StateFlow<ArsamUiState<List<NotificationItem>>> =
        repository.observeNotifications(currentUserId).toUiStateFlow("notifications")

    val donationHistory: StateFlow<ArsamUiState<List<DonationRecord>>> =
        repository.observeDonationHistory(currentUserId).toUiStateFlow("donationHistory")

    val certificates: StateFlow<ArsamUiState<List<CertificateRecord>>> =
        repository.observeCertificates(currentUserId).toUiStateFlow("certificates")

    val myReports: StateFlow<ArsamUiState<List<AbuseReport>>> =
        repository.observeMyReports(currentUserId).toUiStateFlow("reports")

    val allOrganizations: StateFlow<List<Organization>> =
        combine(hospitals, bloodBanks) { hospState, bankState ->
            val live = buildList {
                if (hospState is ArsamUiState.Success) addAll(hospState.data)
                if (bankState is ArsamUiState.Success) addAll(bankState.data)
            }
            if (live.isEmpty()) DemoSampleDirectory.sampleOrganizations else live + DemoSampleDirectory.sampleOrganizations
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DemoSampleDirectory.sampleOrganizations)

    val displayInventory: StateFlow<List<BloodInventoryItem>> =
        inventory.map { state ->
            val live = (state as? ArsamUiState.Success)?.data.orEmpty()
            if (live.isEmpty()) DemoSampleDirectory.sampleInventory else live + DemoSampleDirectory.sampleInventory
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DemoSampleDirectory.sampleInventory)

    val districtGapStats: StateFlow<List<DistrictGapStat>> =
        combine(communityRequests, activeDonors) { reqState, donorState ->
            val reqs = (reqState as? ArsamUiState.Success)?.data.orEmpty()
            val donors = (donorState as? ArsamUiState.Success)?.data.orEmpty()
            RegionalZones.presets.map { zone ->
                val openCount = reqs.count { it.district.equals(zone.district, ignoreCase = true) }
                val availCount = donors.count {
                    it.district.equals(zone.district, ignoreCase = true) &&
                        it.availabilityStatus != AvailabilityStatus.UNAVAILABLE.name
                }
                val gap = (openCount * 3) - availCount
                DistrictGapStat(
                    zone = zone,
                    openRequests = openCount,
                    availableDonors = availCount,
                    gapScore = gap
                )
            }.sortedByDescending { it.gapScore }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _actionFeedback = MutableStateFlow<ActionFeedback?>(null)
    val actionFeedback: StateFlow<ActionFeedback?> = _actionFeedback.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.onSuccess {
                _actionFeedback.value = ActionFeedback(successMessage, isError = false)
            }.onFailure { err ->
                _actionFeedback.value = ActionFeedback(err.localizedMessage ?: "Action failed", isError = true)
            }
        }
    }

    fun saveUserProfile(
        displayName: String,
        role: UserRole,
        district: String,
        city: String,
        languageCode: String
    ) = runAction("Profile saved") {
        repository.saveUserProfile(displayName, role, district, city, languageCode)
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.updateLanguage(languageCode)
        }
    }

    fun deactivateAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deactivateAccount()
            _isBusy.value = false
            res.onSuccess {
                _actionFeedback.value = ActionFeedback("Account deactivated safely.")
                onComplete()
            }.onFailure { err ->
                _actionFeedback.value = ActionFeedback(err.localizedMessage ?: "Deactivation failed", isError = true)
            }
        }
    }

    fun saveDonorProfile(
        displayName: String,
        bloodGroup: BloodGroup,
        district: String,
        city: String,
        lastDonationDate: String?,
        readinessConfirmed: Boolean,
        availabilityStatus: AvailabilityStatus,
        contactPreference: String
    ) = runAction("Donor registry card updated") {
        repository.saveDonorProfile(
            displayName,
            bloodGroup,
            district,
            city,
            lastDonationDate,
            readinessConfirmed,
            availabilityStatus,
            contactPreference
        )
    }

    fun updateLivePulse(status: AvailabilityStatus, readinessConfirmed: Boolean) =
        runAction("Live Pulse updated to ${status.label}") {
            repository.updateLivePulse(status, readinessConfirmed)
        }

    fun createEmergencyRequest(
        requesterName: String,
        bloodGroup: BloodGroup,
        unitsNeeded: Int,
        hospitalName: String,
        district: String,
        city: String,
        requiredBy: String,
        priority: RequestPriority,
        description: String
    ) = runAction("Emergency request published & Rescue Chain Wave 1 notified") {
        val donorsList = (activeDonors.value as? ArsamUiState.Success)?.data.orEmpty()
        val matchingNearby = donorsList
            .filter {
                it.bloodGroup == bloodGroup.code &&
                    it.district.equals(district, ignoreCase = true) &&
                    it.availabilityStatus != AvailabilityStatus.UNAVAILABLE.name
            }
            .map { it.userId }
        repository.createEmergencyRequest(
            requesterName = requesterName,
            bloodGroup = bloodGroup,
            unitsNeeded = unitsNeeded,
            hospitalName = hospitalName,
            district = district,
            city = city,
            requiredBy = requiredBy,
            priority = priority,
            description = description,
            matchingNearbyDonorUids = matchingNearby
        )
    }

    fun advanceRescueChain(requestId: String, nextStage: RescueStage) =
        runAction("Rescue Chain updated to ${nextStage.label}") {
            repository.advanceRescueChain(requestId, nextStage)
        }

    fun updateRequestStatus(requestId: String, status: RequestStatus) =
        runAction("Request marked ${status.label}") {
            repository.updateRequestStatus(requestId, status)
        }

    fun requestSafeContact(
        requestId: String,
        requesterName: String,
        donor: DonorProfile,
        message: String
    ) = runAction("Safe Contact request sent (awaiting donor consent)") {
        repository.requestSafeContact(requestId, requesterName, donor, message)
    }

    fun offerDonorHelp(
        request: BloodRequest,
        donorDisplayName: String,
        donorBloodGroup: String,
        message: String
    ) = runAction("Response sent via ARSAM Safe Contact") {
        repository.offerDonorHelp(request, donorDisplayName, donorBloodGroup, message)
    }

    fun respondToSafeContact(
        response: DonorResponse,
        grantConsent: Boolean,
        sharedContactNote: String?
    ) = runAction(if (grantConsent) "Safe Contact consent granted" else "Safe Contact declined") {
        repository.respondToSafeContact(response, grantConsent, sharedContactNote)
    }

    fun registerOrganization(
        name: String,
        orgType: String,
        district: String,
        city: String,
        addressLine: String
    ) = runAction("Organization registered (Unverified until Trust Gate review)") {
        repository.registerOrganization(name, orgType, district, city, addressLine)
    }

    fun addBloodInventory(
        org: Organization,
        bloodGroup: BloodGroup,
        componentType: ComponentType,
        unitsAvailable: Int,
        expiryDateNote: String?
    ) = runAction("Inventory freshness updated") {
        repository.addBloodInventory(org, bloodGroup, componentType, unitsAvailable, expiryDateNote)
    }

    fun recordEnquiry(org: Organization) =
        runAction("Availability enquiry logged with ${org.name}") {
            repository.recordOrganizationEnquiry(org)
        }

    fun logDonation(
        facilityName: String,
        district: String,
        bloodGroup: BloodGroup,
        donatedOn: String,
        notes: String?
    ) = runAction("Donation logged & appreciation certificate issued") {
        repository.logDonationRecord(facilityName, district, bloodGroup, donatedOn, notes)
    }

    fun submitAbuseReport(targetId: String, targetType: String, reason: String) =
        runAction("Report submitted for Trust Gate review") {
            repository.submitAbuseReport(targetId, targetType, reason)
        }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun verifyRequestTrustGate(requestId: String, level: TrustLevel, flagSuspicious: Boolean) =
        runAction("Trust Gate updated to ${level.label}") {
            repository.verifyRequestTrustGate(requestId, level, flagSuspicious)
        }
}

data class AiChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String
)

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val partialText: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

class AiAssistantViewModel(
    private val aiRepository: ArsamAiRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _streamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val streamState: StateFlow<AiStreamState> = _streamState.asStateFlow()

    private val _draftCheckState = MutableStateFlow<ArsamUiState<RequestCheckReport>?>(null)
    val draftCheckState: StateFlow<ArsamUiState<RequestCheckReport>?> = _draftCheckState.asStateFlow()

    fun sendQuestion(prompt: String, languageLabel: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val userMsg = AiChatMessage("u_${System.currentTimeMillis()}", fromUser = true, text = trimmed)
        _messages.value = _messages.value + userMsg
        viewModelScope.launch {
            _streamState.value = AiStreamState.Loading
            var latest = ""
            try {
                aiRepository.streamAssistantReply(trimmed, languageLabel).collect { partial ->
                    latest = partial
                    _streamState.value = AiStreamState.Streaming(partial)
                }
                if (latest.isNotBlank()) {
                    _messages.value = _messages.value + AiChatMessage(
                        "a_${System.currentTimeMillis()}",
                        fromUser = false,
                        text = latest
                    )
                    _streamState.value = AiStreamState.Idle
                } else {
                    _streamState.value = AiStreamState.Error("No response received from Gemini.")
                }
            } catch (e: TimeoutCancellationException) {
                _streamState.value = AiStreamState.Error("AI request timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiAssistantVM", "AI stream failed", e)
                _streamState.value = AiStreamState.Error(e.localizedMessage ?: "AI Assistant unavailable.")
            }
        }
    }

    fun checkDraft(
        bloodGroup: String,
        units: String,
        hospital: String,
        district: String,
        requiredBy: String,
        description: String,
        existingActiveCountSameFingerprint: Int,
        languageLabel: String
    ) {
        viewModelScope.launch {
            _draftCheckState.value = ArsamUiState.Loading
            val res = aiRepository.checkEmergencyDraft(
                bloodGroup = bloodGroup,
                units = units,
                hospital = hospital,
                district = district,
                requiredBy = requiredBy,
                description = description,
                existingActiveCountSameFingerprint = existingActiveCountSameFingerprint,
                languageLabel = languageLabel
            )
            _draftCheckState.value = res.fold(
                onSuccess = { ArsamUiState.Success(it) },
                onFailure = { ArsamUiState.Error(it.localizedMessage ?: "Draft check failed") }
            )
        }
    }

    fun clearDraftCheck() {
        _draftCheckState.value = null
    }
}
