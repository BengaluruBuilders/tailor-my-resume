package com.tailormyresume.feature.onboarding.impl.reading

internal enum class ReadingRowKind { Contact, Experience, Education, Skills, Achievements }

internal enum class ReadingRowState { Pending, Active, Done }

internal data class ReadingRowUi(
    val kind: ReadingRowKind,
    val state: ReadingRowState,
    val count: Int? = null,
)

internal data class ReadingFileUi(
    val name: String?,
    val mimeType: String?,
    val byteSize: Long,
    val characters: Int,
)

internal data class ReadingUiState(
    val file: ReadingFileUi,
    val percent: Int,
    val rows: List<ReadingRowUi>,
)

internal enum class ReadingEvent { Done, Failed, NothingToRead }
