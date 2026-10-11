package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class JobImportRequest(val url: String)

@Serializable
data class JobImportResponse(val jobText: String, val sourceHost: String)
