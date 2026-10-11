package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastAction
import com.tailormyresume.core.designsystem.component.content.TmrStoryBars
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.onboarding.impl.R

internal object SignInTags {
    const val STORY = "signin_story"
    const val STORY_TOUCH = "signin_story_touch"
}

private const val HINT_ALPHA = 0.6f
private const val ART_HEIGHT_FRACTION = 0.4f
private val ART_MIN_HEIGHT = 300.dp

@Composable
internal fun SignInRoute(
    viewModel: SignInViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val message = stringResource(R.string.feature_onboarding_impl_signin_cancelled)
    val retry = stringResource(R.string.feature_onboarding_impl_signin_retry)
    val onContinue = viewModel::onContinueWithGoogle
    LaunchedEffect(viewModel) {
        viewModel.cancelled.collect { toast.show(message, TmrToastAction(retry, onContinue)) }
    }
    SignInScreen(uiState, onContinue, modifier)
}

@Composable
internal fun SignInScreen(
    uiState: SignInUiState,
    onContinueWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
    story: SignInStoryState = rememberSignInStoryState(uiState == SignInUiState.SigningIn),
) {
    val spec = SignInStories[story.index]
    Box(
        modifier
            .fillMaxSize()
            .background(spec.background(TmrTheme.colors))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            Column(
                Modifier
                    .heightIn(min = screenHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp),
            ) {
                StoryPanel(story, (screenHeight * ART_HEIGHT_FRACTION).coerceAtLeast(ART_MIN_HEIGHT))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(18.dp))
                GoogleButton(uiState, onContinueWithGoogle)
                Spacer(Modifier.height(10.dp))
                Footer(Modifier.padding(bottom = 34.dp))
            }
        }
    }
}

@Composable
private fun StoryPanel(story: SignInStoryState, artHeight: Dp, modifier: Modifier = Modifier) {
    val spec = SignInStories[story.index]
    val ink = TmrTheme.colors.ink
    val typography = TmrTheme.typography
    val headline = stringResource(spec.headline)
    val subline = stringResource(spec.subline)
    val label = stringResource(R.string.feature_onboarding_impl_signin_story_label, story.index + 1, SIGN_IN_STORY_COUNT)
    val hint = stringResource(
        if (story.paused) R.string.feature_onboarding_impl_signin_hint_paused else R.string.feature_onboarding_impl_signin_hint_idle,
    )
    val next = stringResource(R.string.feature_onboarding_impl_signin_action_next)
    val previous = stringResource(R.string.feature_onboarding_impl_signin_action_previous)
    val paused = stringResource(R.string.feature_onboarding_impl_signin_hint_paused)
    Column(
        modifier
            .testTag(SignInTags.STORY)
            .semantics(mergeDescendants = true) {
                contentDescription = "$headline. $subline. $label"
                if (story.paused) stateDescription = paused
                customActions = listOf(
                    CustomAccessibilityAction(next) {
                        story.step(1)
                        true
                    },
                    CustomAccessibilityAction(previous) {
                        story.step(-1)
                        true
                    },
                )
            },
    ) {
        Column(
            Modifier
                .testTag(SignInTags.STORY_TOUCH)
                .signInStoryGestures(story)
                .padding(top = 12.dp),
        ) {
            TmrStoryBars(SIGN_IN_STORY_COUNT, story.index, if (TmrTheme.motion.idle.storyAutoAdvance) story.fraction else 1f)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label.uppercase(), Modifier.weight(1f), ink, style = typography.label)
                Text(
                    hint.uppercase(),
                    Modifier.weight(1f),
                    ink.copy(alpha = HINT_ALPHA),
                    style = typography.label,
                    textAlign = TextAlign.End,
                )
            }
            SignInStoryArt(story.index, Modifier.height(artHeight))
        }
        Text(headline, color = ink, style = typography.display)
        Text(subline, Modifier.padding(top = 12.dp), ink, style = typography.bodyLarge)
    }
}

@Composable
private fun GoogleButton(uiState: SignInUiState, onClick: () -> Unit) {
    val colors = TmrTheme.colors
    val signingIn = uiState == SignInUiState.SigningIn
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(colors.ink)
            .clickable(enabled = !signingIn, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).background(colors.paper, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.feature_onboarding_impl_signin_google_mark),
                color = GOOGLE_BLUE,
                style = TmrTheme.typography.mono14,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(
                if (signingIn) R.string.feature_onboarding_impl_signin_signing_in else R.string.feature_onboarding_impl_signin_google,
            ),
            Modifier.weight(1f, fill = false),
            colors.paper,
            style = TmrTheme.typography.mono14,
            textAlign = TextAlign.Center,
        )
    }
}

private val GOOGLE_BLUE = Color(0xFF4285F4)

@Composable
private fun Footer(modifier: Modifier = Modifier) {
    val ink = TmrTheme.colors.ink
    val toast = LocalTmrToast.current
    val notSet = stringResource(R.string.feature_onboarding_impl_signin_address_not_set)
    val terms = stringResource(R.string.feature_onboarding_impl_signin_terms)
    val privacy = stringResource(R.string.feature_onboarding_impl_signin_privacy)
    val text = stringResource(R.string.feature_onboarding_impl_signin_footer, terms, privacy)
    val annotated = remember(text, terms, privacy, notSet) {
        buildAnnotatedString {
            append(text)
            listOf(terms, privacy).forEach { word ->
                val start = text.indexOf(word)
                addLink(
                    LinkAnnotation.Clickable(
                        tag = word,
                        styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                    ) { toast.show(notSet) },
                    start,
                    start + word.length,
                )
            }
        }
    }
    Text(annotated, modifier.fillMaxWidth(), ink, style = TmrTheme.typography.caption, textAlign = TextAlign.Center)
}
