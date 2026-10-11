package com.tailormyresume.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "notnight")
class TmrThemeDarkOnlyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noLightSymbolsRemain() {
        val banned = listOf(
            "isSystemInDarkTheme",
            "TmrLightColors",
            "LightColorScheme",
            "toLightScheme",
            "Manrope",
            "manrope",
            "Archivo",
        )
        val mainDir = File("src/main")
        val sources = mainDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(sources.isNotEmpty())
        sources.forEach { file ->
            val text = file.readText()
            banned.forEach { word ->
                assertFalse(
                    text.contains(word),
                    "${file.path} contains $word",
                )
            }
        }
        assertTrue(File("src/main/res/font").walkTopDown().none { it.isFile })
    }

    @Test
    fun themeProvidesDarkColorsAtSystemLightMode() {
        var tmrBackground: Color? = null
        var materialBackground: Color? = null
        composeRule.setContent {
            TmrTheme {
                tmrBackground = TmrTheme.colors.background
                materialBackground = MaterialTheme.colorScheme.background
            }
        }
        assertEquals(TmrDarkColors.background, tmrBackground)
        assertEquals(DarkColorScheme.background, materialBackground)
    }

    @Test
    fun typographyUsesDefaultFamily() {
        assertEquals(FontFamily.Default, TmrFontFamilies.sans)
        assertEquals(FontFamily.Default, TmrFontFamilies.display)
    }
}
