package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfShareIntentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetFileProviderCache() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    private fun exportedFile(): File {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(directory, "Priya-Deshmukh_Resume.pdf").apply { writeText("%PDF-1.4") }
    }

    @Test
    fun openBuildsViewIntentWithReadGrant() {
        val file = exportedFile()

        val intent = createPdfViewIntent(context, file)

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.type).isEqualTo("application/pdf")
        assertThat(intent.data?.authority).isEqualTo("${context.packageName}.tailor.fileprovider")
        assertThat(intent.data?.lastPathSegment).isEqualTo(file.name)
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }

    @Test
    fun shareBuildsChooserAroundSendIntentWithReadGrant() {
        val file = exportedFile()

        val chooser = createPdfShareIntent(context, file, subject = "Resume", chooserTitle = "Share resume")

        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertThat(send?.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send?.type).isEqualTo("application/pdf")
        assertThat(send?.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Resume")
        val stream = send?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertThat(stream?.authority).isEqualTo("${context.packageName}.tailor.fileprovider")
        assertThat(stream?.lastPathSegment).isEqualTo(file.name)
        assertThat((send?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }
}
