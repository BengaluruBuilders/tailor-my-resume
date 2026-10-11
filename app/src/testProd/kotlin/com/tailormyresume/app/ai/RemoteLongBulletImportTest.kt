package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.hasTooLongBullet
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteLongBulletImportTest {
    private val backend = FakeBackend()

    @After
    fun tearDown() = backend.shutdown()

    private fun sentence(length: Int): String = "S" + "a".repeat(length - 2) + "."

    private fun response(bullet: String): String =
        """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,"skills":[],
"entries":[{"ref":"e1","category":"EXPERIENCE","title":"Intern","organization":null,"startDate":null,"endDate":null,
"bullets":[{"ref":"e1b1","text":"$bullet"}]}]},"droppedSensitive":[]}"""

    @Test
    fun overLimitBulletOfThreeSentencesIsSplitIntoThree() = runBlocking<Unit> {
        val original = listOf(sentence(299), sentence(299), sentence(300)).joinToString(" ")
        backend.reply(200, response(original))

        val entry = RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("t").entries.single()

        assertThat(entry.bullets).hasSize(3)
        assertThat(entry.bullets.joinToString(" ") { it.text }).isEqualTo(original)
        assertThat(entry.hasTooLongBullet).isFalse()
    }

    @Test
    fun overLimitSingleSentenceIsKeptWholeAndFlagged() = runBlocking<Unit> {
        val original = sentence(450)
        backend.reply(200, response(original))

        val entry = RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("t").entries.single()

        assertThat(entry.bullets.map { it.text }).containsExactly(original)
        assertThat(entry.hasTooLongBullet).isTrue()
    }
}
