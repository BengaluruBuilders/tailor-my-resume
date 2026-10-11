package com.tailormyresume.feature.onboarding.impl.importresume

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ResumeFileDescriptorTest {

    @Test
    fun queryForNameAndSizeRunsOffTheMainThread() {
        val provider = Robolectric.setupContentProvider(RecordingProvider::class.java, AUTHORITY)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val file = runBlocking { ContentResolverResumeFileResolver(context, Dispatchers.IO).resolve(Uri.parse("content://$AUTHORITY/cv.pdf")) }

        assertThat(provider.queriedOnMainThread).isFalse()
        assertThat(file.displayName).isEqualTo("cv.pdf")
        assertThat(file.byteSize).isEqualTo(2_048L)
    }

    class RecordingProvider : ContentProvider() {
        @Volatile
        var queriedOnMainThread: Boolean? = null

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
            queriedOnMainThread = Looper.myLooper() == Looper.getMainLooper()
            return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply { addRow(arrayOf<Any>("cv.pdf", 2_048L)) }
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0

        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    }

    private companion object {
        const val AUTHORITY = "test.resume"
    }
}
