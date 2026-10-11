package com.tailormyresume.feature.profile.impl

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.impl.contact.EditContactActions
import com.tailormyresume.feature.profile.impl.contact.EditContactDraft
import com.tailormyresume.feature.profile.impl.contact.EditContactScreen
import com.tailormyresume.feature.profile.impl.contact.EditContactUiState
import com.tailormyresume.feature.profile.impl.listedit.ListEditActions
import com.tailormyresume.feature.profile.impl.listedit.ListEditScreen
import com.tailormyresume.feature.profile.impl.listedit.ListEditUiState
import com.tailormyresume.feature.profile.impl.skills.SkillsActions
import com.tailormyresume.feature.profile.impl.skills.SkillsScreen
import com.tailormyresume.feature.profile.impl.skills.SkillsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal fun contactState(linkedin: String = "") = EditContactUiState.Editing(
    draft = EditContactDraft(
        fullName = "Priya Deshmukh",
        phone = "+91 98200 41736",
        city = "Pune, India",
        linkedin = linkedin,
        portfolio = "",
    ),
    email = "priya.deshmukh@gmail.com",
    canSave = true,
)

internal val SKILL_NAMES = listOf(
    "SQL", "Excel", "Power BI", "Python", "Forecasting", "Tableau", "Financial modelling",
    "Data cleaning", "Dashboards", "Pandas", "Reconciliation", "Month-end close", "Looker",
    "A/B testing", "Budgeting", "Reporting", "Presenting", "Stakeholder updates",
)

internal fun skillsState(canAdd: Boolean = false, newSkill: String = "") =
    SkillsUiState.Editing(skills = SKILL_NAMES, newSkill = newSkill, canAdd = canAdd)

internal const val SUMMARY_TEXT = "Finance analyst with 4 years of SQL, Power BI and Excel work across banking and retail."

internal val ACHIEVEMENT_TEXTS = listOf(
    "Cut monthly reporting prep time by 40%",
    "Built 12 dashboards used by 3 regional teams",
    "Automated reports for 5 business teams",
    "Top performer award, Infosys 2023",
    "Led intern onboarding for 6 hires",
    "Closed books 2 days faster each month",
)

internal fun listState(section: ProfileListSection): ListEditUiState.Editing = when (section) {
    ProfileListSection.SUMMARY -> ListEditUiState.Editing(section, listOf(SUMMARY_TEXT), canSave = true, canAdd = false)
    ProfileListSection.EDUCATION -> ListEditUiState.Editing(
        section,
        listOf("B.Com, Finance · Symbiosis College, Pune · 2020"),
        canSave = true,
        canAdd = true,
    )
    ProfileListSection.ACHIEVEMENTS -> ListEditUiState.Editing(section, ACHIEVEMENT_TEXTS, canSave = true, canAdd = true)
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class EditorsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersAllFieldsWithAmberLinkedInAndReadOnlyEmail() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            EditContactScreen(state = contactState(), actions = EditContactActions())
        }

        rule.assertTexts(
            "Contact",
            "Full name",
            "Phone",
            "City",
            "Email · from Google",
            "priya.deshmukh@gmail.com",
            "LinkedIn · optional",
            "Portfolio · optional",
        )
        assertThat(rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()).hasSize(5)
        rule.onNodeWithText("priya.deshmukh@gmail.com").assertExists()
    }

    @Test
    fun contactFieldsReportTypedText() {
        val names = mutableListOf<String>()
        var linkedin = ""
        rule.setTmrContent(TmrTestDevices.prototype) {
            EditContactScreen(
                state = contactState(),
                actions = EditContactActions(onFullNameChange = { names += it }, onLinkedinChange = { linkedin = it }),
            )
        }

        rule.onNodeWithContentDescription("Full name").performTextInput("A")
        rule.onNodeWithContentDescription("LinkedIn · optional").performTextInput("B")

        assertThat(names.any { it.contains("A") }).isTrue()
        assertThat(linkedin).isEqualTo("B")
    }

    @Test
    fun skillsShowCountLineChipsAndRemove() {
        var removed = ""
        rule.setTmrContent(TmrTestDevices.prototype) {
            SkillsScreen(state = skillsState(), actions = SkillsActions(onRemove = { removed = it }))
        }

        rule.assertTexts("Skills", "18 skills. Paige picks the most relevant ones for each job.", "Add a skill", "SQL", "Stakeholder updates")
        rule.onNodeWithContentDescription("Remove SQL").performClick()

        assertThat(removed).isEqualTo("SQL")
    }

    @Test
    fun skillsCountLineUsesTheSingularForOne() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            SkillsScreen(
                state = SkillsUiState.Editing(skills = listOf("SQL"), newSkill = "", canAdd = false),
                actions = SkillsActions(),
            )
        }

        rule.assertTexts("1 skill. Paige picks")
    }

    @Test
    fun skillsAddIsDisabledUntilTheDraftCanBeAdded() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            SkillsScreen(state = skillsState(canAdd = false), actions = SkillsActions())
        }

        rule.onNodeWithText("Add").assertIsNotEnabled()
    }

    @Test
    fun skillsAddCallsBackWhenEnabled() {
        var added = 0
        rule.setTmrContent(TmrTestDevices.prototype) {
            SkillsScreen(state = skillsState(canAdd = true, newSkill = "Snowflake"), actions = SkillsActions(onAdd = { added++ }))
        }

        rule.onNodeWithText("Add").performClick()

        assertThat(added).isEqualTo(1)
    }

    @Test
    fun summaryShowsNoteOneTextareaAndNoAddButton() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            ListEditScreen(state = listState(ProfileListSection.SUMMARY), actions = ListEditActions())
        }

        rule.assertTexts("Summary", "Your starting point. Each tailored resume adapts it to the job.", "Finance analyst with 4 years")
        assertThat(rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()).hasSize(1)
        rule.assertNoText("+ Add")
    }

    @Test
    fun educationHasNoNoteAndAddsThroughTheDashedButton() {
        var added = 0
        rule.setTmrContent(TmrTestDevices.prototype) {
            ListEditScreen(state = listState(ProfileListSection.EDUCATION), actions = ListEditActions(onAdd = { added++ }))
        }

        rule.assertTexts("Education", "B.Com, Finance")
        rule.assertNoText("Your starting point")
        rule.assertNoText("Numbers help")
        rule.onNodeWithText("+ Add education").performClick()

        assertThat(added).isEqualTo(1)
    }

    @Test
    fun achievementsShowNoteSixTextareasAndAddLabel() {
        var edited = -1 to ""
        rule.setTmrContent(TmrTestDevices.prototype) {
            ListEditScreen(
                state = listState(ProfileListSection.ACHIEVEMENTS),
                actions = ListEditActions(onTextChange = { index, text -> edited = index to text }),
            )
        }

        rule.assertTexts("Achievements", "Numbers help. Paige pulls the most relevant ones into each resume.", "+ Add achievement")
        val fields = rule.onAllNodes(hasSetTextAction())
        assertThat(fields.fetchSemanticsNodes()).hasSize(6)
        fields[2].performTextInput("!")

        assertThat(edited.first).isEqualTo(2)
        assertThat(rule.onAllNodesWithText("+ Add achievement").fetchSemanticsNodes()).hasSize(1)
    }
}
