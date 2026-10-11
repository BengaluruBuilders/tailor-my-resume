package com.tailormyresume.feature.onboarding.impl.signin

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.hero.TmrSticker
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.onboarding.impl.R

@Immutable
internal class SignInStorySpec(
    @StringRes val headline: Int,
    @StringRes val subline: Int,
    val pose: TmrPaigePose,
    val background: (TmrColors) -> Color,
)

internal val SignInStories = listOf(
    SignInStorySpec(
        R.string.feature_onboarding_impl_signin_story1_headline,
        R.string.feature_onboarding_impl_signin_story1_subline,
        TmrPaigePose.Signin1,
    ) { it.blue },
    SignInStorySpec(
        R.string.feature_onboarding_impl_signin_story2_headline,
        R.string.feature_onboarding_impl_signin_story2_subline,
        TmrPaigePose.Signin2,
    ) { it.amber },
    SignInStorySpec(
        R.string.feature_onboarding_impl_signin_story3_headline,
        R.string.feature_onboarding_impl_signin_story3_subline,
        TmrPaigePose.Signin3,
    ) { it.lime },
)

private const val ONE_PAGE_STICKER_MAX_FONT_SCALE = 1.3f

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
internal fun SignInStoryArt(index: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (index) {
            0 -> UploadArt()
            1 -> JobPostArt()
            else -> TailoredArt()
        }
        TmrPaige(SignInStories[index].pose)
    }
}

@Composable
private fun BoxScope.UploadArt() {
    val ink = TmrTheme.colors.ink
    Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 84.dp, height = 46.dp).clip(TriangleShape).background(ink))
        Box(Modifier.size(width = 32.dp, height = 44.dp).background(ink))
    }
    TmrSticker(
        stringResource(R.string.feature_onboarding_impl_signin_art_pdf),
        Modifier.align(Alignment.TopStart).offset(y = 30.dp),
        rotationDegrees = -10f,
    )
    TmrSticker(
        stringResource(R.string.feature_onboarding_impl_signin_art_docx),
        Modifier.align(Alignment.TopEnd).offset(y = 110.dp),
        rotationDegrees = 8f,
        onCheek = true,
    )
}

@Composable
private fun BoxScope.JobPostArt() {
    val colors = TmrTheme.colors
    val outline = BorderStroke(2.5.dp, colors.ink)
    Column(
        Modifier
            .align(Alignment.TopStart)
            .offset(y = 40.dp)
            .width(150.dp)
            .rotate(5f)
            .background(colors.paper, RoundedCornerShape(14.dp))
            .border(outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.feature_onboarding_impl_signin_art_job_post),
            style = TmrTheme.typography.body,
            color = colors.ink,
        )
        Box(Modifier.fillMaxWidth().height(4.dp).background(colors.ink, RoundedCornerShape(2.dp)))
        HighlightBar(0.7f)
        Box(Modifier.fillMaxWidth(0.85f).height(4.dp).background(colors.ink, RoundedCornerShape(2.dp)))
        HighlightBar(0.48f)
    }
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = 100.dp, y = 20.dp)
            .size(58.dp)
            .background(colors.paper.copy(alpha = 0.45f), CircleShape)
            .border(BorderStroke(3.dp, colors.ink), CircleShape),
    )
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = 146.dp, y = 78.dp)
            .size(width = 32.dp, height = 10.dp)
            .rotate(45f)
            .background(colors.ink, RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun HighlightBar(fraction: Float) {
    val colors = TmrTheme.colors
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(9.dp)
            .background(colors.cheek, RoundedCornerShape(3.dp))
            .border(BorderStroke(2.dp, colors.ink), RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun BoxScope.TailoredArt() {
    val colors = TmrTheme.colors
    TmrSticker(
        stringResource(R.string.feature_onboarding_impl_signin_sticker_keywords),
        Modifier.align(Alignment.TopEnd).offset(y = 14.dp),
        rotationDegrees = 10f,
    )
    Box(
        Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 30.dp).size(16.dp).rotate(45f)
            .background(colors.ink),
    )
    Box(
        Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-40).dp).size(18.dp).rotate(45f)
            .background(colors.cheek).border(BorderStroke(2.5.dp, colors.ink)),
    )
    if (LocalDensity.current.fontScale <= ONE_PAGE_STICKER_MAX_FONT_SCALE) {
        TmrSticker(
            stringResource(R.string.feature_onboarding_impl_signin_art_one_page),
            Modifier.align(Alignment.BottomStart).offset(x = 8.dp, y = (-34).dp),
            rotationDegrees = -8f,
        )
    }
}
