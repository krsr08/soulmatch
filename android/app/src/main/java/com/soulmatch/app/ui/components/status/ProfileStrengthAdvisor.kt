package com.soulmatch.app.ui.components.status

// Shared profile-strength rules used by member screens to keep readiness messaging consistent.

import com.soulmatch.app.data.models.ProfileData

object ProfileStrengthAdvisor {
    fun score(profile: ProfileData): Int {
        if (isProfileSubmitted(profile)) return 100
        val firstName = safe(profile.firstName)
        val dob = safe(profile.dob)
        val gender = safe(profile.gender)
        val religion = safe(profile.religion)
        val caste = safe(profile.caste)
        val maritalStatus = safe(profile.maritalStatus)
        val motherTongue = safe(profile.motherTongue)
        val nativePlace = safe(profile.nativePlace)
        val educationLevel = safe(profile.educationLevel)
        val institutionName = safe(profile.institutionName)
        val occupation = safe(profile.occupation)
        val companyName = safe(profile.companyName)
        val annualIncome = safe(profile.annualIncome)
        val workingCity = safe(profile.workingCity)
        val fatherOccupation = safe(profile.fatherOccupation)
        val motherOccupation = safe(profile.motherOccupation)
        val familyType = safe(profile.familyType)
        val familyStatus = safe(profile.familyStatus)
        val aboutFamily = safe(profile.aboutFamily)
        val diet = safe(profile.diet)
        val smoking = safe(profile.smoking)
        val drinking = safe(profile.drinking)
        val hobbies = profile.hobbies
        val languagesKnown = profile.languagesKnown
        val personalityTraits = profile.personalityTraits
        val sections = listOf(
            firstName.isNotBlank() &&
                dob.isNotBlank() &&
                gender.isNotBlank() &&
                maritalStatus.isNotBlank() &&
                motherTongue.isNotBlank() &&
                workingCity.isNotBlank() &&
                nativePlace.isNotBlank() &&
                (profile.heightCm ?: 0) > 0,
            religion.isNotBlank() &&
                caste.isNotBlank(),
            educationLevel.isNotBlank() &&
                institutionName.isNotBlank() &&
                occupation.isNotBlank() &&
                companyName.isNotBlank() &&
                annualIncome.isNotBlank() &&
                (profile.workLocation.isNotBlank() || workingCity.isNotBlank()),
            fatherOccupation.isNotBlank() &&
                motherOccupation.isNotBlank() &&
                familyType.isNotBlank() &&
                familyStatus.isNotBlank() &&
                aboutFamily.trim().length >= 40,
            diet.isNotBlank() &&
                smoking.isNotBlank() &&
                drinking.isNotBlank() &&
                hobbies.isNotEmpty() &&
                languagesKnown.isNotEmpty() &&
                personalityTraits.isNotEmpty(),
            profile.isPartnerPrefSet &&
                profile.primaryPhotoUrl.isNullOrBlank().not()
        )
        return ((sections.count { it }.toFloat() / sections.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun pendingUpdates(profile: ProfileData): List<String> {
        if (isProfileSubmitted(profile)) {
            return listOf("Your profile is complete")
        }
        val profileScore = score(profile)
        val primaryPhotoUrl = safe(profile.primaryPhotoUrl)
        val fatherOccupation = safe(profile.fatherOccupation)
        val motherOccupation = safe(profile.motherOccupation)
        val familyStatus = safe(profile.familyStatus)
        val aboutFamily = safe(profile.aboutFamily)
        val educationLevel = safe(profile.educationLevel)
        val institutionName = safe(profile.institutionName)
        val occupation = safe(profile.occupation)
        val companyName = safe(profile.companyName)
        val annualIncome = safe(profile.annualIncome)
        val diet = safe(profile.diet)
        val smoking = safe(profile.smoking)
        val drinking = safe(profile.drinking)
        val updates = buildList {
            if (primaryPhotoUrl.isBlank()) add("Add your profile photo")
            if (fatherOccupation.isBlank() || motherOccupation.isBlank() || familyStatus.isBlank() || aboutFamily.trim().length < 40) {
                add("Complete family details")
            }
            if (educationLevel.isBlank() || institutionName.isBlank() || occupation.isBlank() || companyName.isBlank() || annualIncome.isBlank()) {
                add("Add work and education")
            }
            if ((profile.heightCm ?: 0) <= 0 || safe(profile.nativePlace).isBlank() || safe(profile.workingCity).isBlank()) {
                add("Complete basic details")
            }
            if (diet.isBlank() || smoking.isBlank() || drinking.isBlank() || profile.hobbies.isEmpty() || profile.languagesKnown.isEmpty() || profile.personalityTraits.isEmpty()) {
                add("Complete lifestyle choices")
            }
            if (!profile.isPartnerPrefSet) add("Review partner preferences")
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

    private fun safe(value: String?): String = value.orEmpty()

    private fun isProfileSubmitted(profile: ProfileData): Boolean {
        val reviewStatus = safe(profile.reviewStatus).lowercase()
        return reviewStatus == "submitted" || reviewStatus == "under_review" || reviewStatus == "approved"
    }
}
