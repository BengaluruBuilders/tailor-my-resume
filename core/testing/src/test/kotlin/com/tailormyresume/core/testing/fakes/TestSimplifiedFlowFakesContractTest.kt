package com.tailormyresume.core.testing.fakes

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.testing.repository.CreditsRepositoryContractTest
import com.tailormyresume.core.testing.repository.ResumeSettingsRepositoryContractTest
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository

class TestCreditsRepositoryContractTest : CreditsRepositoryContractTest() {
    override fun createCreditsRepository(): CreditsRepository = TestCreditsRepository()
}

class TestResumeSettingsRepositoryContractTest : ResumeSettingsRepositoryContractTest() {
    override fun createResumeSettingsRepository(): ResumeSettingsRepository = TestResumeSettingsRepository()
}
