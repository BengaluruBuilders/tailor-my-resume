package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeUserDto(val id: String, val createdAt: String)

@Serializable
data class MeResponse(val user: MeUserDto)

@Serializable
data class DeletionDto(val status: String)

@Serializable
data class DeletionResponse(val deletion: DeletionDto)
