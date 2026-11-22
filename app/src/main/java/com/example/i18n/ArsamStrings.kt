package com.example.i18n

enum class AppLanguage(val code: String, val nativeLabel: String) {
    EN("en", "English"),
    KN("kn", "ಕನ್ನಡ"),
    ML("ml", "മലയാളം");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

data class ArsamStrings(
    val appTitle: String,
    val tagline: String,
    val heroSubtitle: String,
    val signInWithGoogle: String,
    val aboutArsamTitle: String,
    val aboutArsamBody: String,
    val medicalDisclaimer: String,
    val stockDisclaimer: String,
    val navDashboard: String,
    val navDiscover: String,
    val navRequests: String,
    val navBanks: String,
    val navAssistant: String,
    val livePulseTitle: String,
    val availableNow: String,
    val availableLater: String,
    val unavailable: String,
    val emergencyRequestCta: String,
    val findDonorsTitle: String,
    val mapGapTitle: String,
    val safeContactTitle: String,
    val trustGateTitle: String,
    val bloodFreshnessTitle: String,
    val donorCareTitle: String,
    val aiAssistantTitle: String,
    val notificationsTitle: String,
    val settingsTitle: String,
    val signOutLabel: String,
    val languageLabel: String,
    val translationReviewNote: String,
    val privacyNotice: String
)

object ArsamStringsProvider {
    private val en = ArsamStrings(
        appTitle = "ARSAM",
        tagline = "Connecting Donors. Saving Lives.",
        heroSubtitle = "Community Blood Donor Registry, Trust-Gated Emergency Coordination, and Privacy-First Discovery.",
        signInWithGoogle = "Sign in with Google",
        aboutArsamTitle = "About ARSAM",
        aboutArsamBody = "ARSAM connects verified volunteer donors, patients, hospitals, and blood banks with consent-first contact and transparent blood freshness tracking.",
        medicalDisclaimer = "Preliminary coordination only. Medical professionals and authorized blood banks must confirm donor eligibility, blood compatibility, and crossmatching.",
        stockDisclaimer = "Displayed stock is informational only and is not a guaranteed reservation. Confirm directly with the blood bank.",
        navDashboard = "Dashboard",
        navDiscover = "Discover",
        navRequests = "Requests",
        navBanks = "Blood Banks",
        navAssistant = "AI Guide",
        livePulseTitle = "ARSAM Live Pulse",
        availableNow = "Available Now",
        availableLater = "Available Later",
        unavailable = "Unavailable",
        emergencyRequestCta = "New Emergency Request",
        findDonorsTitle = "Find Donors & Map",
        mapGapTitle = "Community Gap Map",
        safeContactTitle = "ARSAM Safe Contact",
        trustGateTitle = "ARSAM Trust Gate",
        bloodFreshnessTitle = "Blood Freshness",
        donorCareTitle = "ARSAM Donor Care",
        aiAssistantTitle = "ARSAM AI Guide",
        notificationsTitle = "Alerts & Rescue Chain",
        settingsTitle = "Profile & Settings",
        signOutLabel = "Sign Out",
        languageLabel = "Language / ಭಾಷೆ / ഭാഷ",
        translationReviewNote = "Emergency & clinical wording should be reviewed by qualified regional coordinators.",
        privacyNotice = "Exact home addresses and personal phone numbers are never publicly exposed."
    )

    private val kn = ArsamStrings(
        appTitle = "ARSAM (ಅರ್ಸಮ್)",
        tagline = "ದಾನಿಗಳನ್ನು ಸಂಪರ್ಕಿಸುವುದು. ಜೀವಗಳನ್ನು ಉಳಿಸುವುದು.",
        heroSubtitle = "ಸಮುದಾಯ ರಕ್ತದಾನಿ ನೋಂದಣಿ, ವಿಶ್ವಾಸಾರ್ಹ ತುರ್ತು ವಿನಂತಿಗಳು ಮತ್ತು ಗೌಪ್ಯತೆ-ಮೊದಲ ಸಂಪರ್ಕ.",
        signInWithGoogle = "Google ಮೂಲಕ ಸೈನ್ ಇನ್ ಮಾಡಿ",
        aboutArsamTitle = "ARSAM ಬಗ್ಗೆ",
        aboutArsamBody = "ARSAM ಸ್ವಯಂಸೇವಕ ರಕ್ತದಾನಿಗಳು, ಆಸ್ಪತ್ರೆಗಳು ಮತ್ತು ರಕ್ತನಿಧಿಗಳನ್ನು ಸಮ್ಮತಿ-ಆಧಾರಿತ ಸಂಪರ್ಕದೊಂದಿಗೆ ಜೋಡಿಸುತ್ತದೆ.",
        medicalDisclaimer = "ಇದು ಪ್ರಾಥಮಿಕ ಹೊಂದಾಣಿಕೆ ಮಾತ್ರ. ದಾನಿಗಳ ಅರ್ಹತೆ ಮತ್ತು ರಕ್ತ ಹೊಂದಾಣಿಕೆಯನ್ನು ವೈದ್ಯಕೀಯ ಸಿಬ್ಬಂದಿಯೇ ದೃಢೀಕರಿಸಬೇಕು.",
        stockDisclaimer = "ತೋರಿಸಿದ ರಕ್ತದ ದಾಸ್ತಾನು ಮಾಹಿತಿಗಾಗಿ ಮಾತ್ರ; ಇದು ಕಾಯ್ದಿರಿಸುವಿಕೆಯಲ್ಲ.",
        navDashboard = "ಮುಖಪುಟ",
        navDiscover = "ದಾನಿಗಳು",
        navRequests = "ವಿನಂತಿಗಳು",
        navBanks = "ರಕ್ತನಿಧಿ",
        navAssistant = "AI ಸಹಾಯಕ",
        livePulseTitle = "ARSAM ಲೈವ್ ಪಲ್ಸ್",
        availableNow = "ಈಗ ಲಭ್ಯ",
        availableLater = "ನಂತರ ಲಭ್ಯ",
        unavailable = "ಲಭ್ಯವಿಲ್ಲ",
        emergencyRequestCta = "ಹೊಸ ತುರ್ತು ವಿನಂತಿ",
        findDonorsTitle = "ದಾನಿಗಳನ್ನು ಹುಡುಕಿ & ನಕ್ಷೆ",
        mapGapTitle = "ಸಮುದಾಯ ಕೊರತೆ ನಕ್ಷೆ",
        safeContactTitle = "ಸುರಕ್ಷಿತ ಸಂಪರ್ಕ",
        trustGateTitle = "ಟ್ರಸ್ಟ್ ಗೇಟ್",
        bloodFreshnessTitle = "ರಕ್ತ ತಾಜಾತನ",
        donorCareTitle = "ದಾನಿ ಆರೈಕೆ",
        aiAssistantTitle = "ARSAM AI ಮಾರ್ಗದರ್ಶಿ",
        notificationsTitle = "ಎಚ್ಚರಿಕೆಗಳು",
        settingsTitle = "ಸೆಟ್ಟಿಂಗ್‌ಗಳು",
        signOutLabel = "ಸೈನ್ ಔಟ್",
        languageLabel = "ಭಾಷೆ (Language)",
        translationReviewNote = "ತುರ್ತು ಮತ್ತು ವೈದ್ಯಕೀಯ ಪದಗಳನ್ನು ಅರ್ಹ ಸಿಬ್ಬಂದಿ ಪರಿಶೀಲಿಸಬೇಕು.",
        privacyNotice = "ನಿಖರ ವಿಳಾಸ ಮತ್ತು ಫೋನ್ ಸಂಖ್ಯೆಗಳನ್ನು ಸಾರ್ವಜನಿಕವಾಗಿ ತೋರಿಸಲಾಗುವುದಿಲ್ಲ."
    )

    private val ml = ArsamStrings(
        appTitle = "ARSAM (അർസം)",
        tagline = "ദാതാക്കളെ ബന്ധിപ്പിക്കുന്നു. ജീവനുകൾ രക്ഷിക്കുന്നു.",
        heroSubtitle = "കമ്മ്യൂണിറ്റി രക്തദാതാക്കളുടെ രജിസ്ട്രി, അടിയന്തര ഏകോപനം, സ്വകാര്യത സംരക്ഷിത സമ്പർക്കം.",
        signInWithGoogle = "Google ഉപയോഗിച്ച് സൈൻ ഇൻ ചെയ്യുക",
        aboutArsamTitle = "ARSAM നെക്കുറിച്ച്",
        aboutArsamBody = "സന്നദ്ധ രക്തദാതാക്കളെയും ആശുപത്രികളെയും ബ്ലഡ് ബാങ്കുകളെയും സുരക്ഷിതമായി ബന്ധിപ്പിക്കുന്ന സംവിധാനം.",
        medicalDisclaimer = "ഇത് പ്രാഥമിക ഏകോപനം മാത്രമാണ്. ദാതാവിന്റെ യോഗ്യതയും ക്രോസ്‌മാച്ചിംഗും ഡോക്ടർമാരോ ബ്ലഡ് ബാങ്കോ ഉറപ്പാക്കണം.",
        stockDisclaimer = "കാണിച്ചിരിക്കുന്ന സ്റ്റോക്ക് വിവരങ്ങൾക്ക് മാത്രമാണ്; ഇത് ഉറപ്പായ റിസർവേഷൻ അല്ല.",
        navDashboard = "ഡാഷ്‌ബോർഡ്",
        navDiscover = "ദാതാക്കൾ",
        navRequests = "അഭ്യർത്ഥനകൾ",
        navBanks = "ബ്ലഡ് ബാങ്ക്",
        navAssistant = "AI സഹായി",
        livePulseTitle = "ARSAM ലൈവ് പൾസ്",
        availableNow = "ഇപ്പോൾ ലഭ്യമാണ്",
        availableLater = "പിന്നീട് ലഭ്യമാണ്",
        unavailable = "ലഭ്യമല്ല",
        emergencyRequestCta = "പുതിയ അടിയന്തര അഭ്യർത്ഥന",
        findDonorsTitle = "ദാതാക്കളെ കണ്ടെത്തുക & മാപ്പ്",
        mapGapTitle = "കമ്മ്യൂണിറ്റി ഗ്യാപ് മാപ്പ്",
        safeContactTitle = "സുരക്ഷിത സമ്പർക്കം",
        trustGateTitle = "ട്രസ്റ്റ് ഗേറ്റ്",
        bloodFreshnessTitle = "രക്ത ശേഖര പുതുമ",
        donorCareTitle = "ദാതാക്കളുടെ പരിചരണം",
        aiAssistantTitle = "ARSAM AI ഗൈഡ്",
        notificationsTitle = "അറിയിപ്പുകൾ",
        settingsTitle = "ക്രമീകരണങ്ങൾ",
        signOutLabel = "സൈൻ ഔട്ട്",
        languageLabel = "ഭാഷ (Language)",
        translationReviewNote = "അടിയന്തരവും വൈദ്യശാസ്ത്രപരവുമായ പദങ്ങൾ യോഗ്യരായവർ പരിശോധിക്കേണ്ടതാണ്.",
        privacyNotice = "വ്യക്തിഗത ഫോൺ നമ്പറുകളും കൃത്യമായ വിലാസവും ഒരിക്കലും പരസ്യപ്പെടുത്തുകയില്ല."
    )

    fun forLanguage(lang: AppLanguage): ArsamStrings = when (lang) {
        AppLanguage.EN -> en
        AppLanguage.KN -> kn
        AppLanguage.ML -> ml
    }
}
