package com.tailormyresume.core.designsystem.theme

import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TmrFontResourcesTest {
    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun fontFilesAndLicencesExist() {
        val fonts = mapOf(
            "space_mono_regular.ttf" to 99356L,
            "space_mono_bold.ttf" to 98232L,
            "space_grotesk.ttf" to 136676L,
        )
        fonts.forEach { (name, size) ->
            val file = File("src/main/res/font/$name")
            assertTrue(file.isFile, "$name missing")
            assertEquals(size, file.length(), name)
        }
        assertEquals(
            3,
            File("src/main/res/font").listFiles().orEmpty().count { it.isFile },
        )
        val licences = mapOf(
            "SpaceMono-OFL.txt" to "8e4ee42b2553e1e01504e61cb0d46d148cd8c9e5eacaa3622a7df2d4f2955b9f",
            "SpaceGrotesk-OFL.txt" to "c6dec685825f73b18c20926fddc65e8315642e12986f15db0699170940a09efc",
        )
        licences.forEach { (name, hash) ->
            val file = File("fonts-licenses/$name")
            assertTrue(file.isFile, "$name missing")
            assertEquals(hash, sha256(file), name)
        }
    }
}
