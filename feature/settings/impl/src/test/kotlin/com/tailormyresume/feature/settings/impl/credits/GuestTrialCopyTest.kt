package com.tailormyresume.feature.settings.impl.credits

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.settings.impl.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class GuestTrialCopyTest {
    @Test fun trialAmountsUseSingularAndPluralCopyIncludingZero() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        listOf(R.plurals.feature_settings_impl_trial_offer, R.plurals.feature_settings_impl_trial_claim, R.plurals.feature_settings_impl_trial_granted).forEach { resource ->
            assertThat(resources.getQuantityString(resource, 1, 1)).containsMatch("1 (trial )?credit")
            assertThat(resources.getQuantityString(resource, 1, 1)).doesNotContainMatch("1 (trial )?credits")
            assertThat(resources.getQuantityString(resource, 2, 2)).containsMatch("2 (trial )?credits")
            assertThat(resources.getQuantityString(resource, 0, 0)).containsMatch("0 (trial )?credits")
        }
    }
}
