package com.soulmatch.app.data.auth

import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.AgentProfileData

private fun safeText(value: String?): String = value.orEmpty()

fun resolveWizardStep(profile: ProfileData?): Int? {
    val data = profile ?: return 1
    return when {
        data.profileId.isBlank() -> 1
        safeText(data.firstName).isBlank() ||
            data.dob.isNullOrBlank() ||
            safeText(data.gender).isBlank() ||
            data.heightCm == null ||
            safeText(data.motherTongue).isBlank() ||
            safeText(data.maritalStatus).isBlank() ||
            safeText(data.workingCity).isBlank() -> 1

        safeText(data.religion).isBlank() ||
            safeText(data.caste).isBlank() -> 2

        safeText(data.educationLevel).isBlank() ||
            safeText(data.occupation).isBlank() ||
            safeText(data.annualIncome).isBlank() -> 3

        safeText(data.fatherOccupation).isBlank() ||
            safeText(data.motherOccupation).isBlank() ||
            safeText(data.familyType).isBlank() -> 4

        safeText(data.diet).isBlank() ||
            safeText(data.smoking).isBlank() ||
            safeText(data.drinking).isBlank() -> 5
        !data.isPartnerPrefSet -> 6
        data.primaryPhotoUrl.isNullOrBlank() -> 7
        data.verificationStatus.equals("pending", ignoreCase = true) -> 8
        else -> null
    }
}

fun resolvePostLoginRoute(profile: ProfileData?): String {
    val nextWizardStep = resolveWizardStep(profile)
    return when {
        nextWizardStep != null && profile?.profileId.isNullOrBlank() -> "profile_intro"
        nextWizardStep != null -> "profile_wizard/$nextWizardStep"
        else -> "dashboard"
    }
}

fun resolveAgentRoute(agentProfile: AgentProfileData?): String {
    return if (!agentProfile?.advisorId.isNullOrBlank()) {
        "agent_dashboard"
    } else {
        "agent_onboarding"
    }
}
