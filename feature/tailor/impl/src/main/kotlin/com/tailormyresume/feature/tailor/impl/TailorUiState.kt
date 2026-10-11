package com.tailormyresume.feature.tailor.impl

import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.model.TailoredBullet

internal fun sectionReportId(sectionKey: String): String = "section:$sectionKey"

internal data class JobHeader(val title: String, val company: String)

internal sealed interface TailorUiState {
    data class Loading(
        val job: JobHeader? = null,
        val factCount: Int? = null,
        val lineCount: Int? = null,
    ) : TailorUiState

    data class Failed(val job: JobHeader? = null) : TailorUiState

    data object NotFound : TailorUiState

    data class Success(
        val job: JobHeader,
        val sections: List<ReviewSection>,
        val notAdded: List<String>,
        val changes: List<TailorBulletUi>,
        val reportedIds: Set<String> = emptySet(),
    ) : TailorUiState {
        val totalCount: Int get() = changes.size

        val reviewedCount: Int get() = changes.count { it.isReviewed }

        val flaggedCount: Int get() = changes.count { it.isFlagged }

        val openCount: Int get() = totalCount - reviewedCount

        val isAllReviewed: Boolean get() = totalCount > 0 && openCount == 0

        val canPreviewExport: Boolean get() = sections.isNotEmpty()

        fun nextOpenChange(afterBulletId: String? = null): TailorBulletUi? {
            val start = changes.indexOfFirst { it.bullet.id == afterBulletId }
            val ordered = if (start < 0) changes else changes.drop(start + 1) + changes.take(start + 1)
            return ordered.firstOrNull { !it.isReviewed }
        }

        fun changeIndexOf(bulletId: String): Int = changes.indexOfFirst { it.bullet.id == bulletId }
    }
}

internal sealed interface ReviewSection {
    val key: String

    data class Entries(val category: EntryCategory, val entries: List<TailorEntryUi>) : ReviewSection {
        override val key: String get() = category.name

        val changeCount: Int get() = entries.sumOf { entry -> entry.bullets.count { it.isChange } }
    }

    data class Skills(val skills: List<String>) : ReviewSection {
        override val key: String get() = "SKILLS"
    }
}

internal data class TailorEntryUi(
    val entryId: String,
    val category: EntryCategory,
    val title: String,
    val organization: String,
    val dateRange: String,
    val bullets: List<TailorBulletUi>,
)

internal data class TailoredBulletSource(
    val id: String,
    val displayId: String,
    val entryId: String,
    val category: EntryCategory,
    val text: String,
    val source: FactSource,
    val entryTitle: String,
    val organization: String,
    val dateRange: String,
)

internal data class TailorBulletUi(
    val bullet: TailoredBullet,
    val sources: List<TailoredBulletSource>,
    val isStale: Boolean,
    val isUserEdited: Boolean = false,
) {
    val kind: BulletReviewKind =
        if (isUserEdited && !isStale) BulletReviewKind.REVIEWABLE else bullet.reviewKind(isStale)

    val isChange: Boolean get() = kind == BulletReviewKind.REVIEWABLE || kind == BulletReviewKind.REPAIR_FAILED

    val isReviewed: Boolean
        get() = when (kind) {
            BulletReviewKind.REPAIR_FAILED -> true
            BulletReviewKind.REVIEWABLE -> bullet.decision != BulletDecision.PENDING
            else -> false
        }

    val isFlagged: Boolean
        get() = kind == BulletReviewKind.REVIEWABLE &&
            bullet.decision == BulletDecision.PENDING &&
            bullet.violations.isNotEmpty()

    val state: BulletReviewState
        get() = when {
            kind == BulletReviewKind.REPAIR_FAILED -> BulletReviewState.REPAIR_FAILED
            kind == BulletReviewKind.STALE -> BulletReviewState.STALE
            kind == BulletReviewKind.UNCHANGED -> BulletReviewState.UNCHANGED
            isUserEdited && bullet.decision == BulletDecision.ACCEPTED -> BulletReviewState.USER_EDITED
            bullet.decision == BulletDecision.ACCEPTED -> BulletReviewState.ACCEPTED
            bullet.decision == BulletDecision.REJECTED -> BulletReviewState.ORIGINAL_KEPT
            isFlagged -> BulletReviewState.FLAGGED
            else -> BulletReviewState.TO_REVIEW
        }

    val flagViolation: GuardrailViolation? get() = bullet.violations.firstOrNull()
}

internal enum class BulletReviewKind { STALE, REPAIR_FAILED, UNCHANGED, REVIEWABLE }

internal enum class BulletReviewState {
    TO_REVIEW,
    FLAGGED,
    ACCEPTED,
    ORIGINAL_KEPT,
    USER_EDITED,
    REPAIR_FAILED,
    STALE,
    UNCHANGED,
}

internal fun TailoredBullet.reviewKind(isStale: Boolean): BulletReviewKind = when {
    isStale -> BulletReviewKind.STALE
    violations.isNotEmpty() && proposedText.trim() == originalText.trim() -> BulletReviewKind.REPAIR_FAILED
    proposedText.trim() == originalText.trim() && EditType.REORDER !in editTypes -> BulletReviewKind.UNCHANGED
    else -> BulletReviewKind.REVIEWABLE
}

internal fun TailorUiState.Success.reportedGenerationId(kind: ReportedItemKind, itemId: String): String? {
    val bullets = sections.filterIsInstance<ReviewSection.Entries>().flatMap { it.entries }.flatMap { it.bullets }.map { it.bullet }
    return when (kind) {
        ReportedItemKind.RESUME_BULLET -> bullets.firstOrNull { it.id == itemId }?.generationId
        else -> bullets.firstNotNullOfOrNull { it.generationId }
    }
}

internal fun TailorUiState.Success.reportedText(kind: ReportedItemKind, itemId: String): String? = when (kind) {
    ReportedItemKind.RESUME_BULLET -> sections.filterIsInstance<ReviewSection.Entries>()
        .flatMap { it.entries }.flatMap { it.bullets }
        .firstOrNull { it.bullet.id == itemId }?.bullet?.proposedText

    else -> sections.firstOrNull { it.key == itemId }?.let { section ->
        when (section) {
            is ReviewSection.Skills -> section.skills.joinToString(", ")
            is ReviewSection.Entries -> section.entries.flatMap { it.bullets }.joinToString("\n") { it.bullet.proposedText }
        }
    }
}
