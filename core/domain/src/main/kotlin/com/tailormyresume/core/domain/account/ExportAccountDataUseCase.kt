package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.account.LocalWorkFence
import javax.inject.Inject

class ExportAccountDataUseCase @Inject constructor(
    private val collectAccountData: CollectAccountDataUseCase,
    private val exporter: AccountDataExporter,
    private val workFence: LocalWorkFence = LocalWorkFence(),
) {
    suspend operator fun invoke(): AccountDataArchive = workFence.protect { exporter.export(collectAccountData()) }
}
