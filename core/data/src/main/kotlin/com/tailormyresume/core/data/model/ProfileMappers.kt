package com.tailormyresume.core.data.model

import com.tailormyresume.core.database.json.EvidenceBulletDto
import com.tailormyresume.core.database.model.PopulatedProfile
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.database.model.ProfileEntryEntity
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.ProfileEntry

fun EvidenceBullet.asDto() = EvidenceBulletDto(id = id, text = text)

fun EvidenceBulletDto.asExternalModel() = EvidenceBullet(id = id, text = text)

fun CandidateProfile.asEntity() = ProfileEntity(
    fullName = fullName,
    email = email,
    phone = phone,
    headline = headline,
    skills = skills,
    userStatedSkills = userStatedSkills,
    city = city,
    linkedinUrl = linkedinUrl,
    portfolioUrl = portfolioUrl,
    summary = summary,
    sourceFileName = sourceFileName,
    reviewedAt = reviewedAt,
)

fun ProfileEntry.asEntity(position: Int) = ProfileEntryEntity(
    id = id,
    position = position,
    category = category,
    title = title,
    organization = organization,
    startDate = startDate,
    endDate = endDate,
    bullets = bullets.map(EvidenceBullet::asDto),
    source = source,
    isConfirmed = isConfirmed,
)

fun CandidateProfile.asEntryEntities(): List<ProfileEntryEntity> =
    entries.mapIndexed { index, entry -> entry.asEntity(position = index) }

fun ProfileEntryEntity.asExternalModel() = ProfileEntry(
    id = id,
    category = category,
    title = title,
    organization = organization,
    startDate = startDate,
    endDate = endDate,
    bullets = bullets.map(EvidenceBulletDto::asExternalModel),
    source = source,
    isConfirmed = isConfirmed,
)

fun PopulatedProfile.asExternalModel() = CandidateProfile(
    fullName = profile.fullName,
    email = profile.email,
    phone = profile.phone,
    headline = profile.headline,
    skills = profile.skills,
    userStatedSkills = profile.userStatedSkills,
    city = profile.city,
    linkedinUrl = profile.linkedinUrl,
    portfolioUrl = profile.portfolioUrl,
    summary = profile.summary,
    sourceFileName = profile.sourceFileName,
    reviewedAt = profile.reviewedAt,
    entries = entries
        .sortedBy(ProfileEntryEntity::position)
        .map(ProfileEntryEntity::asExternalModel),
)
