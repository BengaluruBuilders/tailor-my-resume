package com.tailormyresume.feature.settings.impl.settings

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.settings.impl.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SupportMailTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun blankAddressOpensNothing() {
        assertThat(application.openSupportMail("", "Feedback")).isFalse()
        assertThat(application.openSupportMail("   ", "Feedback")).isFalse()

        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun blankAddressUsesThePrecedentToastText() {
        assertThat(application.getString(R.string.feature_settings_impl_toast_support_missing))
            .isEqualTo("Address not set in this build")
    }

    @Test
    fun setAddressStartsAMailtoSendToIntent() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()

        val opened = activity.openSupportMail(" help@example.com ", "Feedback")

        assertThat(opened).isTrue()
        val started = checkNotNull(shadowOf(activity).nextStartedActivity)
        assertThat(started.action).isEqualTo(Intent.ACTION_SENDTO)
        val data = checkNotNull(started.data)
        assertThat(data.scheme).isEqualTo("mailto")
        assertThat(data.schemeSpecificPart).isEqualTo("help@example.com")
        assertThat(started.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Feedback")
    }
}
