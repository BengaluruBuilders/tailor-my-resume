package com.tailormyresume.feature.tailor.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.BulletReviewNavKey
import com.tailormyresume.feature.tailor.api.navigation.CoverLetterNavKey
import com.tailormyresume.feature.tailor.api.navigation.CreditsNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportPreviewNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.PackPurchaseNavKey
import com.tailormyresume.feature.tailor.api.navigation.PrepQuestionsNavKey
import com.tailormyresume.feature.tailor.api.navigation.ShareLastExportNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailorNavKey

fun EntryProviderScope<NavKey>.tailorEntry(navigator: Navigator) {
    entry<TailorNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<BulletReviewNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<CoverLetterNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<PrepQuestionsNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<ExportPreviewNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<ExportedNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<ShareLastExportNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<PackPurchaseNavKey> { key -> BasicText(key.javaClass.simpleName) }
    entry<CreditsNavKey> { key -> BasicText(key.javaClass.simpleName) }
}
