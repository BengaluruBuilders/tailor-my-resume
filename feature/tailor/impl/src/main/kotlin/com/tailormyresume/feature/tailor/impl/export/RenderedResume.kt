package com.tailormyresume.feature.tailor.impl.export

import java.io.File

internal data class RenderedResume(
    val file: File,
    val pageCount: Int,
    val lines: List<String>,
    val sizeBytes: Long = file.length(),
)
