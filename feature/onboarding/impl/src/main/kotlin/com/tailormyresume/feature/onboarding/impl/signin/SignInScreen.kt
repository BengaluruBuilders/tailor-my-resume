package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastAction
import com.tailormyresume.core.designsystem.component.content.TmrStoryBars
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.feature.onboarding.impl.R

internal object SignInTags {
    const val STORY = "signin_story"
    const val STORY_TOUCH = "signin_story_touch"
    const val STORY_CONTENT = "signin_story_content"
    const val GOOGLE_ACTION = "signin_google_action"
}

private const val HINT_ALPHA = 0.6f
private const val SUBORDINATE_TEXT_ALPHA = 0.8f
private const val ART_HEIGHT_FRACTION = 0.45f
private const val ART_WIDTH_FRACTION = 0.8f
private const val COMPACT_FONT_SCALE = 1.3f
private val COMPACT_STORY_HEIGHT = 400.dp
private val COMPACT_ART_HEIGHT = 96.dp
private val GOOGLE_OUTLINE = Color(0xFF747775)
private val GOOGLE_TEXT = Color(0xFF1F1F1F)

@OptIn(ExperimentalTextApi::class)
private val googleSans = FontFamily(
    Font(
        R.font.google_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
)

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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    SignInScreen(
        uiState,
        onContinue,
        modifier,
        onTryGuest = viewModel::onTryGuest,
        onClaimTrial = viewModel::onClaimTrial,
        onContinueGuest = viewModel::onContinueGuest,
    )
}

@Composable
internal fun SignInScreen(
    uiState: SignInUiState,
    onContinueWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
    story: SignInStoryState = rememberSignInStoryState(uiState == SignInUiState.SigningIn),
    onTryGuest: () -> Unit = {},
    onClaimTrial: () -> Unit = {},
    onContinueGuest: () -> Unit = {},
) {
    val spec = SignInStories[story.index]
    Box(
        modifier
            .fillMaxSize()
            .background(spec.background(TmrTheme.colors))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp),
        ) {
            StoryPanel(
                story = story,
                modifier = Modifier.weight(1f),
            )
            if (uiState is SignInUiState.GuestOffer || uiState is SignInUiState.GuestResult || uiState == SignInUiState.PreparingGuest) {
                GuestActions(uiState, onTryGuest, onClaimTrial, onContinueGuest)
            } else {
                SignInActions(uiState, onContinueWithGoogle, onTryGuest)
            }
        }
    }
}

@Composable
private fun StoryPanel(
    story: SignInStoryState,
    modifier: Modifier = Modifier,
) {
    val spec = SignInStories[story.index]
    val headline = stringResource(spec.headline)
    val subline = stringResource(spec.subline)
    val label = stringResource(R.string.feature_onboarding_impl_signin_story_label, story.index + 1, SIGN_IN_STORY_COUNT)
    val hint = stringResource(
        if (story.paused) R.string.feature_onboarding_impl_signin_hint_paused else R.string.feature_onboarding_impl_signin_hint_idle,
    )
    val next = stringResource(R.string.feature_onboarding_impl_signin_action_next)
    val previous = stringResource(R.string.feature_onboarding_impl_signin_action_previous)
    val paused = stringResource(R.string.feature_onboarding_impl_signin_hint_paused)
    val scrollState = remember(story.index) { ScrollState(initial = 0) }
    val reading by remember(scrollState) {
        derivedStateOf { scrollState.value > 0 || scrollState.isScrollInProgress }
    }
    LaunchedEffect(reading) { story.reading = reading }
    BoxWithConstraints(modifier) {
        val compact = maxHeight < COMPACT_STORY_HEIGHT || LocalDensity.current.fontScale > COMPACT_FONT_SCALE
        val artHeight = if (compact) COMPACT_ART_HEIGHT else minOf(maxHeight * ART_HEIGHT_FRACTION, maxWidth * ART_WIDTH_FRACTION)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                TmrStoryBars(SIGN_IN_STORY_COUNT, story.index, if (TmrTheme.motion.reduced) 1f else story.fraction)
                if (compact) StoryExplanation(headline, subline, Modifier.padding(top = TmrTheme.spacing.sectionGap))
                if (!compact) StoryHint(hint)
                Box(
                    Modifier.fillMaxWidth().height(artHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    SignInStoryArt(story.index, artHeight, Modifier.fillMaxSize())
                }
                if (compact) StoryHint(hint)
                if (!compact) StoryExplanation(headline, subline)
            }
        }
    }
}

@Composable
private fun StoryExplanation(headline: String, subline: String, modifier: Modifier = Modifier) {
    Column(modifier.testTag(SignInTags.STORY_CONTENT)) {
        Text(headline, color = TmrTheme.colors.ink, style = TmrTheme.typography.display)
        Text(subline, Modifier.padding(top = 12.dp), TmrTheme.colors.ink, style = TmrTheme.typography.bodyLarge)
    }
}

@Composable
private fun StoryHint(hint: String) {
    Text(
        hint,
        Modifier.fillMaxWidth().padding(top = 12.dp),
        TmrTheme.colors.ink.copy(alpha = HINT_ALPHA),
        style = TmrTheme.typography.label,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun SignInActions(uiState: SignInUiState, onContinueWithGoogle: () -> Unit, onTryGuest: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    TmrPrimaryButton(
        label = stringResource(R.string.feature_onboarding_impl_guest_try),
        onClick = onTryGuest,
        enabled = uiState != SignInUiState.SigningIn,
        modifier = Modifier.fillMaxWidth().testTag("signin_guest_action"),
    )
    Spacer(Modifier.height(12.dp))
    GoogleButton(uiState, onContinueWithGoogle)
    Spacer(Modifier.height(16.dp))
    Footer(Modifier.padding(bottom = 34.dp))
}

@Composable
private fun GoogleButton(uiState: SignInUiState, onClick: () -> Unit) {
    val signingIn = uiState == SignInUiState.SigningIn
    Row(
        Modifier
            .testTag(SignInTags.GOOGLE_ACTION)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, GOOGLE_OUTLINE, CircleShape)
            .clickable(enabled = !signingIn, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.feature_onboarding_impl_google_g_logo),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            stringResource(
                if (signingIn) R.string.feature_onboarding_impl_signin_signing_in else R.string.feature_onboarding_impl_signin_google,
            ),
            color = GOOGLE_TEXT,
            fontFamily = googleSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
        )
    }
}

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
    Text(
        annotated,
        modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth(),
        ink.copy(alpha = SUBORDINATE_TEXT_ALPHA),
        style = TmrTheme.typography.caption,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GuestActions(state: SignInUiState, onRetry: () -> Unit, onClaim: () -> Unit, onContinue: () -> Unit) {
    val offer = (state as? SignInUiState.GuestOffer)?.offer
    val result = (state as? SignInUiState.GuestResult)?.result
    val busy = state == SignInUiState.PreparingGuest || (state as? SignInUiState.GuestOffer)?.busy == true
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp).verticalScroll(remember { ScrollState(0) }),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val message = when {
            busy -> stringResource(R.string.feature_onboarding_impl_guest_checking)
            offer is GuestTrialOffer.Available -> pluralStringResource(R.plurals.feature_onboarding_impl_guest_offer, offer.credits, offer.credits)
            result is GuestTrialResult.Granted -> pluralStringResource(R.plurals.feature_onboarding_impl_guest_granted, result.credits, result.credits)
            offer == GuestTrialOffer.Existing || result == GuestTrialResult.Existing -> stringResource(R.string.feature_onboarding_impl_guest_existing)
            offer == GuestTrialOffer.NotEligible || result == GuestTrialResult.NotEligible -> stringResource(R.string.feature_onboarding_impl_guest_ineligible)
            offer is GuestTrialOffer.Unavailable && !offer.canContinue -> stringResource(R.string.feature_onboarding_impl_guest_signin_unavailable)
            else -> stringResource(R.string.feature_onboarding_impl_guest_unavailable)
        }
        Text(message, color = TmrTheme.colors.ink, style = TmrTheme.typography.body)
        if (offer is GuestTrialOffer.Available) {
            if (offer.changed) Text(stringResource(R.string.feature_onboarding_impl_guest_changed), color = TmrTheme.colors.ink, style = TmrTheme.typography.label)
            Text(stringResource(R.string.feature_onboarding_impl_guest_disclosure), color = TmrTheme.colors.ink, style = TmrTheme.typography.caption)
            TmrPrimaryButton(
                label = pluralStringResource(R.plurals.feature_onboarding_impl_guest_claim, offer.credits, offer.credits),
                onClick = onClaim,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (offer is GuestTrialOffer.Unavailable) {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_onboarding_impl_signin_retry),
                onClick = onRetry,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!busy && (offer !is GuestTrialOffer.Unavailable || offer.canContinue)) {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_onboarding_impl_guest_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
