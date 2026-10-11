package com.tailormyresume.app.navigation

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.app.R
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading
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

enum class ChromeAction(@StringRes val label: Int) {
    Save(R.string.shell_action_save),
    Done(R.string.shell_action_done),
}

data class ScreenChrome(
    val step: Int?,
    val topBar: Boolean,
    val leading: TmrTopBarLeading,
    @StringRes val title: Int?,
    val action: ChromeAction?,
)

private val NO_CHROME = ScreenChrome(null, false, TmrTopBarLeading.None, null, null)

private fun bar(
    step: Int? = null,
    leading: TmrTopBarLeading = TmrTopBarLeading.Back,
    @StringRes title: Int? = null,
    action: ChromeAction? = null,
) = ScreenChrome(step, true, leading, title, action)

private fun backWhenNotFirst(entriesBelow: Int): TmrTopBarLeading =
    if (entriesBelow > 0) TmrTopBarLeading.Back else TmrTopBarLeading.None

fun chromeFor(
    key: NavKey,
    entriesBelow: Int,
): ScreenChrome =
    when (key) {
        is UploadNavKey -> bar(step = 1, leading = backWhenNotFirst(entriesBelow))
        is UploadErrorNavKey, is PasteResumeNavKey, is ManualProfileNavKey, is ReviewProfileNavKey -> bar(step = 1)
        is ReadingNavKey -> bar(step = 1, leading = TmrTopBarLeading.None)

        is JobNavKey -> bar(step = 2, leading = backWhenNotFirst(entriesBelow))
        is JobLinkNavKey, is JobResultNavKey -> bar(step = 2)

        is QuickQuestionNavKey, is TailorFailedNavKey, is TailoredNavKey -> bar(step = 3)
        is TailoringNavKey -> bar(step = 3, leading = TmrTopBarLeading.None)

        is EditResumeNavKey -> bar(title = R.string.shell_title_resume, action = ChromeAction.Save)
        is ExportedNavKey, is PaywallNavKey -> bar(leading = TmrTopBarLeading.Close)

        is ApplicationDetailNavKey -> bar(title = R.string.shell_title_application)

        is ExperienceNavKey, is SettingsNavKey -> bar(title = R.string.shell_title_profile)
        is EditRoleNavKey -> bar(title = R.string.shell_title_experience, action = ChromeAction.Save)
        is EditContactNavKey, is ListEditNavKey -> bar(title = R.string.shell_title_profile, action = ChromeAction.Save)
        is SkillsNavKey -> bar(title = R.string.shell_title_profile, action = ChromeAction.Done)
        is CreditsNavKey -> bar(title = R.string.shell_title_settings)

        is SignInNavKey, is ApplicationsNavKey, is ProfileNavKey -> NO_CHROME
        else -> NO_CHROME
    }
