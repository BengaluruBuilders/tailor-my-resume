package com.tailormyresume.core.domain

import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobApplication

enum class ExportCheck { ALLOWED, NOT_ACCEPTED, PENDING_CHANGES }

object ExportReadiness {
    fun check(application: JobApplication): ExportCheck {
        val resume = application.tailoredResume
        return when {
            resume == null -> ExportCheck.NOT_ACCEPTED
            resume.changeCount == 0 -> ExportCheck.ALLOWED
            application.changesAcceptedAt == null -> ExportCheck.NOT_ACCEPTED
            BulletDecision.PENDING in resume.decisions -> ExportCheck.PENDING_CHANGES
            else -> ExportCheck.ALLOWED
        }
    }
}
