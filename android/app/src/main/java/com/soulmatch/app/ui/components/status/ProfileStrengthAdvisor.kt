package com.soulmatch.app.ui.components.status

// Shared profile-strength rules used by member screens to keep readiness messaging consistent.

import com.soulmatch.app.data.models.ProfileData

object ProfileStrengthAdvisor {
    fun score(profile: ProfileData): Int {
        val sections = listOf(
            profile.firstName.isNotBlank() &&
                profile.lastName.isNotBlank() &&
                !profile.dob.isNullOrBlank() &&
                profile.gender.isNotBlank() &&
                profile.religion.isNotBlank() &&
                profile.caste.isNotBlank() &&
                profile.maritalStatus.isNotBlank() &&
                profile.motherTongue.isNotBlank(),
            (profile.heightCm ?: 0) > 0 &&
                (profile.weightKg ?: 0) > 0 &&
                profile.complexion.isNotBlank() &&
                profile.bodyType.isNotBlank() &&
                profile.bloodGroup.isNotBlank(),
            profile.educationLevel.isNotBlank() &&
                (!profile.isEmployed || (
                    profile.occupation.isNotBlank() &&
                        profile.annualIncome.isNotBlank() &&
                        profile.workingCity.isNotBlank() &&
                        profile.workingState.isNotBlank() &&
                        profile.workingPincode.length == 6
                    )),
            profile.fatherOccupation.isNotBlank() &&
                profile.motherOccupation.isNotBlank() &&
                profile.numBrothers != null &&
                profile.numSisters != null &&
                profile.familyType.isNotBlank() &&
                profile.familyCity.isNotBlank(),
            profile.diet.isNotBlank() &&
                profile.smoking.isNotBlank() &&
                profile.drinking.isNotBlank() &&
                profile.aboutMe.trim().length >= 30,
            profile.rashi.isNotBlank() ||
                profile.nakshatra.isNotBlank() ||
                profile.birthCity.isNotBlank() ||
                profile.gotra.isNotBlank() ||
                profile.isManglik
        )
        return ((sections.count { it }.toFloat() / sections.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun pendingUpdates(profile: ProfileData): List<String> {
        val profileScore = score(profile)
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
            profileScore >= 100 -> listOf("Your profile is complete")
            profileScore >= 85 -> listOf("Review partner preferences", "Add one recent family-approved photo")
            else -> listOf("Review missing profile sections", "Add recent photos")
        }
    }

    fun summary(profile: ProfileData): String {
        val updates = pendingUpdates(profile)
        if (updates.firstOrNull() == "Your profile is complete") return "Your profile is complete."
        return "Next: ${updates.take(2).joinToString(", ")}."
    }
}
