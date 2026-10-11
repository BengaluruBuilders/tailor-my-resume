package com.tailormyresume.feature.tailor.impl.exported

import android.content.Context
import com.tailormyresume.feature.tailor.impl.export.ExportDirectory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

internal interface ExportedFileStore {
    fun fileFor(applicationId: String, fileName: String): File?
}

internal class CacheExportedFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExportedFileStore {

    override fun fileFor(applicationId: String, fileName: String): File? =
        ExportDirectory(File(context.cacheDir, EXPORT_DIRECTORY)).existing(applicationId, fileName)

    private companion object {
        const val EXPORT_DIRECTORY = "exports"
    }
}
