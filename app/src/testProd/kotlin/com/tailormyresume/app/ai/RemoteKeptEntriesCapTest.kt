package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.hasTooManyBullets
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteKeptEntriesCapTest {
    private val backend = FakeBackend()

    @After
    fun tearDown() = backend.shutdown()

    private fun entryJson(ref: String, bullets: Int): String {
        val lines = (1..bullets).joinToString(",") { """{"ref":"${ref}b$it","text":"Task $it done."}""" }
        return """{"ref":"$ref","category":"EXPERIENCE","title":"Role $ref","organization":"Acme","startDate":"2024","endDate":"2025","bullets":[$lines]}"""
    }

    private fun parse(keptEntries: Int) = runBlocking {
        val entries = (1..37).map { entryJson("o$it", 1) } + entryJson("big", 20)
        backend.reply(
            200,
            """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,"skills":[],"entries":[${entries.joinToString(",")}]},"droppedSensitive":[]}""",
        )
        RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("t", keptEntries).entries
    }

    @Test
    fun entryIsSplitWhenTheWholeProfileStillFitsFortyEntries() {
        val entries = parse(keptEntries = 0)

        assertThat(entries).hasSize(39)
    }

    @Test
    fun keptEntriesCountAgainstTheSplitSoTheProfileNeverPassesForty() {
        val entries = parse(keptEntries = 3)

        assertThat(entries).hasSize(38)
        assertThat(entries.last().bullets).hasSize(20)
        assertThat(entries.last().hasTooManyBullets).isTrue()
    }
}
