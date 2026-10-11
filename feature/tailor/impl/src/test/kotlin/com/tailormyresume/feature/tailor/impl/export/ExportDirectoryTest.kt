package com.tailormyresume.feature.tailor.impl.export

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ExportDirectoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun exportDirectory(): File = File(folder.root, "exports")

    @Test
    fun write_createsTheFileWithTheContent() {
        val file = ExportDirectory(exportDirectory()).write("resume.pdf") { it.write("pdf".toByteArray()) }

        assertThat(file.name).isEqualTo("resume.pdf")
        assertThat(file.readText()).isEqualTo("pdf")
    }

    @Test
    fun write_keepsOtherApplicationsExports() {
        val directory = exportDirectory().also { it.mkdirs() }
        File(directory, "other.pdf").writeText("other")

        ExportDirectory(directory).write("new.pdf") { it.write(1) }

        assertThat(directory.list()?.toList()).containsExactly("other.pdf", "new.pdf")
    }

    @Test
    fun write_removesOnlyThePreviousFileOfThisApplicationAfterTheRename() {
        val directory = exportDirectory().also { it.mkdirs() }
        File(directory, "other.pdf").writeText("other")
        File(directory, "old.pdf").writeText("old")

        ExportDirectory(directory).write("new.pdf", previousFileName = "old.pdf") { it.write(1) }

        assertThat(directory.list()?.toList()).containsExactly("other.pdf", "new.pdf")
    }

    @Test
    fun write_keepsTheFileWhenThePreviousNameIsTheSame() {
        val directory = exportDirectory()

        val file = ExportDirectory(directory).write("same.pdf", previousFileName = "same.pdf") { it.write("pdf".toByteArray()) }

        assertThat(file.readText()).isEqualTo("pdf")
    }

    @Test
    fun write_failureKeepsThePreviousFile() {
        val directory = exportDirectory().also { it.mkdirs() }
        File(directory, "old.pdf").writeText("old")

        val result = runCatching {
            ExportDirectory(directory).write("new.pdf", previousFileName = "old.pdf") { throw IOException("disk full") }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(directory.list()?.toList()).containsExactly("old.pdf")
    }

    @Test
    fun write_replacesAFileWithTheSameName() {
        val directory = ExportDirectory(exportDirectory())
        directory.write("resume.pdf") { it.write("first".toByteArray()) }

        val file = directory.write("resume.pdf") { it.write("second".toByteArray()) }

        assertThat(file.readText()).isEqualTo("second")
    }

    @Test
    fun write_leavesNoPartialFileWhenTheWriterFails() {
        val directory = exportDirectory()

        val result = runCatching {
            ExportDirectory(directory).write("resume.pdf") {
                it.write("partial".toByteArray())
                throw IOException("disk full")
            }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(directory.list()?.toList()).isEmpty()
    }
}
