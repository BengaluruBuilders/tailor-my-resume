import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.tailormyresume.buildlogic.configureAndroidLint
import com.tailormyresume.buildlogic.configureKotlinAndroid
import com.tailormyresume.buildlogic.configureSpotlessForAndroid
import com.tailormyresume.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class TailorMyResumeAndroidLibraryScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "io.github.takahirom.roborazzi")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureAndroidLint(lint)
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                testOptions.unitTests.apply {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
                resourcePrefix =
                    path.split("""\W""".toRegex()).drop(1).distinct().joinToString(separator = "_")
                        .lowercase() + "_"
            }
            val hasAndroidTests = fileTree("src") { include("androidTest*/**/*.kt", "androidTest*/**/*.java") }.files.isNotEmpty()
            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants(selector().all()) { it.enableAndroidTest = hasAndroidTests }
            }
            configureSpotlessForAndroid()

            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                val roborazziCore = libs.findLibrary("roborazzi").get()
                val roborazziCompose = libs.findLibrary("roborazzi.compose").get()
                listOf("debugImplementation", "releaseImplementation").forEach { configuration ->
                    add(configuration, platform(bom))
                    add(configuration, roborazziCore)
                    add(configuration, roborazziCompose)
                }
                "debugImplementation"(libs.findLibrary("robolectric").get())
                "debugImplementation"(libs.findLibrary("androidx.compose.ui-test").get())
                "debugImplementation"(libs.findLibrary("androidx.compose.ui-testManifest").get())
            }
        }
    }
}
