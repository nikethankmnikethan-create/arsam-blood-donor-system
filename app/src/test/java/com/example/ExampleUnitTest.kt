package com.example

import com.example.i18n.AppLanguage
import com.example.i18n.ArsamStringsProvider
import com.example.model.BloodGroup
import com.example.model.RegionalZones
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun allEightBloodGroupsSupported() {
        assertEquals(8, BloodGroup.entries.size)
        assertEquals(BloodGroup.AB_NEG, BloodGroup.fromCode("AB-"))
        assertEquals(BloodGroup.O_POS, BloodGroup.fromCode("invalid"))
    }

    @Test
    fun regionalDistanceEstimationIsPositive() {
        val blr = RegionalZones.findOrDefault("Bengaluru Urban")
        val mys = RegionalZones.findOrDefault("Mysuru")
        val distKm = RegionalZones.approxDistanceKm(blr.approxLat, blr.approxLng, mys.approxLat, mys.approxLng)
        assertTrue(distKm in 100..160)
    }

    @Test
    fun multilingualStringsAndRoleFallbacksWork() {
        assertEquals(AppLanguage.KN, AppLanguage.fromCode("kn"))
        assertEquals(AppLanguage.ML, AppLanguage.fromCode("ml"))
        assertTrue(ArsamStringsProvider.forLanguage(AppLanguage.KN).tagline.isNotBlank())
        assertTrue(ArsamStringsProvider.forLanguage(AppLanguage.ML).tagline.isNotBlank())
        assertEquals(UserRole.DONOR, UserRole.fromName("UNKNOWN"))
    }
}
