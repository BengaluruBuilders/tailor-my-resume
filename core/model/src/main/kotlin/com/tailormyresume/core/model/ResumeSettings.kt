package com.tailormyresume.core.model

enum class PageSize { A4, LETTER }

enum class FileNameFormat { NAME_COMPANY_ROLE, NAME_ROLE, NAME_RESUME }

data class ResumeSettings(
    val pageSize: PageSize = PageSize.A4,
    val fileNameFormat: FileNameFormat = FileNameFormat.NAME_COMPANY_ROLE,
    val productUpdates: Boolean = false,
)
