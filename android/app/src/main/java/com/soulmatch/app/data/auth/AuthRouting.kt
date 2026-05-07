package com.soulmatch.app.data.auth

import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.AgentProfileData

private fun safeText(value: String?): String = value.orEmpty()

fun resolveWizardStep(profile: ProfileData?): Int? {
    val data = profile ?: return 1
    return when {
        data.profileId.isBlank() -> 1
        safeText(data.firstName).isBlank() ||
            safeText(data.lastName).isBlank() ||
            data.dob.isNullOrBlank() ||
            safeText(data.gender).isBlank() ||
            safeText(data.religion).isBlank() ||
            safeText(data.caste).isBlank() ||
            safeText(data.motherTongue).isBlank() ||
            safeText(data.maritalStatus).isBlank() -> 1

        (data.heightCm ?: 0) <= 0 ||
            (data.weightKg ?: 0) <= 0 ||
            safeText(data.complexion).isBlank() ||
            safeText(data.bodyType).isBlank() ||
            safeText(data.bloodGroup).isBlank() -> 2

        safeText(data.educationLevel).isBlank() ||
            (data.isEmployed && (
                safeText(data.occupation).isBlank() ||
                    safeText(data.annualIncome).isBlank() ||
                    safeText(data.workingCity).isBlank() ||
                    safeText(data.workingState).isBlank() ||
                    safeText(data.workingPincode).length != 6
                )) -> 3

        safeText(data.fatherOccupation).isBlank() ||
            safeText(data.motherOccupation).isBlank() ||
            data.numBrothers == null ||
            data.numSisters == null ||
            safeText(data.familyType).isBlank() ||
            safeText(data.familyCity).isBlank() -> 4

        safeText(data.diet).isBlank() || safeText(data.aboutMe).trim().length < 30 -> 5
        else -> null
    }
}

fun resolvePostLoginRoute(profile: ProfileData?): String {
    val nextWizardStep = resolveWizardStep(profile)
    return if (profile?.profileId.isNullOrBlank() && nextWizardStep != null) {
        "profile_wizard/$nextWizardStep"
    } else {
        "dashboard"
    }
}

fun resolveAgentRoute(agentProfile: AgentProfileData?): String {
    return if (agentProfile?.isOnboarded == true) {
        "agent_dashboard"
    } else {
        "agent_onboarding"
    }
}
