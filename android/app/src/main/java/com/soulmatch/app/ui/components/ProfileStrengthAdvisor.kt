package com.soulmatch.app.ui.components

import com.soulmatch.app.data.models.ProfileData

object ProfileStrengthAdvisor {
    fun pendingUpdates(profile: ProfileData): List<String> {
        val updates = buildList {
            if (profile.primaryPhotoUrl.isNullOrBlank()) add("Add your profile photo")
            if (profile.fatherOccupation.isBlank() || profile.motherOccupation.isBlank() || profile.familyCity.isBlank()) {
                add("Complete family details")
            }
            if (profile.educationLevel.isBlank() || profile.occupation.isBlank() || profile.annualIncome.isBlank()) {
                add("Add work and education")
            }
            if ((profile.heightCm ?: 0) <= 0 || profile.complexion.isBlank() || profile.bodyType.isBlank()) {
                add("Add height and personal details")
            }
            if (profile.diet.isBlank() || profile.smoking.isBlank() || profile.drinking.isBlank()) {
                add("Complete lifestyle choices")
            }
            if (profile.aboutMe.trim().length < 30) add("Write a short introduction")
            if (profile.rashi.isBlank() && profile.nakshatra.isBlank() && profile.birthCity.isBlank()) {
                add("Add horoscope details if your family uses them")
            }
        }

        if (updates.isNotEmpty()) return updates

        return when {
            profile.completionScore >= 100 -> listOf("Your profile is complete")
            profile.completionScore >= 85 -> listOf("Review partner preferences", "Add one recent family-approved photo")
            else -> listOf("Review missing profile sections", "Add recent photos")
        }
    }

    fun summary(profile: ProfileData): String {
        val updates = pendingUpdates(profile)
        if (updates.firstOrNull() == "Your profile is complete") return "Your profile is complete."
        return "Next: ${updates.take(2).joinToString(", ")}."
    }
}
