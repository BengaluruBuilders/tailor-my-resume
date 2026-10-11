package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.model.hasTooManyBullets
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteBulletCapImportTest {
    private val backend = FakeBackend()

    @After
    fun tearDown() = backend.shutdown()

    private fun entryJson(ref: String, bullets: Int): String {
        val lines = (1..bullets).joinToString(",") { """{"ref":"${ref}b$it","text":"Task $it done."}""" }
        return """{"ref":"$ref","category":"EXPERIENCE","title":"Intern","organization":"Acme","startDate":"2024","endDate":"2025","bullets":[$lines]}"""
    }

    private fun response(vararg entries: String): String =
        """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,"skills":[],"entries":[${entries.joinToString(",")}]},"droppedSensitive":[]}"""

    private fun parse() = runBlocking {
        RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("t").entries
    }

    @Test
    fun twentyBulletsImportAsFifteenAndFiveInOrder() {
        backend.reply(200, response(entryJson("e1", 20)))

        val entries = parse()

        assertThat(entries.map { it.bullets.size }).containsExactly(15, 5).inOrder()
        assertThat(entries.flatMap { it.bullets }.map { it.text })
            .containsExactlyElementsIn((1..20).map { "Task $it done." }).inOrder()
        assertThat(entries.map { it.title }.toSet()).containsExactly("Intern")
        assertThat(entries.map { it.id }.toSet()).hasSize(2)
        assertThat(entries.none { it.hasTooManyBullets }).isTrue()
    }

    @Test
    fun continuationNeverPushesTheProfileOverFortyEntries() {
        val others = (1..39).map { entryJson("o$it", 1) }
        backend.reply(200, response(*others.toTypedArray(), entryJson("big", 20)))

        val entries = parse()

        assertThat(entries).hasSize(ProfileLimits.MAX_ENTRIES)
        assertThat(entries.last().bullets).hasSize(20)
        assertThat(entries.last().hasTooManyBullets).isTrue()
    }
}
