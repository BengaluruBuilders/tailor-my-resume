package com.tailormyresume.feature.tailor.impl.exported

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

internal interface ExportedFileStore {
    fun fileFor(fileName: String): File?
}

internal class CacheExportedFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExportedFileStore {

    override fun fileFor(fileName: String): File? {
        if (fileName.isBlank()) return null
        val file = File(File(context.cacheDir, EXPORT_DIRECTORY), fileName)
        return file.takeIf { candidate -> candidate.isFile }
    }

    private companion object {
        const val EXPORT_DIRECTORY = "exports"
    }
}
