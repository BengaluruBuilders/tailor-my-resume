plugins {
    alias(libs.plugins.tailormyresume.android.feature.impl)
    alias(libs.plugins.tailormyresume.android.library.compose)
}

android {
    namespace = "com.tailormyresume.feature.tailor.impl"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(projects.feature.applications.api)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.tailor.api)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    kspTest(libs.kotlin.metadata)
}
