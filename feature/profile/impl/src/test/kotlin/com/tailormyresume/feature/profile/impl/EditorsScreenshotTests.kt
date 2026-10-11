package com.tailormyresume.feature.profile.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.impl.contact.EditContactActions
import com.tailormyresume.feature.profile.impl.contact.EditContactScreen
import com.tailormyresume.feature.profile.impl.listedit.ListEditActions
import com.tailormyresume.feature.profile.impl.listedit.ListEditScreen
import com.tailormyresume.feature.profile.impl.skills.SkillsActions
import com.tailormyresume.feature.profile.impl.skills.SkillsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal fun editorStates(): List<Triple<String, String, @Composable () -> Unit>> = listOf(
    Triple("editors_contact_empty_linkedin", "LinkedIn · optional") {
        EditContactScreen(state = contactState(), actions = EditContactActions())
    },
    Triple("editors_skills", "Paige picks the most relevant ones for each job.") {
        SkillsScreen(state = skillsState(), actions = SkillsActions())
    },
    Triple("editors_list_summary", "Your starting point.") {
        ListEditScreen(state = listState(ProfileListSection.SUMMARY), actions = ListEditActions())
    },
    Triple("editors_list_education", "+ Add education") {
        ListEditScreen(state = listState(ProfileListSection.EDUCATION), actions = ListEditActions())
    },
    Triple("editors_list_achievements", "+ Add achievement") {
        ListEditScreen(state = listState(ProfileListSection.ACHIEVEMENTS), actions = ListEditActions())
    },
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorsScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun contactEmptyLinkedIn() = shoot("editors_contact_empty_linkedin", TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun contactEmptyLinkedInFont200At337dp() = shoot("editors_contact_empty_linkedin", NARROW_DEVICE)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun skills() = shoot("editors_skills", TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun skillsFont200At337dp() = shoot("editors_skills", NARROW_DEVICE)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun listEditSummary() = shoot("editors_list_summary", TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun listEditSummaryFont200At337dp() = shoot("editors_list_summary", NARROW_DEVICE)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun listEditEducation() = shoot("editors_list_education", TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun listEditEducationFont200At337dp() = shoot("editors_list_education", NARROW_DEVICE)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun listEditAchievements() = shoot("editors_list_achievements", TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun listEditAchievementsFont200At337dp() = shoot("editors_list_achievements", NARROW_DEVICE)

    private fun shoot(name: String, device: TmrTestDevice) {
        val (_, marker, content) = editorStates().first { it.first == name }
        rule.setTmrContent(device, content)
        rule.assertTexts(marker)
        rule.assertNoTruncatedText()
        rule.capture(name, device)
    }
}
