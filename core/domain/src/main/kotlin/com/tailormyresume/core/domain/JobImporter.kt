package com.tailormyresume.core.domain

import javax.inject.Inject

data class ImportedJob(val jobText: String, val sourceHost: String)

interface JobImporter {
    suspend fun import(url: String): ImportedJob
}

class OfflineJobImporter @Inject constructor() : JobImporter {
    override suspend fun import(url: String): ImportedJob = throw AiException(AiFailure.Network)
}
