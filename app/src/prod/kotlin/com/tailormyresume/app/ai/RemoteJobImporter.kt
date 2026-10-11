package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.dto.JobImportRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class RemoteJobImporter @Inject constructor(private val api: TailorMyResumeApi) : JobImporter {
    override suspend fun import(url: String): ImportedJob {
        val link = url.trim()
        if (link.length > MAX_URL_LENGTH || link.toHttpUrlOrNull()?.isHttps != true) {
            throw AiException(AiFailure.InvalidInput)
        }
        val response = remoteAi { api.jobImport(JobImportRequest(link)) }
        return ImportedJob(response.jobText, response.sourceHost)
    }

    private companion object {
        const val MAX_URL_LENGTH = 2_048
    }
}
