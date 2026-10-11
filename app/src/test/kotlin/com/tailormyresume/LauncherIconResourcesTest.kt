package com.tailormyresume

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LauncherIconResourcesTest {

    @Test
    fun foreground_has_no_H_path_and_draws_arrow() {
        val paths = elements("src/main/res/drawable/ic_launcher_foreground.xml", "path")
        assertThat(paths.map { it.getAttribute("android:pathData") })
            .doesNotContain("M38,34h8v14h16v-14h8v40h-8v-18h-16v18h-8z")
        assertThat(paths.map { it.getAttribute("android:pathData") }.any { it.startsWith("M5,13h11.17") })
            .isTrue()
    }

    @Test
    fun background_is_canvas_lime() {
        val colors =
            elements("src/main/res/values/colors.xml", "color")
                .filter { it.getAttribute("name") == "ic_launcher_background" }
        assertThat(colors.map { it.textContent.trim() }).containsExactly("#AEFF00")
    }

    @Test
    fun both_adaptive_icons_declare_monochrome() {
        listOf(
            "src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
            "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
        )
            .forEach { path ->
                val monochrome = elements(path, "monochrome")
                assertThat(monochrome.map { it.getAttribute("android:drawable") })
                    .containsExactly("@drawable/ic_launcher_monochrome")
            }
    }

    @Test
    fun monochrome_drawable_is_single_colour_arrow() {
        val path = "src/main/res/drawable/ic_launcher_monochrome.xml"
        assertThat(File(path).exists()).isTrue()
        val paths = elements(path, "path")
        assertThat(paths).isNotEmpty()
        assertThat(paths.map { it.getAttribute("android:fillColor") }.distinct()).containsExactly("#000000")
        assertThat(paths.map { it.getAttribute("android:pathData") }.any { it.startsWith("M5,13h11.17") })
            .isTrue()
    }

    @Test
    fun api31_splash_icon_background_is_launcher_lime() {
        val items = elements("src/main/res/values/themes.xml", "item")
        assertThat(
            items.filter { it.getAttribute("name") == "android:windowSplashScreenIconBackgroundColor" }
                .map { it.textContent.trim() },
        ).containsExactly("@color/ic_launcher_background")
    }

    @Test
    fun theme_keeps_system_bars_dark() {
        val themeItems =
            elements("src/main/res/values/themes.xml", "item")
                .filter { (it.parentNode as Element).getAttribute("name") == "Theme.TailorMyResume" }
        val byName = themeItems.associate { it.getAttribute("name") to it.textContent.trim() }
        assertThat(byName["android:windowLightStatusBar"]).isEqualTo("false")
        assertThat(byName["android:windowLightNavigationBar"]).isEqualTo("false")
    }

    @Test
    fun theme_window_background_is_window_background_colour() {
        val themeItems =
            elements("src/main/res/values/themes.xml", "item")
                .filter { (it.parentNode as Element).getAttribute("name") == "Theme.TailorMyResume" }
        assertThat(
            themeItems
                .filter { it.getAttribute("name") == "android:windowBackground" }
                .map { it.textContent.trim() },
        ).containsExactly("@color/window_background")
    }

    @Test
    fun window_background_colour_is_black() {
        val colours =
            elements("src/main/res/values/colors.xml", "color")
                .filter { it.getAttribute("name") == "window_background" }
        assertThat(colours.map { it.textContent.trim() }).containsExactly("#000000")
        val paletteSource =
            File("../core/designsystem/src/main/kotlin/com/tailormyresume/core/designsystem/theme/Palette.kt").readText()
        assertThat(paletteSource.lineSequence().first { it.trim().startsWith("background =") })
            .contains("Color(0xFF000000)")
    }

    @Test
    fun res_has_no_values_night_directory() {
        assertThat(File("src/main/res").listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values-night") })
            .isEmpty()
    }

    private fun elements(path: String, tag: String): List<Element> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val document = factory.newDocumentBuilder().parse(File(path))
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }
}
