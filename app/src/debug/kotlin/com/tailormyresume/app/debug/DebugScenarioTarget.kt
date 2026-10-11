package com.tailormyresume.app.debug

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.app.R
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import java.util.UUID

const val SAMPLE_APPLICATION_ID = "sample-northwind-associate-analyst"

enum class DebugScenarioTarget(
    @param:StringRes val titleRes: Int,
    val opensFirstRunRoot: Boolean = false,
) {
    SignIn(R.string.debug_target_sign_in, opensFirstRunRoot = true),
    ImportResume(R.string.debug_target_import_resume, opensFirstRunRoot = true),
    Applications(R.string.debug_target_applications),
    ApplicationDetail(R.string.debug_target_application_detail),
    Profile(R.string.debug_target_profile),
    Analysis(R.string.debug_target_analysis),
    Tailor(R.string.debug_target_tailor),
    Exported(R.string.debug_target_exported),
    Credits(R.string.debug_target_credits),
    Settings(R.string.debug_target_settings),
    DeleteAccount(R.string.debug_target_delete_account),
    ;

    val needsSampleJob: Boolean get() = this == Analysis

    val forcesPayment: Boolean get() = this == Credits

    fun navKey(scenario: DebugScenario): NavKey = when (this) {
        SignIn -> SignInNavKey(scenario = scenario)
        ImportResume -> UploadNavKey(scenario = scenario)
        Applications -> ApplicationsNavKey(scenario = scenario)
        ApplicationDetail -> ApplicationDetailNavKey(SAMPLE_APPLICATION_ID, scenario)
        Profile -> ProfileNavKey(scenario = scenario)
        Analysis -> JobResultNavKey(SAMPLE_APPLICATION_ID, scenario)
        Tailor -> TailoringNavKey(SAMPLE_APPLICATION_ID, UUID.randomUUID().toString(), scenario)
        Exported -> ExportedNavKey(applicationId = SAMPLE_APPLICATION_ID, scenario = scenario)
        Credits -> CreditsNavKey(scenario = scenario)
        Settings -> SettingsNavKey(scenario = scenario)
        DeleteAccount -> SettingsNavKey(scenario = scenario)
    }
}
