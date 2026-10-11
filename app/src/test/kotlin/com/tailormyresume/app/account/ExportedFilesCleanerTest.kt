package com.tailormyresume.app.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportedFilesCleanerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun cleaner() = CacheExportedFiles(
        { File(folder.root, "cache") },
        { File(folder.root, "files") },
        UnconfinedTestDispatcher(),
    )

    private fun file(root: String, directory: String, name: String) =
        File(folder.newFolder(root, directory), name).apply { writeText("x") }

    @Test
    fun deleteAllRemovesSavedAndCachedExports() = runTest {
        val saved = file("files", "exports", "saved.pdf")
        val resume = file("cache", "exports", "resume.pdf")
        val archive = file("cache", "data-exports", "my-data.zip")

        cleaner().deleteAll()

        assertThat(saved.exists()).isFalse()
        assertThat(resume.exists()).isFalse()
        assertThat(archive.exists()).isFalse()
        assertThat(File(folder.root, "files/exports").exists()).isFalse()
        assertThat(File(folder.root, "cache/exports").exists()).isFalse()
        assertThat(File(folder.root, "cache/data-exports").exists()).isFalse()
    }

    @Test
    fun deleteAllLeavesOtherCacheFilesAlone() = runTest {
        file("cache", "exports", "resume.pdf")
        file("files", "exports", "saved.pdf")
        val other = File(folder.root, "keep.txt").apply { writeText("x") }

        cleaner().deleteAll()

        assertThat(other.exists()).isTrue()
    }

    @Test
    fun deleteAllWithNothingExportedIsHarmless() = runTest {
        cleaner().deleteAll()

        assertThat(folder.root.listFiles()).isEmpty()
    }

    @Test
    fun directoriesAreResolvedWhenCleanupRuns() = runTest {
        val requested = mutableListOf<String>()
        val cleaner = CacheExportedFiles(
            {
                requested += "cache"
                File(folder.root, "cache")
            },
            {
                requested += "files"
                File(folder.root, "files")
            },
            UnconfinedTestDispatcher(),
        )

        assertThat(requested).isEmpty()

        cleaner.deleteAll()

        assertThat(requested).containsExactly("cache", "files").inOrder()
    }
}
