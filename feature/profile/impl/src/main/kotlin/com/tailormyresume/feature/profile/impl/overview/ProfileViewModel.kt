package com.tailormyresume.feature.profile.impl.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.profile.ProfileCompleteness
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.ProfileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Month
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.time.Clock

internal sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data object Empty : ProfileUiState

    data class Content(
        val initials: String,
        val name: String,
        val headline: String,
        val years: Int?,
        val city: String,
        val percent: Int,
        val experienceCount: Int,
        val educationCount: Int,
        val skillsCount: Int,
        val achievementsCount: Int,
        val linkedinMissing: Boolean,
        val sourceFileName: String?,
    ) : ProfileUiState
}

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = profileRepository.observeProfile().map { profile ->
        if (profile == null) {
            ProfileUiState.Empty
        } else {
            ProfileUiState.Content(
                initials = initialsOf(profile.fullName),
                name = profile.fullName,
                headline = profile.headline,
                years = yearsOfExperience(profile.entries, clock),
                city = profile.city,
                percent = ProfileCompleteness.percent(profile),
                experienceCount = profile.entryCount(EntryCategory.EXPERIENCE),
                educationCount = profile.entryCount(EntryCategory.EDUCATION),
                skillsCount = profile.skills.size,
                achievementsCount = profile.entryCount(EntryCategory.ACHIEVEMENT),
                linkedinMissing = profile.linkedinUrl.isBlank(),
                sourceFileName = profile.sourceFileName?.takeIf { it.isNotBlank() },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading,
    )

    private fun CandidateProfile.entryCount(category: EntryCategory): Int =
        entries.count { it.category == category }
}

private fun initialsOf(fullName: String): String =
    fullName.split(' ', '\t', '\n')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

internal fun yearsOfExperience(entries: List<ProfileEntry>, clock: Clock): Int? {
    val roles = entries.filter { it.category == EntryCategory.EXPERIENCE }
    if (roles.isEmpty()) return null
    val spans = mutableListOf<Pair<Int, Int>>()
    val thisMonth = clockMonthIndex(clock)
    for (role in roles) {
        val start = monthIndex(role.startDate, JANUARY) ?: return null
        val end = endMonthIndex(role.endDate, clock) ?: return null
        if (end < start) return null
        if (start <= thisMonth) spans += start to minOf(end, thisMonth) + 1
    }
    return mergedMonthSpan(spans) / MONTHS_PER_YEAR
}

private const val MONTHS_PER_YEAR = 12

private const val JANUARY = 1

private const val DECEMBER = 12

private val monthNumbers: Map<String, Int> = buildMap {
    for (month in Month.entries) {
        val name = month.name.lowercase()
        put(name, month.value)
        put(name.take(3), month.value)
    }
}

private val ongoingEndLabels = setOf("present", "now", "current")

private fun endMonthIndex(text: String, clock: Clock): Int? =
    if (ongoingEndLabels.contains(text.trim().lowercase())) clockMonthIndex(clock) else monthIndex(text, DECEMBER)

private fun monthIndex(text: String, bareYearMonth: Int): Int? {
    val words = text.replace(',', ' ').split(' ', '\t', '\n').filter { it.isNotBlank() }
    val year = words.lastOrNull()?.toIntOrNull()?.takeIf { it in 1000..9999 } ?: return null
    val month = when (words.size) {
        1 -> bareYearMonth
        2 -> monthNumbers[words.first().lowercase()] ?: return null
        else -> return null
    }
    return year * MONTHS_PER_YEAR + month - 1
}

private fun clockMonthIndex(clock: Clock): Int {
    val date = java.time.Instant.ofEpochSecond(clock.now().epochSeconds).atZone(ZoneOffset.UTC)
    return date.year * MONTHS_PER_YEAR + date.monthValue - 1
}

private fun mergedMonthSpan(spans: List<Pair<Int, Int>>): Int {
    var total = 0
    var lastEnd = Int.MIN_VALUE
    for ((start, end) in spans.sortedBy { it.first }) {
        if (start > lastEnd) {
            total += end - start
            lastEnd = end
        } else if (end > lastEnd) {
            total += end - lastEnd
            lastEnd = end
        }
    }
    return total
}
