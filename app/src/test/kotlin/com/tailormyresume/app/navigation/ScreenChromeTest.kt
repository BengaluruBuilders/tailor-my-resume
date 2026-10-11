package com.tailormyresume.app.navigation

import android.content.Context
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading.Back
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading.Close
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading.None
import com.tailormyresume.feature.analysis.api.navigation.JobLinkNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ManualProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadErrorNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.ListEditNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailorFailedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_ID = "app-1"

internal data class ChromeRow(
    val key: NavKey,
    val step: Int?,
    val topBar: Boolean,
    val leading: TmrTopBarLeading,
    val title: String?,
    val action: String?,
)

internal val CHROME_TABLE: List<ChromeRow> = listOf(
    ChromeRow(SignInNavKey(), null, false, None, null, null),
    ChromeRow(UploadNavKey(), 1, true, Back, null, null),
    ChromeRow(UploadErrorNavKey(), 1, true, Back, null, null),
    ChromeRow(PasteResumeNavKey(), 1, true, Back, null, null),
    ChromeRow(ManualProfileNavKey(), 1, true, Back, null, null),
    ChromeRow(ReadingNavKey(), 1, true, None, null, null),
    ChromeRow(ReviewProfileNavKey(), 1, true, Back, null, null),
    ChromeRow(JobNavKey(), 2, true, Back, null, null),
    ChromeRow(JobLinkNavKey(), 2, true, Back, null, null),
    ChromeRow(JobResultNavKey(APP_ID), 2, true, Back, null, null),
    ChromeRow(QuickQuestionNavKey(APP_ID), 3, true, Back, null, null),
    ChromeRow(TailoringNavKey(APP_ID, "run-1"), 3, true, None, null, null),
    ChromeRow(TailorFailedNavKey(APP_ID, "run-1"), 3, true, Back, null, null),
    ChromeRow(TailoredNavKey(APP_ID), 3, true, Back, null, null),
    ChromeRow(EditResumeNavKey(APP_ID), null, true, Back, "Resume", "Save"),
    ChromeRow(ExportedNavKey(APP_ID), null, true, Close, null, null),
    ChromeRow(ApplicationsNavKey(), null, false, None, null, null),
    ChromeRow(ApplicationDetailNavKey(APP_ID), null, true, Back, "Application", null),
    ChromeRow(ProfileNavKey(), null, false, None, null, null),
    ChromeRow(ExperienceNavKey(), null, true, Back, "Profile", null),
    ChromeRow(EditRoleNavKey(null), null, true, Back, "Experience", "Save"),
    ChromeRow(EditContactNavKey(), null, true, Back, "Profile", "Save"),
    ChromeRow(SkillsNavKey(), null, true, Back, "Profile", "Done"),
    ChromeRow(ListEditNavKey(ProfileListSection.SUMMARY), null, true, Back, "Profile", "Save"),
    ChromeRow(SettingsNavKey(), null, true, Back, "Profile", null),
    ChromeRow(CreditsNavKey(), null, true, Back, "Settings", null),
    ChromeRow(PaywallNavKey(null), null, true, Close, null, null),
)

@RunWith(AndroidJUnit4::class)
class ScreenChromeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun chromeMatchesTTableForAll27Keys() {
        assertThat(CHROME_TABLE).hasSize(27)
        assertThat(CHROME_TABLE.map { it.key::class }).containsNoDuplicates()

        CHROME_TABLE.forEach { row ->
            val chrome = chromeFor(row.key, entriesBelow = 1)
            val label = row.key::class.simpleName
            assertWithMessage("$label step").that(chrome.step).isEqualTo(row.step)
            assertWithMessage("$label topBar").that(chrome.topBar).isEqualTo(row.topBar)
            assertWithMessage("$label leading").that(chrome.leading).isEqualTo(row.leading)
            assertWithMessage("$label title").that(chrome.title?.let(context::getString)).isEqualTo(row.title)
            assertWithMessage("$label action").that(chrome.action?.label?.let(context::getString)).isEqualTo(row.action)
        }
    }

    @Test
    fun uploadAndJobHideBackOnEmptyStackAndShowItOtherwise() {
        listOf<NavKey>(UploadNavKey(), JobNavKey()).forEach { key ->
            assertThat(chromeFor(key, entriesBelow = 0).leading).isEqualTo(None)
            assertThat(chromeFor(key, entriesBelow = 1).leading).isEqualTo(Back)
        }
        assertThat(chromeFor(UploadErrorNavKey(), entriesBelow = 0).leading).isEqualTo(Back)
        assertThat(chromeFor(JobLinkNavKey(), entriesBelow = 0).leading).isEqualTo(Back)
    }
}
