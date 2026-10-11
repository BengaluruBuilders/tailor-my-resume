package com.tailormyresume.feature.applications.impl

import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.ApplicationStatus

internal fun ApplicationStatus.toTmrStatus(): TmrApplicationStatus = when (this) {
    ApplicationStatus.SAVED -> TmrApplicationStatus.Saved
    ApplicationStatus.APPLIED -> TmrApplicationStatus.Applied
    ApplicationStatus.INTERVIEW -> TmrApplicationStatus.Interview
    ApplicationStatus.OFFER -> TmrApplicationStatus.Offer
    ApplicationStatus.REJECTED -> TmrApplicationStatus.Rejected
}

internal fun TmrApplicationStatus.toApplicationStatus(): ApplicationStatus = when (this) {
    TmrApplicationStatus.Saved -> ApplicationStatus.SAVED
    TmrApplicationStatus.Applied -> ApplicationStatus.APPLIED
    TmrApplicationStatus.Interview -> ApplicationStatus.INTERVIEW
    TmrApplicationStatus.Offer -> ApplicationStatus.OFFER
    TmrApplicationStatus.Rejected -> ApplicationStatus.REJECTED
}

internal fun ApplicationKeywordCoverage?.keywordPercent(): Int? = this?.let { it.final ?: it.upTo }
