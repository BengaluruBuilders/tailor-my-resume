package com.tailormyresume.feature.onboarding.impl.reading

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.domain.offline.OfflineResumeTextParser
import com.tailormyresume.core.domain.onboarding.SaveImportedProfileUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.feature.onboarding.impl.importresume.MAX_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_READ_LIMIT_BYTES
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeRead
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeTextSource
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModelTest {

    private val file = ResumeFile("cv.pdf", RESUME_PDF_MIME, 2_048L, "content://cv")
    private val text = "Jane Doe. Senior engineer at Acme. Kotlin, SQL, Tailoring."

    private fun entry(
        id: String,
        category: EntryCategory,
        source: FactSource = FactSource.IMPORTED,
    ) = ProfileEntry(
        id = id,
        category = category,
        title = "Title $id",
        organization = "Acme",
        startDate = "2020",
        endDate = "2022",
        bullets = emptyList(),
        source = source,
        isConfirmed = false,
    )

    private fun parsedProfile(
        skills: List<String> = listOf("Kotlin", "SQL", "Tailoring"),
    ) = CandidateProfile(
        fullName = "Jane Doe",
        email = "jane@example.com",
        phone = "+44 700 000 0000",
        headline = "Senior engineer",
        skills = skills,
        entries = listOf(
            entry("e1", EntryCategory.EXPERIENCE),
            entry("e2", EntryCategory.EXPERIENCE),
            entry("e3", EntryCategory.EDUCATION),
            entry("e4", EntryCategory.ACHIEVEMENT),
        ),
    )

    private fun emptyProfile() = CandidateProfile(
        fullName = " ",
        email = "",
        phone = "",
        headline = "",
        skills = emptyList(),
        entries = emptyList(),
    )

    private fun viewModelIn(
        draft: ResumeImportDraft,
        textSource: ResumeTextSource = FakeResumeTextSource(),
        parser: ResumeTextParser = FakeResumeTextParser(parsedProfile()),
        profileRepository: ProfileRepository = TestProfileRepository(),
    ) = ReadingViewModel(
        draft,
        textSource,
        parser,
        SaveImportedProfileUseCase(profileRepository, FactIdAllocator()),
        profileRepository,
        UnconfinedTestDispatcher(),
    )

    private fun draftIn(scope: CoroutineScope) = ResumeImportDraft(TestSessionRepository(), scope)

    @Test
    fun percentFollowsStagesAndRowsMoveToDoneWithCounts() = runTest(UnconfinedTestDispatcher()) {
        val readGate = CompletableDeferred<Unit>()
        val parseGate = CompletableDeferred<Unit>()
        val draft = draftIn(backgroundScope)
        draft.setFile(file)
        val viewModel = viewModelIn(
            draft,
            FakeResumeTextSource(ResumeRead.Text(text), readGate),
            FakeResumeTextParser(parsedProfile(), parseGate),
        )

        val beforeRead = viewModel.uiState.value
        assertThat(beforeRead.percent).isEqualTo(0)
        assertThat(beforeRead.rows.map { it.kind }).containsExactlyElementsIn(ReadingRowKind.entries).inOrder()
        assertThat(beforeRead.rows.map { it.state }).containsExactly(
            ReadingRowState.Active,
            ReadingRowState.Pending,
            ReadingRowState.Pending,
            ReadingRowState.Pending,
            ReadingRowState.Pending,
        ).inOrder()

        readGate.complete(Unit)

        val whileParsing = viewModel.uiState.value
        assertThat(whileParsing.percent).isEqualTo(40)
        assertThat(whileParsing.rows.map { it.state }).containsExactly(
            ReadingRowState.Active,
            ReadingRowState.Active,
            ReadingRowState.Active,
            ReadingRowState.Active,
            ReadingRowState.Active,
        ).inOrder()

        parseGate.complete(Unit)

        val afterSave = viewModel.uiState.value
        assertThat(afterSave.percent).isEqualTo(100)
        assertThat(afterSave.rows.map { it.kind }).containsExactlyElementsIn(ReadingRowKind.entries).inOrder()
        assertThat(afterSave.rows.map { it.state }).containsExactly(
            ReadingRowState.Done,
            ReadingRowState.Done,
            ReadingRowState.Done,
            ReadingRowState.Done,
            ReadingRowState.Done,
        ).inOrder()
        assertThat(afterSave.rows.map { it.count }[0]).isEqualTo(3)
        assertThat(afterSave.rows.map { it.count }.drop(1)).containsExactly(2, 1, 3, 1).inOrder()
        assertThat(afterSave.file).isEqualTo(
            ReadingFileUi(file.displayName, file.mimeType, file.byteSize, 0),
        )
    }

    @Test
    fun experienceCountSkipsEntriesThatContinueThePreviousOne() = runTest(UnconfinedTestDispatcher()) {
        val first = entry("e1", EntryCategory.EXPERIENCE)
        val parsed = parsedProfile().copy(entries = listOf(first, first.copy(id = "e1b")))
        val draft = draftIn(backgroundScope)
        draft.setFile(file)

        val viewModel = viewModelIn(draft, FakeResumeTextSource(ResumeRead.Text(text)), FakeResumeTextParser(parsed))

        assertThat(viewModel.uiState.value.rows.first { it.kind == ReadingRowKind.Experience }.count).isEqualTo(1)
    }

    @Test
    fun contactRowCountsZeroWhenOnlySkillsWereParsed() = runTest(UnconfinedTestDispatcher()) {
        val draft = draftIn(backgroundScope)
        draft.setFile(file)
        val skillsOnly = emptyProfile().copy(skills = listOf("Kotlin"))

        val viewModel = viewModelIn(draft, FakeResumeTextSource(ResumeRead.Text(text)), FakeResumeTextParser(skillsOnly))

        assertThat(viewModel.uiState.value.rows.first { it.kind == ReadingRowKind.Contact }.count).isEqualTo(0)
    }

    @Test
    fun junkSentenceSavesNothingAndShowsTheNeutralLine() = runTest(UnconfinedTestDispatcher()) {
        val repository = TestProfileRepository()
        val draft = draftIn(backgroundScope)
        draft.setText("The quick brown fox jumps over the lazy dog and keeps running.")

        val viewModel = viewModelIn(
            draft,
            parser = OfflineResumeTextParser(),
            profileRepository = repository,
        )

        viewModel.events.test { assertThat(awaitItem()).isEqualTo(ReadingEvent.Failed) }
        assertThat(draft.failure.value?.kind).isEqualTo(UploadFailureKind.Neutral)
        assertThat(repository.observeProfile().first()).isNull()
    }

    @Test
    fun nameOnlyParseSavesNothing() = runTest(UnconfinedTestDispatcher()) {
        val repository = TestProfileRepository()
        val draft = draftIn(backgroundScope)
        draft.setText(text)

        val viewModel = viewModelIn(
            draft,
            parser = FakeResumeTextParser(emptyProfile().copy(fullName = "Jane Doe")),
            profileRepository = repository,
        )

        viewModel.events.test { assertThat(awaitItem()).isEqualTo(ReadingEvent.Failed) }
        assertThat(repository.observeProfile().first()).isNull()
    }

    @Test
    fun successEmitsDoneOnce() = runTest(UnconfinedTestDispatcher()) {
        val readGate = CompletableDeferred<Unit>()
        val draft = draftIn(backgroundScope)
        draft.setFile(file)
        val viewModel = viewModelIn(
            draft,
            FakeResumeTextSource(ResumeRead.Text(text), readGate),
            FakeResumeTextParser(parsedProfile()),
        )

        viewModel.events.test {
            readGate.complete(Unit)

            assertThat(awaitItem()).isEqualTo(ReadingEvent.Done)
            expectNoEvents()
        }
    }

    @Test
    fun everyReadOutcomeAndParseFailureMapsToUnreadable() = runTest(UnconfinedTestDispatcher()) {
        val cases = buildList {
            add(FailureCase(read = ResumeRead.NoTextLayer(file.displayName), kind = UploadFailureKind.ImageOnly))
            add(FailureCase(read = ResumeRead.Empty(file.byteSize), kind = UploadFailureKind.FileProblem))
            add(FailureCase(read = ResumeRead.Unsupported("cv.doc"), kind = UploadFailureKind.FileProblem))
            add(FailureCase(read = ResumeRead.Unreadable(file.displayName), kind = UploadFailureKind.FileProblem))
            add(
                FailureCase(
                    read = ResumeRead.TooLarge(6L * 1024L * 1024L, RESUME_READ_LIMIT_BYTES),
                    kind = UploadFailureKind.TooLarge,
                ),
            )
            add(FailureCase(readThrows = RuntimeException("io"), kind = UploadFailureKind.FileProblem))
            listOf(
                AiFailure.RateLimited,
                AiFailure.QuotaExceeded,
                AiFailure.Network,
                AiFailure.Timeout,
                AiFailure.SignInRequired,
            ).forEach { failure ->
                add(FailureCase(parserThrows = AiException(failure), kind = UploadFailureKind.Neutral))
            }
            add(FailureCase(parserThrows = RuntimeException("boom"), kind = UploadFailureKind.Neutral))
            add(FailureCase(parsed = emptyProfile(), kind = UploadFailureKind.Neutral))
            add(FailureCase(parsed = emptyProfile(), pasted = true, kind = UploadFailureKind.Neutral))
            add(
                FailureCase(
                    repository = ThrowingProfileRepository(),
                    kind = UploadFailureKind.Neutral,
                    fileInFailure = false,
                ),
            )
        }

        cases.forEach { case ->
            val draft = draftIn(backgroundScope)
            val readGate = CompletableDeferred<Unit>()
            if (case.pasted) draft.setText(text) else draft.setFile(file)
            val viewModel = viewModelIn(
                draft,
                FakeResumeTextSource(case.read, readGate, case.readThrows),
                FakeResumeTextParser(case.parsed, error = case.parserThrows),
                case.repository,
            )

            viewModel.events.test {
                readGate.complete(Unit)

                assertThat(awaitItem()).isEqualTo(ReadingEvent.Failed)
                expectNoEvents()
            }

            val failure = draft.failure.value
            assertThat(failure?.kind).isEqualTo(case.kind)
            if (case.fileInFailure) {
                assertThat(failure).isEqualTo(
                    UploadFailure(
                        kind = case.kind,
                        fileName = if (case.pasted) null else file.displayName,
                        mimeType = if (case.pasted) null else file.mimeType,
                        byteSize = if (case.pasted) 0L else file.byteSize,
                    ),
                )
            }
        }
    }

    @Test
    fun draftClearedAfterSuccessAndAfterFailure() = runTest(UnconfinedTestDispatcher()) {
        val successDraft = draftIn(backgroundScope)
        successDraft.setFile(file)
        viewModelIn(successDraft)

        assertThat(successDraft.source.value).isNull()
        assertThat(successDraft.failure.value).isNull()

        val failingDraft = draftIn(backgroundScope)
        failingDraft.setFile(file)
        viewModelIn(
            failingDraft,
            FakeResumeTextSource(ResumeRead.NoTextLayer(file.displayName)),
        )

        assertThat(failingDraft.source.value).isNull()
        assertThat(failingDraft.failure.value?.kind).isEqualTo(UploadFailureKind.ImageOnly)
    }

    @Test
    fun parserReceivesOnlyTextNotFile() = runTest(UnconfinedTestDispatcher()) {
        val repository = TestProfileRepository()
        repository.sendProfile(
            CandidateProfile(
                fullName = "Existing",
                email = "",
                phone = "",
                headline = "",
                skills = emptyList(),
                entries = listOf(
                    entry("i1", EntryCategory.EXPERIENCE),
                    entry("i2", EntryCategory.EDUCATION),
                    entry("u1", EntryCategory.EXPERIENCE, FactSource.USER_STATED),
                    entry("u2", EntryCategory.EDUCATION, FactSource.USER_ANSWER),
                    entry("u3", EntryCategory.PROJECT, FactSource.USER_EDITED),
                ),
            ),
        )
        val draft = draftIn(backgroundScope)
        draft.setFile(file)
        val textSource = FakeResumeTextSource(ResumeRead.Text(text))
        val parser = FakeResumeTextParser(parsedProfile())

        viewModelIn(draft, textSource, parser, repository)

        assertThat(parser.rawText).isEqualTo(text)
        assertThat(parser.keptEntries).isEqualTo(3)
        assertThat(textSource.lastFile).isEqualTo(file)
    }

    @Test
    fun emptyDraftEmitsNothingToRead() = runTest(UnconfinedTestDispatcher()) {
        val draft = draftIn(backgroundScope)
        val viewModel = viewModelIn(draft)

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(ReadingEvent.NothingToRead)
        }
        assertThat(draft.source.value).isNull()
        assertThat(draft.failure.value).isNull()
    }

    @Test
    fun longTextIsTruncatedToTheCap() = runTest(UnconfinedTestDispatcher()) {
        val long = "a".repeat(MAX_RESUME_CHARS + 500)

        val fileDraft = draftIn(backgroundScope)
        fileDraft.setFile(file)
        val fileParser = FakeResumeTextParser(parsedProfile())
        viewModelIn(
            fileDraft,
            FakeResumeTextSource(ResumeRead.Text(long)),
            fileParser,
        )

        assertThat(fileParser.rawText).hasLength(MAX_RESUME_CHARS)
        assertThat(fileParser.rawText).isEqualTo(long.take(MAX_RESUME_CHARS))

        val pastedDraft = draftIn(backgroundScope)
        pastedDraft.setText(long)
        val pastedParser = FakeResumeTextParser(parsedProfile())
        viewModelIn(pastedDraft, parser = pastedParser)

        assertThat(pastedParser.rawText).hasLength(MAX_RESUME_CHARS)
        assertThat(pastedParser.rawText).isEqualTo(long.take(MAX_RESUME_CHARS))
    }

    @Test
    fun pastedTextSkipsTheFileRead() = runTest(UnconfinedTestDispatcher()) {
        val draft = draftIn(backgroundScope)
        draft.setText(text)
        val textSource = FakeResumeTextSource()
        val parser = FakeResumeTextParser(parsedProfile())

        val viewModel = viewModelIn(draft, textSource, parser)

        assertThat(textSource.readCount).isEqualTo(0)
        assertThat(parser.rawText).isEqualTo(text)
        assertThat(viewModel.uiState.value.file).isEqualTo(
            ReadingFileUi(name = null, mimeType = null, byteSize = 0L, characters = text.length),
        )
    }

    @Test
    fun fileNameIsSavedOnTheProfile() = runTest(UnconfinedTestDispatcher()) {
        val pickedRepository = TestProfileRepository()
        val pickedDraft = draftIn(backgroundScope)
        pickedDraft.setFile(file)
        viewModelIn(pickedDraft, profileRepository = pickedRepository)

        assertThat(pickedRepository.observeProfile().first()?.sourceFileName).isEqualTo(file.displayName)

        val pastedRepository = TestProfileRepository()
        val pastedDraft = draftIn(backgroundScope)
        pastedDraft.setText(text)
        viewModelIn(pastedDraft, profileRepository = pastedRepository)

        assertThat(pastedRepository.observeProfile().first()?.sourceFileName).isNull()
    }

    private data class FailureCase(
        val read: ResumeRead = ResumeRead.Text("Jane Doe. Senior engineer at Acme."),
        val readThrows: Throwable? = null,
        val parserThrows: Throwable? = null,
        val parsed: CandidateProfile = CandidateProfile(
            fullName = "Jane Doe",
            email = "jane@example.com",
            phone = "+44 700 000 0000",
            headline = "Senior engineer",
            skills = listOf("Kotlin"),
            entries = listOf(
                ProfileEntry(
                    id = "e1",
                    category = EntryCategory.EXPERIENCE,
                    title = "Engineer",
                    organization = "Acme",
                    startDate = "2020",
                    endDate = "2022",
                    bullets = emptyList(),
                    source = FactSource.IMPORTED,
                    isConfirmed = false,
                ),
            ),
        ),
        val repository: ProfileRepository = TestProfileRepository(),
        val pasted: Boolean = false,
        val kind: UploadFailureKind,
        val fileInFailure: Boolean = true,
    )

    private class FakeResumeTextSource(
        private val read: ResumeRead = ResumeRead.Text(""),
        private val gate: CompletableDeferred<Unit>? = null,
        private val error: Throwable? = null,
    ) : ResumeTextSource {
        var readCount = 0
        var lastFile: ResumeFile? = null

        override suspend fun read(file: ResumeFile): ResumeRead {
            readCount++
            lastFile = file
            gate?.await()
            error?.let { throw it }
            return read
        }
    }

    private class FakeResumeTextParser(
        private val profile: CandidateProfile,
        private val gate: CompletableDeferred<Unit>? = null,
        private val error: Throwable? = null,
    ) : ResumeTextParser {
        var rawText: String? = null
        var keptEntries: Int? = null

        override suspend fun parse(rawText: String): CandidateProfile = record(rawText, 0)

        override suspend fun parse(rawText: String, keptEntries: Int): CandidateProfile =
            record(rawText, keptEntries)

        private suspend fun record(text: String, kept: Int): CandidateProfile {
            rawText = text
            keptEntries = kept
            gate?.await()
            error?.let { throw it }
            return profile
        }
    }

    private class ThrowingProfileRepository : ProfileRepository {
        override fun observeProfile(): Flow<CandidateProfile?> = flowOf(null)

        override suspend fun saveProfile(profile: CandidateProfile) {
            throw RuntimeException("save failed")
        }

        override suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean = error("save failed")

        override suspend fun clearProfile() = Unit
    }
}
