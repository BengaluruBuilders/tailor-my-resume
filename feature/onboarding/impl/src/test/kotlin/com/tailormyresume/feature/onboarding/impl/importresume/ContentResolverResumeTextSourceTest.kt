package com.tailormyresume.feature.onboarding.impl.importresume

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ContentResolverResumeTextSourceTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val uri = Uri.parse("content://test.resume/big.pdf")

    @Test
    fun overLimitFileWithUnknownSizeIsTooLargeNotParsedTruncated() {
        val oversize = RESUME_READ_LIMIT_BYTES + 1
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArray(oversize.toInt()).inputStream())
        val file = ResumeFile("big.pdf", RESUME_PDF_MIME, 0L, uri.toString())

        val read = runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }

        assertThat(read).isEqualTo(ResumeRead.TooLarge(oversize, RESUME_READ_LIMIT_BYTES))
    }
}
