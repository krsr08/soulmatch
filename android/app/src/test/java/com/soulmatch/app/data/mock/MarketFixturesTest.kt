package com.soulmatch.app.data.mock

import com.soulmatch.app.data.models.SearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketFixturesTest {
    @Test
    fun dummyProfilesCoverMajorIndianLanguageMarkets() {
        assertTrue(MarketFixtures.matches.size >= 500)

        val tongues = MarketFixtures.matches
            .map { MarketFixtures.profileDetails(it.profileId).motherTongue }
            .toSet()

        assertTrue(tongues.containsAll(setOf("Telugu", "Tamil", "Malayalam", "Hindi", "Punjabi", "Gujarati", "Odia", "Marathi")))
    }

    @Test
    fun defaultAnySearchShowsAllMockMatches() {
        assertEquals(MarketFixtures.matches.size, MarketFixtures.search(SearchRequest()).size)
    }

    @Test
    fun freePlanAllowsAtLeastTwentyFiveProfileViews() {
        val freePlan = MarketFixtures.plans.first { it.planId == "free" }
        assertTrue(freePlan.features.any { it.contains("25 profiles", ignoreCase = true) })
    }
}
