package com.tailormyresume.core.domain.fact

import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.ProfileEntry
import java.util.UUID
import javax.inject.Inject

class FactIdAllocator @Inject constructor() {
    fun nextId(category: EntryCategory, existing: Collection<ProfileEntry>, title: String = ""): String =
        nextId(prefixOf(category, title), existing)

    fun neverUsedId(category: EntryCategory, taken: Collection<ProfileEntry>, title: String = ""): String {
        val takenIds = taken.mapTo(mutableSetOf()) { it.id }
        val prefix = prefixOf(category, title)
        while (true) {
            val id = prefix + "-" + UUID.randomUUID().toString().take(UNIQUE_SUFFIX_LENGTH)
            if (id !in takenIds) return id
        }
    }

    fun nextUncategorisedId(existing: Collection<ProfileEntry>): String =
        nextId(FactIdPrefix.UNCATEGORISED, existing)

    private fun nextId(prefix: String, existing: Collection<ProfileEntry>): String {
        val taken = existing.map { it.id }.toSet()
        var index = 1
        while (idOf(prefix, index) in taken) {
            index++
        }
        return idOf(prefix, index)
    }

    private fun idOf(prefix: String, index: Int): String = prefix + "-" + index.toString().padStart(2, '0')

    private fun prefixOf(category: EntryCategory, title: String): String = FactIdPrefix.of(category, title)
}

private const val UNIQUE_SUFFIX_LENGTH = 8

object FactIdPrefix {
    const val EDUCATION = "E"
    const val WORK = "W"
    const val INTERNSHIP = "I"
    const val PROJECT = "P"
    const val CERTIFICATION = "CT"
    const val ACHIEVEMENT = "X"
    const val UNCATEGORISED = "U"

    fun of(category: EntryCategory, title: String = ""): String = when (category) {
        EntryCategory.EDUCATION -> EDUCATION
        EntryCategory.EXPERIENCE -> if (title.contains("intern", ignoreCase = true)) INTERNSHIP else WORK
        EntryCategory.PROJECT -> PROJECT
        EntryCategory.CERTIFICATION -> CERTIFICATION
        EntryCategory.ACHIEVEMENT -> ACHIEVEMENT
    }
}
