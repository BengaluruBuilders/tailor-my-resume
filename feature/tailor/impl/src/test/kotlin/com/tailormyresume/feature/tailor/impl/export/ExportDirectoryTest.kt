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
        val file = ExportDirectory(exportDirectory()).write("app-1", "resume.pdf") { it.write("pdf".toByteArray()) }

        assertThat(file.name).isEqualTo("resume.pdf")
        assertThat(file.readText()).isEqualTo("pdf")
    }

    @Test
    fun write_keepsOtherApplicationsExports() {
        val directory = exportDirectory()
        ExportDirectory(directory).write("app-2", "other.pdf") { it.write(1) }

        ExportDirectory(directory).write("app-1", "new.pdf") { it.write(1) }

        assertThat(File(directory, "app-1").list()?.toList()).containsExactly("new.pdf")
        assertThat(File(directory, "app-2").list()?.toList()).containsExactly("other.pdf")
    }

    @Test
    fun write_removesOnlyThePreviousFileOfThisApplicationAfterTheRename() {
        val directory = exportDirectory()
        val exports = ExportDirectory(directory)
        exports.write("app-2", "old.pdf") { it.write(1) }
        exports.write("app-1", "old.pdf") { it.write(1) }

        exports.write("app-1", "new.pdf", previousFileName = "old.pdf") { it.write(1) }

        assertThat(File(directory, "app-1").list()?.toList()).containsExactly("new.pdf")
        assertThat(File(directory, "app-2").list()?.toList()).containsExactly("old.pdf")
    }

    @Test
    fun write_keepsTheFileWhenThePreviousNameIsTheSame() {
        val directory = exportDirectory()

        val file = ExportDirectory(directory).write("app-1", "same.pdf", previousFileName = "same.pdf") { it.write("pdf".toByteArray()) }

        assertThat(file.readText()).isEqualTo("pdf")
    }

    @Test
    fun write_failureKeepsThePreviousFile() {
        val directory = exportDirectory()
        ExportDirectory(directory).write("app-1", "old.pdf") { it.write(1) }

        val result = runCatching {
            ExportDirectory(directory).write("app-1", "new.pdf", previousFileName = "old.pdf") { throw IOException("disk full") }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(File(directory, "app-1").list()?.toList()).containsExactly("old.pdf")
    }

    @Test
    fun write_replacesAFileWithTheSameName() {
        val directory = ExportDirectory(exportDirectory())
        directory.write("app-1", "resume.pdf") { it.write("first".toByteArray()) }

        val file = directory.write("app-1", "resume.pdf") { it.write("second".toByteArray()) }

        assertThat(file.readText()).isEqualTo("second")
    }

    @Test
    fun write_leavesNoPartialFileWhenTheWriterFails() {
        val directory = exportDirectory()

        val result = runCatching {
            ExportDirectory(directory).write("app-1", "resume.pdf") {
                it.write("partial".toByteArray())
                throw IOException("disk full")
            }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(File(directory, "app-1").list()?.toList()).isEmpty()
    }

    @Test
    fun write_hostileApplicationIdsStayInsideTheExportRoot() {
        val directory = exportDirectory()
        val exports = ExportDirectory(directory)

        listOf("../escape", "..", ".", "a/b", "", "/abs").forEach { id ->
            val file = exports.write(id, "resume.pdf") { it.write(1) }
            assertThat(file.canonicalPath).startsWith(directory.canonicalPath + File.separator)
            assertThat(file.parentFile?.parentFile?.canonicalPath).isEqualTo(directory.canonicalPath)
        }
        assertThat(folder.root.list()?.toList()).containsExactly("exports")
    }

    @Test
    fun existing_findsOnlyTheFileOfThatApplication() {
        val exports = ExportDirectory(exportDirectory())
        exports.write("app-1", "resume.pdf") { it.write(1) }

        assertThat(exports.existing("app-1", "resume.pdf")).isNotNull()
        assertThat(exports.existing("app-2", "resume.pdf")).isNull()
    }
}
