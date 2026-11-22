package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class UserRole(val label: String) {
    DONOR("Blood Donor"),
    REQUESTER("Blood Requester"),
    HOSPITAL("Hospital"),
    BLOOD_BANK("Blood Bank"),
    ADMIN("Administrator");

    companion object {
        fun fromName(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: DONOR
    }
}

enum class BloodGroup(val code: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    companion object {
        fun fromCode(raw: String?): BloodGroup =
            entries.firstOrNull { it.code == raw } ?: O_POS
    }
}

enum class TrustLevel(val label: String) {
    UNVERIFIED("Unverified"),
    IDENTITY_VERIFIED("Identity Verified"),
    HOSPITAL_CONFIRMED("Hospital Confirmed"),
    HOSPITAL_AUTHORIZED("Hospital Authorized");

    companion object {
        fun fromName(raw: String?): TrustLevel =
            entries.firstOrNull { it.name == raw } ?: UNVERIFIED
    }
}

enum class AvailabilityStatus(val label: String) {
    AVAILABLE_NOW("Available Now"),
    AVAILABLE_LATER("Available Later"),
    UNAVAILABLE("Unavailable");

    companion object {
        fun fromName(raw: String?): AvailabilityStatus =
            entries.firstOrNull { it.name == raw } ?: UNAVAILABLE
    }
}

enum class RequestPriority(val label: String) {
    CRITICAL("Critical"),
    HIGH("High"),
    STANDARD("Standard");

    companion object {
        fun fromName(raw: String?): RequestPriority =
            entries.firstOrNull { it.name == raw } ?: HIGH
    }
}

enum class RequestStatus(val label: String) {
    OPEN("Open"),
    ESCALATED("Escalated"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    companion object {
        fun fromName(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class RescueStage(val label: String) {
    NEARBY_BATCH("Wave 1: Nearby Batch"),
    EXPANDED_DISTRICT("Wave 2: Expanded District"),
    ESCALATED("Wave 3: Escalated Coordination");

    companion object {
        fun fromName(raw: String?): RescueStage =
            entries.firstOrNull { it.name == raw } ?: NEARBY_BATCH
    }
}

enum class ComponentType(val label: String) {
    WHOLE_BLOOD("Whole Blood"),
    PRBC("Packed RBC (PRBC)"),
    PLATELETS("Platelets"),
    PLASMA("Fresh Frozen Plasma");

    companion object {
        fun fromName(raw: String?): ComponentType =
            entries.firstOrNull { it.name == raw } ?: WHOLE_BLOOD
    }
}

enum class StockStatus(val label: String) {
    ADEQUATE("Adequate"),
    LOW("Low"),
    CRITICAL("Critical"),
    OUT_OF_STOCK("Out of Stock");

    companion object {
        fun fromName(raw: String?): StockStatus =
            entries.firstOrNull { it.name == raw } ?: ADEQUATE
    }
}

data class DistrictZone(
    val district: String,
    val city: String,
    val approxLat: Double,
    val approxLng: Double
)

object RegionalZones {
    val presets = listOf(
        DistrictZone("Bengaluru Urban", "Bengaluru", 12.97, 77.59),
        DistrictZone("Mysuru", "Mysuru", 12.30, 76.65),
        DistrictZone("Dakshina Kannada", "Mangaluru", 12.91, 74.85),
        DistrictZone("Ernakulam", "Kochi", 9.93, 76.26),
        DistrictZone("Thiruvananthapuram", "Thiruvananthapuram", 8.52, 76.93),
        DistrictZone("Kozhikode", "Kozhikode", 11.25, 75.78),
        DistrictZone("Dharwad", "Hubballi", 15.36, 75.12),
        DistrictZone("Thrissur", "Thrissur", 10.52, 76.21)
    )

    fun findOrDefault(district: String): DistrictZone =
        presets.firstOrNull { it.district.equals(district, ignoreCase = true) } ?: presets.first()

    fun approxDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt().coerceAtLeast(1)
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.DONOR.name,
    val district: String = "Bengaluru Urban",
    val city: String = "Bengaluru",
    val preferredLanguage: String = "en",
    val trustLevel: String = TrustLevel.UNVERIFIED.name,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "role" to role,
        "district" to district,
        "city" to city,
        "preferredLanguage" to preferredLanguage,
        "trustLevel" to trustLevel,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DonorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val bloodGroup: String = BloodGroup.O_POS.code,
    val district: String = "Bengaluru Urban",
    val city: String = "Bengaluru",
    val approxLat: Double = 12.97,
    val approxLng: Double = 77.59,
    val availabilityStatus: String = AvailabilityStatus.AVAILABLE_NOW.name,
    val statusExpiresAt: Timestamp? = null,
    val lastDonationDate: String? = null,
    val readinessConfirmed: Boolean = false,
    val trustLevel: String = TrustLevel.UNVERIFIED.name,
    val qrToken: String = "",
    val contactPreference: String = "IN_APP_CONSENT",
    val donationCount: Long = 0L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "bloodGroup" to bloodGroup,
        "district" to district,
        "city" to city,
        "approxLat" to approxLat,
        "approxLng" to approxLng,
        "availabilityStatus" to availabilityStatus,
        "statusExpiresAt" to statusExpiresAt,
        "lastDonationDate" to lastDonationDate,
        "readinessConfirmed" to readinessConfirmed,
        "trustLevel" to trustLevel,
        "qrToken" to qrToken,
        "contactPreference" to contactPreference,
        "donationCount" to donationCount,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val bloodGroup: String = BloodGroup.O_POS.code,
    val unitsNeeded: Long = 1L,
    val hospitalName: String = "",
    val district: String = "Bengaluru Urban",
    val city: String = "Bengaluru",
    val approxLat: Double = 12.97,
    val approxLng: Double = 77.59,
    val requiredBy: String = "",
    val priority: String = RequestPriority.HIGH.name,
    val description: String = "",
    val status: String = RequestStatus.OPEN.name,
    val rescueStage: String = RescueStage.NEARBY_BATCH.name,
    val trustLevel: String = TrustLevel.UNVERIFIED.name,
    val duplicateFingerprint: String = "",
    val responseCount: Long = 0L,
    val flaggedSuspicious: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "requesterId" to requesterId,
        "requesterName" to requesterName,
        "bloodGroup" to bloodGroup,
        "unitsNeeded" to unitsNeeded,
        "hospitalName" to hospitalName,
        "district" to district,
        "city" to city,
        "approxLat" to approxLat,
        "approxLng" to approxLng,
        "requiredBy" to requiredBy,
        "priority" to priority,
        "description" to description,
        "status" to status,
        "rescueStage" to rescueStage,
        "trustLevel" to trustLevel,
        "duplicateFingerprint" to duplicateFingerprint,
        "responseCount" to responseCount,
        "flaggedSuspicious" to flaggedSuspicious,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DonorResponse(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val requesterId: String = "",
    val donorId: String = "",
    val donorDisplayName: String = "",
    val donorBloodGroup: String = BloodGroup.O_POS.code,
    val requesterName: String = "",
    val status: String = "CONTACT_REQUESTED",
    val donorConsented: Boolean = false,
    val sharedContactNote: String? = null,
    val message: String = "",
    val participantUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "requestId" to requestId,
        "requesterId" to requesterId,
        "donorId" to donorId,
        "donorDisplayName" to donorDisplayName,
        "donorBloodGroup" to donorBloodGroup,
        "requesterName" to requesterName,
        "status" to status,
        "donorConsented" to donorConsented,
        "sharedContactNote" to sharedContactNote,
        "message" to message,
        "participantUids" to participantUids,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Organization(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val orgType: String = "HOSPITAL",
    val district: String = "Bengaluru Urban",
    val city: String = "Bengaluru",
    val addressLine: String = "",
    val approxLat: Double = 12.97,
    val approxLng: Double = 77.59,
    val trustLevel: String = TrustLevel.UNVERIFIED.name,
    val enquiryCount: Long = 0L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val sampleRecord: Boolean = false
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "name" to name,
        "orgType" to orgType,
        "district" to district,
        "city" to city,
        "addressLine" to addressLine,
        "approxLat" to approxLat,
        "approxLng" to approxLng,
        "trustLevel" to trustLevel,
        "enquiryCount" to enquiryCount,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BloodInventoryItem(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val orgName: String = "",
    val orgCollection: String = "bloodBanks",
    val ownerUid: String = "",
    val bloodGroup: String = BloodGroup.O_POS.code,
    val componentType: String = ComponentType.WHOLE_BLOOD.name,
    val unitsAvailable: Long = 0L,
    val stockStatus: String = StockStatus.ADEQUATE.name,
    val expiryDateNote: String? = null,
    val district: String = "Bengaluru Urban",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val sampleRecord: Boolean = false
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "orgId" to orgId,
        "orgName" to orgName,
        "orgCollection" to orgCollection,
        "ownerUid" to ownerUid,
        "bloodGroup" to bloodGroup,
        "componentType" to componentType,
        "unitsAvailable" to unitsAvailable,
        "stockStatus" to stockStatus,
        "expiryDateNote" to expiryDateNote,
        "district" to district,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val requestId: String? = null,
    val title: String = "",
    val body: String = "",
    val category: String = "RESCUE_CHAIN",
    val readFlag: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "recipientUid" to recipientUid,
        "requestId" to requestId,
        "title" to title,
        "body" to body,
        "category" to category,
        "readFlag" to readFlag,
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class DonationRecord(
    @DocumentId val id: String = "",
    val donorUid: String = "",
    val facilityName: String = "",
    val district: String = "",
    val bloodGroup: String = BloodGroup.O_POS.code,
    val donatedOn: String = "",
    val notes: String? = null,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "donorUid" to donorUid,
        "facilityName" to facilityName,
        "district" to district,
        "bloodGroup" to bloodGroup,
        "donatedOn" to donatedOn,
        "notes" to notes,
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CertificateRecord(
    @DocumentId val id: String = "",
    val donorUid: String = "",
    val title: String = "",
    val issuedBy: String = "",
    val issuedOn: String = "",
    val certificateCode: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "donorUid" to donorUid,
        "title" to title,
        "issuedBy" to issuedBy,
        "issuedOn" to issuedOn,
        "certificateCode" to certificateCode,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AbuseReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetId: String = "",
    val targetType: String = "REQUEST",
    val reason: String = "",
    val status: String = "OPEN",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "reporterUid" to reporterUid,
        "targetId" to targetId,
        "targetType" to targetType,
        "reason" to reason,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AdminLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val action: String = "",
    val targetId: String = "",
    val note: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "adminUid" to adminUid,
        "action" to action,
        "targetId" to targetId,
        "note" to note,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class DistrictGapStat(
    val zone: DistrictZone,
    val openRequests: Int,
    val availableDonors: Int,
    val gapScore: Int
)

object DemoSampleDirectory {
    val sampleOrganizations = listOf(
        Organization(
            id = "sample_hosp_victoria",
            ownerUid = "sample_directory",
            name = "Victoria Regional Medical Center (Sample)",
            orgType = "HOSPITAL",
            district = "Bengaluru Urban",
            city = "Bengaluru",
            addressLine = "KR Market Zone, Bengaluru",
            approxLat = 12.96,
            approxLng = 77.57,
            trustLevel = TrustLevel.HOSPITAL_AUTHORIZED.name,
            enquiryCount = 4,
            active = true,
            sampleRecord = true
        ),
        Organization(
            id = "sample_bb_kaveri",
            ownerUid = "sample_directory",
            name = "Kaveri Red Cross Blood Bank (Sample)",
            orgType = "BLOOD_BANK",
            district = "Mysuru",
            city = "Mysuru",
            addressLine = "Sayyaji Rao Road Zone, Mysuru",
            approxLat = 12.31,
            approxLng = 76.65,
            trustLevel = TrustLevel.HOSPITAL_CONFIRMED.name,
            enquiryCount = 2,
            active = true,
            sampleRecord = true
        ),
        Organization(
            id = "sample_bb_malabar",
            ownerUid = "sample_directory",
            name = "Ernakulam Community Blood Centre (Sample)",
            orgType = "BLOOD_BANK",
            district = "Ernakulam",
            city = "Kochi",
            addressLine = "Kaloor Zone, Kochi",
            approxLat = 9.99,
            approxLng = 76.29,
            trustLevel = TrustLevel.HOSPITAL_AUTHORIZED.name,
            enquiryCount = 3,
            active = true,
            sampleRecord = true
        )
    )

    val sampleInventory = listOf(
        BloodInventoryItem(
            id = "sample_inv_1",
            orgId = "sample_bb_kaveri",
            orgName = "Kaveri Red Cross Blood Bank (Sample)",
            orgCollection = "bloodBanks",
            ownerUid = "sample_directory",
            bloodGroup = "O+",
            componentType = ComponentType.PRBC.name,
            unitsAvailable = 12,
            stockStatus = StockStatus.ADEQUATE.name,
            expiryDateNote = "Sample freshness: 18 days remaining",
            district = "Mysuru",
            active = true,
            sampleRecord = true
        ),
        BloodInventoryItem(
            id = "sample_inv_2",
            orgId = "sample_bb_malabar",
            orgName = "Ernakulam Community Blood Centre (Sample)",
            orgCollection = "bloodBanks",
            ownerUid = "sample_directory",
            bloodGroup = "O-",
            componentType = ComponentType.WHOLE_BLOOD.name,
            unitsAvailable = 2,
            stockStatus = StockStatus.CRITICAL.name,
            expiryDateNote = "Sample freshness: Verify before dispatch",
            district = "Ernakulam",
            active = true,
            sampleRecord = true
        ),
        BloodInventoryItem(
            id = "sample_inv_3",
            orgId = "sample_hosp_victoria",
            orgName = "Victoria Regional Medical Center (Sample)",
            orgCollection = "hospitals",
            ownerUid = "sample_directory",
            bloodGroup = "A+",
            componentType = ComponentType.PLATELETS.name,
            unitsAvailable = 5,
            stockStatus = StockStatus.LOW.name,
            expiryDateNote = "Sample freshness: Short shelf-life component",
            district = "Bengaluru Urban",
            active = true,
            sampleRecord = true
        )
    )
}
