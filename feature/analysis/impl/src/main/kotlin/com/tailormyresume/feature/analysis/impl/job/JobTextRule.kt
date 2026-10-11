package com.tailormyresume.feature.analysis.impl.job

internal const val MIN_JOB_CHARS = 200

// docs/BACKEND_CONTRACT.md line 237: jobText is at most 20,000 characters.
internal const val MAX_JOB_CHARS = 20_000

internal fun isPlausibleJobPost(text: String): Boolean = text.trim().length >= MIN_JOB_CHARS
