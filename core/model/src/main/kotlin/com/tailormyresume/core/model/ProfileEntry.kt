package com.tailormyresume.core.model

enum class EntryCategory { EDUCATION, EXPERIENCE, PROJECT, CERTIFICATION, ACHIEVEMENT }

enum class FactSource { IMPORTED, USER_STATED, USER_EDITED, USER_ANSWER }

data class EvidenceBullet(val id: String, val text: String)

data class ProfileEntry(
    val id: String,
    val category: EntryCategory,
    val title: String,
    val organization: String,
    val startDate: String,
    val endDate: String,
    val bullets: List<EvidenceBullet>,
    val source: FactSource,
    val isConfirmed: Boolean,
)
