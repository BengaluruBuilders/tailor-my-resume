package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.model.KeptJobDescription

sealed interface OnboardingStep {
    data object SignIn : OnboardingStep

    data object ImportResume : OnboardingStep

    data object ConfirmFacts : OnboardingStep

    data class GapAnalysis(val job: KeptJobDescription) : OnboardingStep

    data object PasteJobDescription : OnboardingStep

    data object Applications : OnboardingStep
}
