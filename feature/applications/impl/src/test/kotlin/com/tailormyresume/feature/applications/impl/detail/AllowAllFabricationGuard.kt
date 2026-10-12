package com.tailormyresume.feature.applications.impl.detail

import com.tailormyresume.core.domain.FabricationGuard
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GuardrailViolation

internal object AllowAllFabricationGuard : FabricationGuard {
    override fun check(
        proposedText: String,
        sources: List<EvidenceBullet>,
        profile: CandidateProfile,
    ): List<GuardrailViolation> = emptyList()
}
