package com.tailormyresume.feature.tailor.impl.export

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URLEncoder

internal class ExportDirectory(private val root: File) {

    fun write(
        applicationId: String,
        fileName: String,
        previousFileName: String? = null,
        writeContent: (OutputStream) -> Unit,
    ): File {
        val directory = folderOf(applicationId).also { it.mkdirs() }
        val target = File(directory, fileName)
        val temporary = File(directory, "$fileName$TEMPORARY_SUFFIX")
        try {
            temporary.outputStream().use(writeContent)
            if (!temporary.renameTo(target)) throw IOException("Could not move the export to ${target.name}")
        } finally {
            temporary.delete()
        }
        previousFileName?.takeIf { it != fileName }?.let { File(directory, File(it).name).delete() }
        return target
    }

    fun existing(applicationId: String, fileName: String): File? {
        if (fileName.isBlank()) return null
        return File(folderOf(applicationId), File(fileName).name).takeIf { it.isFile }
    }

    private fun folderOf(applicationId: String): File = File(root, segmentOf(applicationId))

    private fun segmentOf(applicationId: String): String =
        URLEncoder.encode(applicationId, Charsets.UTF_8.name()).replace(".", "%2E").ifEmpty { "%" }

    private companion object {
        const val TEMPORARY_SUFFIX = ".tmp"
    }
}
