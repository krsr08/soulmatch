package com.soulmatch.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileEnhancementsTest {
    @Test
    fun profileEnhancementFieldsDefaultSafelyForExistingRows() {
        val profile = ProfileData()

        assertFalse(profile.isPartnerPrefSet)
        assertEquals("active", profile.profileStatus)
        assertEquals("self", profile.profileCreatedBy)
    }

    @Test
    fun searchResultsCarryProfileOwnerTagIntoCards() {
        val summary = SearchProfileItem(profileId = "p-1", profileCreatedBy = "mediator")
            .toProfileSummary()

        assertEquals("mediator", summary.profileCreatedBy)
    }
}
