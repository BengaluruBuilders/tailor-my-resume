package com.tailormyresume.feature.profile.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import com.tailormyresume.feature.profile.impl.experience.EditRoleActions
import com.tailormyresume.feature.profile.impl.experience.EditRoleDraft
import com.tailormyresume.feature.profile.impl.experience.EditRoleScreen
import com.tailormyresume.feature.profile.impl.experience.EditRoleUiState
import com.tailormyresume.feature.profile.impl.experience.ExperienceRow
import com.tailormyresume.feature.profile.impl.experience.ExperienceScreen
import com.tailormyresume.feature.profile.impl.experience.ExperienceUiState
import com.tailormyresume.feature.profile.impl.overview.ProfileScreen
import com.tailormyresume.feature.profile.impl.overview.ProfileUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NARROW_QUALIFIERS = "w337dp-h734dp-normal-long-notround-any-480dpi-keyshidden-nonav"

private val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private fun ComposeContentTestRule.setTmrContent(
    device: TmrTestDevice,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
            TmrTheme {
                Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                    content()
                }
            }
        }
    }
}

private fun ComposeContentTestRule.capture(
    name: String,
    device: TmrTestDevice,
) {
    runBlocking {
        captureForDevice(outputDirectory = "src/test/screenshots", screenName = name, device = device)
    }
}

private fun ComposeContentTestRule.assertTexts(vararg texts: String) {
    texts.forEach { text -> onNodeWithText(text, substring = true).assertExists() }
}

private fun ComposeContentTestRule.assertNoText(text: String) {
    onNodeWithText(text, substring = true).assertDoesNotExist()
}

private fun ComposeContentTestRule.assertNoTruncatedText() {
    textLayouts().forEach { (node, layout) ->
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
        val widestLine = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
        assertTrue("$text overflows its width: line right $widestLine > ${layout.size.width}", widestLine <= layout.size.width + 0.5f)
        assertFalse("$text overflows its height", layout.didOverflowHeight)
        assertTrue("$text is clipped by its node", layout.size.width <= node.size.width && layout.size.height <= node.size.height)
        val ellipsized = (0 until layout.lineCount).filter { layout.isLineEllipsized(it) }
        assertTrue("$text has ellipsized lines $ellipsized", ellipsized.isEmpty())
    }
}

private fun ComposeContentTestRule.textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> =
    onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
        .fetchSemanticsNodes()
        .mapNotNull { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.firstOrNull()?.let { node to it }
        }

private fun profileContent(): @Composable () -> Unit = {
    ProfileScreen(
        state = ProfileUiState.Content(
            initials = "PD",
            name = "Priya Deshmukh",
            headline = "Finance analyst",
            years = 4,
            city = "Pune",
            percent = 96,
            experienceCount = 3,
            educationCount = 1,
            skillsCount = 18,
            achievementsCount = 6,
            linkedinMissing = true,
            sourceFileName = "Priya_Deshmukh_Resume.pdf",
        ),
        onOpen = {},
    )
}

private fun experienceContent(rows: List<ExperienceRow>): @Composable () -> Unit = {
    ExperienceScreen(
        state = ExperienceUiState.Content(rows),
        onRole = {},
        onAdd = {},
    )
}

private fun experienceRows(tataNeedsEndDate: Boolean = false): List<ExperienceRow> = listOf(
    ExperienceRow(
        entryId = "exp-infosys",
        title = "Business Analyst",
        company = "Infosys",
        start = "Jul 2022",
        end = "Present",
        isCurrent = true,
        needsEndDate = false,
    ),
    ExperienceRow(
        entryId = "exp-tata",
        title = "Data Analyst Intern",
        company = "Tata Digital",
        start = "Jun 2021",
        end = if (tataNeedsEndDate) "" else "May 2022",
        isCurrent = false,
        needsEndDate = tataNeedsEndDate,
    ),
    ExperienceRow(
        entryId = "exp-bajaj",
        title = "Finance Associate",
        company = "Bajaj Finserv",
        start = "Jun 2020",
        end = "May 2021",
        isCurrent = false,
        needsEndDate = false,
    ),
)

private fun editRoleContent(state: EditRoleUiState.Editing): @Composable () -> Unit = {
    EditRoleScreen(state = state, actions = EditRoleActions())
}

private fun internDraft(): EditRoleDraft = EditRoleDraft(
    title = "Data Analyst Intern",
    company = "Tata Digital",
    start = "Jun 2021",
    end = "May 2022",
    current = false,
    bullets = listOf(
        "Built forecasting models in Python for category demand.",
        "Automated weekly Excel reports for 5 business teams.",
    ),
)

private fun currentJobDraft(): EditRoleDraft = EditRoleDraft(
    title = "Data Analyst Intern",
    company = "Tata Digital",
    start = "Jun 2021",
    end = "",
    current = true,
    bullets = listOf(
        "Built forecasting models in Python for category demand.",
        "Automated weekly Excel reports for 5 business teams.",
    ),
)

private class ScreenSlot {
    var content: @Composable () -> Unit by mutableStateOf<@Composable () -> Unit>({})
}

private fun allProfileStates(): List<Pair<String, @Composable () -> Unit>> = listOf(
    "Priya Deshmukh" to profileContent(),
    "Business Analyst" to experienceContent(experienceRows()),
    "+ Add role" to experienceContent(emptyList()),
    "end date?" to experienceContent(experienceRows(tataNeedsEndDate = true)),
    "Delete this role" to editRoleContent(
        EditRoleUiState.Editing(
            isNew = false,
            draft = internDraft(),
            canSave = true,
            canAddBullet = true,
        ),
    ),
    "e.g. Infosys" to editRoleContent(
        EditRoleUiState.Editing(
            isNew = true,
            draft = EditRoleDraft(),
            canSave = false,
            canAddBullet = true,
        ),
    ),
    "Present" to editRoleContent(
        EditRoleUiState.Editing(
            isNew = false,
            draft = currentJobDraft(),
            canSave = true,
            canAddBullet = true,
        ),
    ),
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun profile() {
        profileState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun profileFont200At337dp() {
        profileState(NARROW_DEVICE)
    }

    private fun profileState(device: TmrTestDevice) {
        rule.setTmrContent(device, profileContent())
        rule.assertTexts(
            "Profile",
            "Settings",
            "Priya Deshmukh",
            "Finance analyst · 4 yrs · Pune",
            "96% complete",
            "Contact",
            "Summary",
            "Experience",
            "Education",
            "Skills",
            "Achievements",
            "LinkedIn & links",
            "Add",
            "Built from",
            "Replace",
        )
        rule.assertNoTruncatedText()
        rule.capture("profile_profile", device)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExperienceScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun experience() {
        experienceState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun experienceFont200At337dp() {
        experienceState(NARROW_DEVICE)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun experienceEmpty() {
        experienceEmptyState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun experienceEmptyFont200At337dp() {
        experienceEmptyState(NARROW_DEVICE)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun experienceRequiredFix() {
        experienceRequiredFixState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun experienceRequiredFixFont200At337dp() {
        experienceRequiredFixState(NARROW_DEVICE)
    }

    private fun experienceState(device: TmrTestDevice) {
        rule.setTmrContent(device, experienceContent(experienceRows()))
        rule.assertTexts(
            "Experience",
            "Business Analyst",
            "Infosys",
            "Jul 2022 – Present",
            "Data Analyst Intern",
            "Jun 2021 – May 2022",
            "Finance Associate",
            "+ Add role",
        )
        rule.assertNoTruncatedText()
        rule.capture("profile_experience", device)
    }

    private fun experienceEmptyState(device: TmrTestDevice) {
        rule.setTmrContent(device, experienceContent(emptyList()))
        rule.assertTexts("Experience", "+ Add role")
        rule.assertNoText("Business Analyst")
        rule.assertNoTruncatedText()
        rule.capture("profile_experience_empty", device)
    }

    private fun experienceRequiredFixState(device: TmrTestDevice) {
        rule.setTmrContent(device, experienceContent(experienceRows(tataNeedsEndDate = true)))
        rule.assertTexts(
            "Experience",
            "Business Analyst",
            "Infosys",
            "Data Analyst Intern",
            "Jun 2021 – end date?",
            "Finance Associate",
            "+ Add role",
        )
        rule.assertNoTruncatedText()
        rule.capture("profile_experience_required_fix", device)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditRoleScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun editRole() {
        editRoleState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun editRoleFont200At337dp() {
        editRoleState(NARROW_DEVICE)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun addRole() {
        addRoleState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun addRoleFont200At337dp() {
        addRoleState(NARROW_DEVICE)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun currentJobOn() {
        currentJobOnState(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun currentJobOnFont200At337dp() {
        currentJobOnState(NARROW_DEVICE)
    }

    private fun editRoleState(device: TmrTestDevice) {
        rule.setTmrContent(
            device,
            editRoleContent(
                EditRoleUiState.Editing(
                    isNew = false,
                    draft = internDraft(),
                    canSave = true,
                    canAddBullet = true,
                ),
            ),
        )
        rule.assertTexts(
            "Edit role",
            "Job title",
            "Company",
            "Start",
            "End",
            "I work here now",
            "What you did",
            "+ Add bullet",
            "Delete this role",
            "Built forecasting models",
        )
        rule.assertNoTruncatedText()
        rule.capture("profile_edit_role", device)
    }

    private fun addRoleState(device: TmrTestDevice) {
        rule.setTmrContent(
            device,
            editRoleContent(
                EditRoleUiState.Editing(
                    isNew = true,
                    draft = EditRoleDraft(),
                    canSave = false,
                    canAddBullet = true,
                ),
            ),
        )
        rule.assertTexts(
            "Add role",
            "e.g. Business Analyst",
            "e.g. Infosys",
            "+ Add bullet",
        )
        rule.onAllNodesWithText("Mon YYYY").assertCountEquals(2)
        rule.assertNoText("Delete this role")
        rule.assertNoTruncatedText()
        rule.capture("profile_add_role", device)
    }

    private fun currentJobOnState(device: TmrTestDevice) {
        rule.setTmrContent(
            device,
            editRoleContent(
                EditRoleUiState.Editing(
                    isNew = false,
                    draft = currentJobDraft(),
                    canSave = true,
                    canAddBullet = true,
                ),
            ),
        )
        rule.assertTexts("Edit role", "Present")
        rule.assertNoTruncatedText()
        rule.capture("profile_current_job_on", device)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileLargeFontLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun allStatesAt337dpFontScale200AreNotClipped() {
        val slot = ScreenSlot()
        rule.setTmrContent(NARROW_DEVICE) { slot.content() }
        allProfileStates().forEach { (marker, state) ->
            slot.content = state
            rule.waitForIdle()
            rule.assertTexts(marker)
            rule.assertNoTruncatedText()
        }
    }
}
