package com.tailormyresume.feature.tailor.impl.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfTextLigatureTest {
    @Test
    fun everyTextPaintDisablesLigaturesSoExtractedTextReadsFind() {
        FitPass.entries.forEach { pass ->
            val style = PdfResumeStyle(pass)
            listOf(
                style.name, style.headline, style.contact, style.sectionHeading,
                style.entryTitle, style.entryDetail, style.body,
            ).forEach { paint ->
                assertThat(paint.fontFeatureSettings).contains("\"liga\" 0")
                assertThat(paint.fontFeatureSettings).contains("\"clig\" 0")
            }
        }
    }
}
