package com.tailormyresume.feature.onboarding.impl.importresume

import android.content.Context
import android.net.Uri
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal interface ResumeFileResolver {
    suspend fun resolve(uri: Uri): ResumeFile
}

internal class ContentResolverResumeFileResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val dispatcher: CoroutineDispatcher,
) : ResumeFileResolver {
    override suspend fun resolve(uri: Uri): ResumeFile = withContext(dispatcher) { context.resumeFileFor(uri) }
}
