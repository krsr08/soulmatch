package com.soulmatch.app.ui.components.status

// Shared profile-strength rules used by member screens to keep readiness messaging consistent.

import com.soulmatch.app.data.models.ProfileData

object ProfileStrengthAdvisor {
    fun score(profile: ProfileData): Int {
        val firstName = safe(profile.firstName)
        val lastName = safe(profile.lastName)
        val dob = safe(profile.dob)
        val gender = safe(profile.gender)
        val religion = safe(profile.religion)
        val caste = safe(profile.caste)
        val maritalStatus = safe(profile.maritalStatus)
        val motherTongue = safe(profile.motherTongue)
        val complexion = safe(profile.complexion)
        val bodyType = safe(profile.bodyType)
        val bloodGroup = safe(profile.bloodGroup)
        val educationLevel = safe(profile.educationLevel)
        val occupation = safe(profile.occupation)
        val annualIncome = safe(profile.annualIncome)
        val workingCity = safe(profile.workingCity)
        val workingState = safe(profile.workingState)
        val workingPincode = safe(profile.workingPincode)
        val fatherOccupation = safe(profile.fatherOccupation)
        val motherOccupation = safe(profile.motherOccupation)
        val familyType = safe(profile.familyType)
        val familyCity = safe(profile.familyCity)
        val diet = safe(profile.diet)
        val smoking = safe(profile.smoking)
        val drinking = safe(profile.drinking)
        val aboutMe = safe(profile.aboutMe)
        val rashi = safe(profile.rashi)
        val nakshatra = safe(profile.nakshatra)
        val birthCity = safe(profile.birthCity)
        val gotra = safe(profile.gotra)
        val sections = listOf(
            firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                dob.isNotBlank() &&
                gender.isNotBlank() &&
                religion.isNotBlank() &&
                caste.isNotBlank() &&
                maritalStatus.isNotBlank() &&
                motherTongue.isNotBlank(),
            (profile.heightCm ?: 0) > 0 &&
                (profile.weightKg ?: 0) > 0 &&
                complexion.isNotBlank() &&
                bodyType.isNotBlank() &&
                bloodGroup.isNotBlank(),
            educationLevel.isNotBlank() &&
                (!profile.isEmployed || (
                    occupation.isNotBlank() &&
                        annualIncome.isNotBlank() &&
                        workingCity.isNotBlank() &&
                        workingState.isNotBlank() &&
                        workingPincode.length == 6
                    )),
            fatherOccupation.isNotBlank() &&
                motherOccupation.isNotBlank() &&
                profile.numBrothers != null &&
                profile.numSisters != null &&
                familyType.isNotBlank() &&
                familyCity.isNotBlank(),
            diet.isNotBlank() &&
                smoking.isNotBlank() &&
                drinking.isNotBlank() &&
                aboutMe.trim().length >= 30,
            rashi.isNotBlank() ||
                nakshatra.isNotBlank() ||
                birthCity.isNotBlank() ||
                gotra.isNotBlank() ||
                profile.isManglik
        )
        return ((sections.count { it }.toFloat() / sections.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun pendingUpdates(profile: ProfileData): List<String> {
        val profileScore = score(profile)
        val primaryPhotoUrl = safe(profile.primaryPhotoUrl)
        val fatherOccupation = safe(profile.fatherOccupation)
        val motherOccupation = safe(profile.motherOccupation)
        val familyCity = safe(profile.familyCity)
        val educationLevel = safe(profile.educationLevel)
        val occupation = safe(profile.occupation)
        val annualIncome = safe(profile.annualIncome)
        val complexion = safe(profile.complexion)
        val bodyType = safe(profile.bodyType)
        val diet = safe(profile.diet)
        val smoking = safe(profile.smoking)
        val drinking = safe(profile.drinking)
        val aboutMe = safe(profile.aboutMe)
        val rashi = safe(profile.rashi)
        val nakshatra = safe(profile.nakshatra)
        val birthCity = safe(profile.birthCity)
        val updates = buildList {
            if (primaryPhotoUrl.isBlank()) add("Add your profile photo")
            if (fatherOccupation.isBlank() || motherOccupation.isBlank() || familyCity.isBlank()) {
                add("Complete family details")
            }
            if (educationLevel.isBlank() || occupation.isBlank() || annualIncome.isBlank()) {
                add("Add work and education")
            }
            if ((profile.heightCm ?: 0) <= 0 || complexion.isBlank() || bodyType.isBlank()) {
                add("Add height and personal details")
            }
            if (diet.isBlank() || smoking.isBlank() || drinking.isBlank()) {
                add("Complete lifestyle choices")
            }
            if (aboutMe.trim().length < 30) add("Write a short introduction")
            if (rashi.isBlank() && nakshatra.isBlank() && birthCity.isBlank()) {
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

    private fun safe(value: String?): String = value.orEmpty()
}
