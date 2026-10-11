plugins {
    alias(libs.plugins.tailormyresume.android.feature.impl)
    alias(libs.plugins.tailormyresume.android.library.compose)
}

android {
    namespace = "com.tailormyresume.feature.profile.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.analysis.api)
    implementation(projects.feature.applications.api)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    kspTest(libs.kotlin.metadata)
    testImplementation(projects.core.testing)
}
