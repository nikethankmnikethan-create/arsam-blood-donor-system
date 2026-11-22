package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AbuseReport
import com.example.model.AdminLog
import com.example.model.AvailabilityStatus
import com.example.model.BloodGroup
import com.example.model.BloodInventoryItem
import com.example.model.BloodRequest
import com.example.model.CertificateRecord
import com.example.model.ComponentType
import com.example.model.DonationRecord
import com.example.model.DonorProfile
import com.example.model.DonorResponse
import com.example.model.NotificationItem
import com.example.model.Organization
import com.example.model.RegionalZones
import com.example.model.RequestPriority
import com.example.model.RequestStatus
import com.example.model.RescueStage
import com.example.model.StockStatus
import com.example.model.TrustLevel
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class ArsamRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> handleListFlowError(
        flow: Flow<T>,
        path: String,
        expectedUid: String
    ): Flow<T> = flow.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            handleListFlowError(
                db.collection("users").document(userId)
                    .snapshots()
                    .map { snapshot ->
                        if (!snapshot.exists()) null
                        else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                path = path,
                expectedUid = userId
            )
        )
    }

    fun observeActiveDonors(currentUserId: String): Flow<List<DonorProfile>> = flow {
        require(currentUserId.isNotBlank())
        val path = "donors"
        emitAll(
            handleListFlowError(
                db.collection("donors")
                    .whereEqualTo("active", true)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(DonorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeCommunityRequests(currentUserId: String): Flow<List<BloodRequest>> = flow {
        require(currentUserId.isNotBlank())
        val path = "bloodRequests"
        emitAll(
            handleListFlowError(
                db.collection("bloodRequests")
                    .whereIn("status", listOf(RequestStatus.OPEN.name, RequestStatus.ESCALATED.name))
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeMyRequests(currentUserId: String): Flow<List<BloodRequest>> = flow {
        require(currentUserId.isNotBlank())
        val path = "bloodRequests"
        emitAll(
            handleListFlowError(
                db.collection("bloodRequests")
                    .whereEqualTo("requesterId", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeSafeContacts(currentUserId: String): Flow<List<DonorResponse>> = flow {
        require(currentUserId.isNotBlank())
        val path = "donorResponses"
        emitAll(
            handleListFlowError(
                db.collection("donorResponses")
                    .whereArrayContains("participantUids", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(DonorResponse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeHospitals(currentUserId: String): Flow<List<Organization>> = flow {
        require(currentUserId.isNotBlank())
        val path = "hospitals"
        emitAll(
            handleListFlowError(
                db.collection("hospitals")
                    .whereEqualTo("active", true)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(Organization::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeBloodBanks(currentUserId: String): Flow<List<Organization>> = flow {
        require(currentUserId.isNotBlank())
        val path = "bloodBanks"
        emitAll(
            handleListFlowError(
                db.collection("bloodBanks")
                    .whereEqualTo("active", true)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(Organization::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeInventory(currentUserId: String): Flow<List<BloodInventoryItem>> = flow {
        require(currentUserId.isNotBlank())
        val path = "bloodInventory"
        emitAll(
            handleListFlowError(
                db.collection("bloodInventory")
                    .whereEqualTo("active", true)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(BloodInventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeNotifications(currentUserId: String): Flow<List<NotificationItem>> = flow {
        require(currentUserId.isNotBlank())
        val path = "notifications"
        emitAll(
            handleListFlowError(
                db.collection("notifications")
                    .whereEqualTo("recipientUid", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeDonationHistory(currentUserId: String): Flow<List<DonationRecord>> = flow {
        require(currentUserId.isNotBlank())
        val path = "donationHistory"
        emitAll(
            handleListFlowError(
                db.collection("donationHistory")
                    .whereEqualTo("donorUid", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeCertificates(currentUserId: String): Flow<List<CertificateRecord>> = flow {
        require(currentUserId.isNotBlank())
        val path = "certificates"
        emitAll(
            handleListFlowError(
                db.collection("certificates")
                    .whereEqualTo("donorUid", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(CertificateRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    fun observeMyReports(currentUserId: String): Flow<List<AbuseReport>> = flow {
        require(currentUserId.isNotBlank())
        val path = "reports"
        emitAll(
            handleListFlowError(
                db.collection("reports")
                    .whereEqualTo("reporterUid", currentUserId)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(AbuseReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    },
                path = path,
                expectedUid = currentUserId
            )
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        role: UserRole,
        district: String,
        city: String,
        preferredLanguage: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            require(cleanName.length >= 2) { "Display name must be at least 2 characters." }
            val safeRole = if (role == UserRole.ADMIN) UserRole.REQUESTER else role
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "role" to safeRole.name,
                        "district" to district.trim().take(60),
                        "city" to city.trim().take(60),
                        "preferredLanguage" to preferredLanguage,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val profile = UserProfile(
                    userId = uid,
                    displayName = cleanName,
                    role = safeRole.name,
                    district = district.trim().take(60),
                    city = city.trim().take(60),
                    preferredLanguage = preferredLanguage,
                    trustLevel = TrustLevel.UNVERIFIED.name,
                    active = true
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLanguage(languageCode: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(languageCode in listOf("en", "kn", "ml")) { "Unsupported language." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to languageCode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deactivateAccount(): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            if (userRef.get().await().exists()) {
                userRef.update(
                    mapOf(
                        "active" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val donorRef = db.collection("donors").document(uid)
            if (donorRef.get().await().exists()) {
                donorRef.update(
                    mapOf(
                        "active" to false,
                        "availabilityStatus" to AvailabilityStatus.UNAVAILABLE.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveDonorProfile(
        displayName: String,
        bloodGroup: BloodGroup,
        district: String,
        city: String,
        lastDonationDate: String?,
        readinessConfirmed: Boolean,
        availabilityStatus: AvailabilityStatus,
        contactPreference: String
    ): Result<Unit> {
        var docPath = "donors/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            require(cleanName.length >= 2) { "Name must be at least 2 characters." }
            val zone = RegionalZones.findOrDefault(district)
            val expiresAt = computeStatusExpiry(availabilityStatus)
            val docRef = db.collection("donors").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any?>(
                    "displayName" to cleanName,
                    "bloodGroup" to bloodGroup.code,
                    "district" to zone.district,
                    "city" to city.ifBlank { zone.city }.trim().take(60),
                    "approxLat" to zone.approxLat,
                    "approxLng" to zone.approxLng,
                    "availabilityStatus" to availabilityStatus.name,
                    "statusExpiresAt" to expiresAt,
                    "lastDonationDate" to lastDonationDate?.trim()?.takeIf { it.isNotBlank() },
                    "readinessConfirmed" to readinessConfirmed,
                    "contactPreference" to contactPreference,
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(updates).await()
            } else {
                val token = "ARSAM-${uid.take(6).uppercase()}-${bloodGroup.code}"
                val donor = DonorProfile(
                    userId = uid,
                    displayName = cleanName,
                    bloodGroup = bloodGroup.code,
                    district = zone.district,
                    city = city.ifBlank { zone.city }.trim().take(60),
                    approxLat = zone.approxLat,
                    approxLng = zone.approxLng,
                    availabilityStatus = availabilityStatus.name,
                    statusExpiresAt = expiresAt,
                    lastDonationDate = lastDonationDate?.trim()?.takeIf { it.isNotBlank() },
                    readinessConfirmed = readinessConfirmed,
                    trustLevel = TrustLevel.UNVERIFIED.name,
                    qrToken = token,
                    contactPreference = contactPreference,
                    donationCount = 0L,
                    active = true
                )
                docRef.set(donor.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLivePulse(status: AvailabilityStatus, readinessConfirmed: Boolean): Result<Unit> {
        var docPath = "donors/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("donors").document(uid)
            docPath = docRef.path
            val expiresAt = computeStatusExpiry(status)
            val updates = mutableMapOf<String, Any?>(
                "availabilityStatus" to status.name,
                "readinessConfirmed" to readinessConfirmed,
                "statusExpiresAt" to expiresAt,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.mapValues { it.value!! }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun computeStatusExpiry(status: AvailabilityStatus): Timestamp? {
        val nowMs = System.currentTimeMillis()
        return when (status) {
            AvailabilityStatus.AVAILABLE_NOW -> Timestamp(Date(nowMs + 6 * 3600_000L))
            AvailabilityStatus.AVAILABLE_LATER -> Timestamp(Date(nowMs + 24 * 3600_000L))
            AvailabilityStatus.UNAVAILABLE -> null
        }
    }

    suspend fun createEmergencyRequest(
        requesterName: String,
        bloodGroup: BloodGroup,
        unitsNeeded: Int,
        hospitalName: String,
        district: String,
        city: String,
        requiredBy: String,
        priority: RequestPriority,
        description: String,
        matchingNearbyDonorUids: List<String>
    ): Result<String> {
        var docPath = "bloodRequests"
        return try {
            val uid = requireUserId()
            val cleanHospital = hospitalName.trim().take(120)
            val cleanDesc = description.trim().take(400)
            val cleanReqBy = requiredBy.trim().take(60)
            require(cleanHospital.length >= 2) { "Hospital name is required." }
            require(unitsNeeded in 1..20) { "Units needed must be between 1 and 20." }
            require(cleanDesc.length >= 4) { "Description must be at least 4 characters." }
            require(cleanReqBy.length >= 2) { "Required date/time is required." }

            val zone = RegionalZones.findOrDefault(district)
            val fingerprint = "${bloodGroup.code}|${zone.district.lowercase()}|${cleanHospital.lowercase()}".take(180)
            val docRef = db.collection("bloodRequests").document()
            docPath = docRef.path

            val req = BloodRequest(
                requesterId = uid,
                requesterName = requesterName.trim().ifBlank { "ARSAM Member" }.take(80),
                bloodGroup = bloodGroup.code,
                unitsNeeded = unitsNeeded.toLong(),
                hospitalName = cleanHospital,
                district = zone.district,
                city = city.ifBlank { zone.city }.trim().take(60),
                approxLat = zone.approxLat,
                approxLng = zone.approxLng,
                requiredBy = cleanReqBy,
                priority = priority.name,
                description = cleanDesc,
                status = RequestStatus.OPEN.name,
                rescueStage = RescueStage.NEARBY_BATCH.name,
                trustLevel = TrustLevel.UNVERIFIED.name,
                duplicateFingerprint = fingerprint,
                responseCount = 0L,
                flaggedSuspicious = false
            )
            docRef.set(req.toCreateMap()).await()

            // Rescue Chain Wave 1: Notify up to 5 nearby compatible donors without exposing requester PII
            matchingNearbyDonorUids.filter { it != uid }.take(5).forEach { donorUid ->
                val notifRef = db.collection("notifications").document()
                val notif = NotificationItem(
                    recipientUid = donorUid,
                    requestId = docRef.id,
                    title = "Rescue Chain Wave 1: ${bloodGroup.code} needed",
                    body = "$unitsNeeded unit(s) requested near $cleanHospital (${zone.district}). Preliminary alert only.",
                    category = "RESCUE_CHAIN",
                    readFlag = false
                )
                notifRef.set(notif.toCreateMap()).await()
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun advanceRescueChain(requestId: String, nextStage: RescueStage): Result<Unit> {
        val docPath = "bloodRequests/$requestId"
        return try {
            requireUserId()
            val newStatus = if (nextStage == RescueStage.ESCALATED) RequestStatus.ESCALATED.name else RequestStatus.OPEN.name
            db.collection("bloodRequests").document(requestId).update(
                mapOf(
                    "rescueStage" to nextStage.name,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Result<Unit> {
        val docPath = "bloodRequests/$requestId"
        return try {
            requireUserId()
            db.collection("bloodRequests").document(requestId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun requestSafeContact(
        requestId: String,
        requesterName: String,
        donor: DonorProfile,
        message: String
    ): Result<Unit> {
        var docPath = "donorResponses"
        return try {
            val uid = requireUserId()
            val cleanMsg = message.trim().take(250)
            require(cleanMsg.isNotEmpty()) { "Message cannot be empty." }
            val respRef = db.collection("donorResponses").document()
            docPath = respRef.path
            val resp = DonorResponse(
                requestId = requestId,
                requesterId = uid,
                donorId = donor.userId,
                donorDisplayName = donor.displayName,
                donorBloodGroup = donor.bloodGroup,
                requesterName = requesterName.ifBlank { "ARSAM Requester" }.take(80),
                status = "CONTACT_REQUESTED",
                donorConsented = false,
                message = cleanMsg,
                participantUids = listOf(uid, donor.userId)
            )
            respRef.set(resp.toCreateMap()).await()

            val notifRef = db.collection("notifications").document()
            notifRef.set(
                NotificationItem(
                    recipientUid = donor.userId,
                    requestId = requestId,
                    title = "New Safe Contact Request",
                    body = "A verified request for ${donor.bloodGroup} is awaiting your consent in Safe Contact.",
                    category = "SAFE_CONTACT",
                    readFlag = false
                ).toCreateMap()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun offerDonorHelp(
        request: BloodRequest,
        donorDisplayName: String,
        donorBloodGroup: String,
        message: String
    ): Result<Unit> {
        var docPath = "donorResponses"
        return try {
            val uid = requireUserId()
            require(uid != request.requesterId) { "Cannot respond to your own request." }
            val cleanMsg = message.trim().take(250)
            require(cleanMsg.isNotEmpty()) { "Message cannot be empty." }

            val batch = db.batch()
            val respRef = db.collection("donorResponses").document()
            docPath = respRef.path
            val resp = DonorResponse(
                requestId = request.id,
                requesterId = request.requesterId,
                donorId = uid,
                donorDisplayName = donorDisplayName.ifBlank { "ARSAM Donor" }.take(80),
                donorBloodGroup = donorBloodGroup,
                requesterName = request.requesterName,
                status = "DONOR_INTERESTED",
                donorConsented = false,
                message = cleanMsg,
                participantUids = listOf(request.requesterId, uid)
            )
            batch.set(respRef, resp.toCreateMap())
            val reqRef = db.collection("bloodRequests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "responseCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            db.collection("notifications").document().set(
                NotificationItem(
                    recipientUid = request.requesterId,
                    requestId = request.id,
                    title = "Donor Interested (${donorBloodGroup})",
                    body = "$donorDisplayName responded to your ${request.hospitalName} request.",
                    category = "SAFE_CONTACT",
                    readFlag = false
                ).toCreateMap()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToSafeContact(
        response: DonorResponse,
        grantConsent: Boolean,
        sharedContactNote: String?
    ): Result<Unit> {
        val docPath = "donorResponses/${response.id}"
        return try {
            val uid = requireUserId()
            check(uid == response.donorId) { "Only the donor can grant or decline Safe Contact consent." }
            val updates = mutableMapOf<String, Any?>(
                "donorConsented" to grantConsent,
                "status" to if (grantConsent) "CONSENT_GRANTED" else "DECLINED",
                "sharedContactNote" to if (grantConsent) sharedContactNote?.trim()?.take(160) else null,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.mapValues { it.value!! }
            db.collection("donorResponses").document(response.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun registerOrganization(
        name: String,
        orgType: String,
        district: String,
        city: String,
        addressLine: String
    ): Result<String> {
        val collection = if (orgType == "BLOOD_BANK") "bloodBanks" else "hospitals"
        var docPath = collection
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120)
            val cleanAddr = addressLine.trim().take(200)
            require(cleanName.length >= 2) { "Organization name must be at least 2 characters." }
            require(cleanAddr.length >= 4) { "Address / zone must be at least 4 characters." }
            val zone = RegionalZones.findOrDefault(district)
            val docRef = db.collection(collection).document()
            docPath = docRef.path
            val org = Organization(
                ownerUid = uid,
                name = cleanName,
                orgType = if (orgType == "BLOOD_BANK") "BLOOD_BANK" else "HOSPITAL",
                district = zone.district,
                city = city.ifBlank { zone.city }.trim().take(60),
                addressLine = cleanAddr,
                approxLat = zone.approxLat,
                approxLng = zone.approxLng,
                trustLevel = TrustLevel.UNVERIFIED.name,
                enquiryCount = 0L,
                active = true
            )
            docRef.set(org.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addBloodInventory(
        org: Organization,
        bloodGroup: BloodGroup,
        componentType: ComponentType,
        unitsAvailable: Int,
        expiryDateNote: String?
    ): Result<Unit> {
        var docPath = "bloodInventory"
        return try {
            val uid = requireUserId()
            require(unitsAvailable in 0..500) { "Units must be between 0 and 500." }
            val stockStatus = when {
                unitsAvailable == 0 -> StockStatus.OUT_OF_STOCK.name
                unitsAvailable <= 2 -> StockStatus.CRITICAL.name
                unitsAvailable <= 6 -> StockStatus.LOW.name
                else -> StockStatus.ADEQUATE.name
            }
            val orgCollection = if (org.orgType == "BLOOD_BANK") "bloodBanks" else "hospitals"
            val docRef = db.collection("bloodInventory").document()
            docPath = docRef.path
            val item = BloodInventoryItem(
                orgId = org.id,
                orgName = org.name,
                orgCollection = orgCollection,
                ownerUid = uid,
                bloodGroup = bloodGroup.code,
                componentType = componentType.name,
                unitsAvailable = unitsAvailable.toLong(),
                stockStatus = stockStatus,
                expiryDateNote = expiryDateNote?.trim()?.takeIf { it.isNotBlank() }?.take(60),
                district = org.district,
                active = true
            )
            docRef.set(item.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordOrganizationEnquiry(org: Organization): Result<Unit> {
        if (org.sampleRecord) return Result.success(Unit)
        val col = if (org.orgType == "BLOOD_BANK") "bloodBanks" else "hospitals"
        val docPath = "$col/${org.id}"
        return try {
            requireUserId()
            db.collection(col).document(org.id).update(
                mapOf(
                    "enquiryCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logDonationRecord(
        facilityName: String,
        district: String,
        bloodGroup: BloodGroup,
        donatedOn: String,
        notes: String?
    ): Result<Unit> {
        var docPath = "donationHistory"
        return try {
            val uid = requireUserId()
            val cleanFacility = facilityName.trim().take(120)
            require(cleanFacility.length >= 2) { "Facility name is required." }
            val recordRef = db.collection("donationHistory").document()
            docPath = recordRef.path
            val record = DonationRecord(
                donorUid = uid,
                facilityName = cleanFacility,
                district = district.trim().take(60),
                bloodGroup = bloodGroup.code,
                donatedOn = donatedOn.trim().take(30),
                notes = notes?.trim()?.takeIf { it.isNotBlank() }?.take(200)
            )
            recordRef.set(record.toCreateMap()).await()

            val certRef = db.collection("certificates").document()
            val cert = CertificateRecord(
                donorUid = uid,
                title = "Voluntary Blood Donation Appreciation",
                issuedBy = cleanFacility,
                issuedOn = donatedOn.trim().take(30),
                certificateCode = "ARSAM-CERT-${certRef.id.take(6).uppercase()}"
            )
            certRef.set(cert.toCreateMap()).await()

            val donorRef = db.collection("donors").document(uid)
            if (donorRef.get().await().exists()) {
                donorRef.update(
                    mapOf(
                        "lastDonationDate" to donatedOn.trim().take(30),
                        "donationCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitAbuseReport(targetId: String, targetType: String, reason: String): Result<Unit> {
        var docPath = "reports"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim().take(300)
            require(cleanReason.length >= 4) { "Please provide a brief reason (at least 4 characters)." }
            val reportRef = db.collection("reports").document()
            docPath = reportRef.path
            val report = AbuseReport(
                reporterUid = uid,
                targetId = targetId,
                targetType = targetType,
                reason = cleanReason,
                status = "OPEN"
            )
            reportRef.set(report.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val docPath = "notifications/$notifId"
        return try {
            requireUserId()
            db.collection("notifications").document(notifId).update(
                mapOf("readFlag" to true)
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyRequestTrustGate(
        requestId: String,
        newTrustLevel: TrustLevel,
        flagSuspicious: Boolean
    ): Result<Unit> {
        val docPath = "bloodRequests/$requestId"
        return try {
            val uid = requireUserId()
            db.collection("bloodRequests").document(requestId).update(
                mapOf(
                    "trustLevel" to newTrustLevel.name,
                    "flaggedSuspicious" to flagSuspicious,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("adminLogs").document().set(
                AdminLog(
                    adminUid = uid,
                    action = "TRUST_GATE_REQUEST_UPDATE",
                    targetId = requestId,
                    note = "Set trustLevel=${newTrustLevel.name}, flaggedSuspicious=$flagSuspicious"
                ).toCreateMap()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
