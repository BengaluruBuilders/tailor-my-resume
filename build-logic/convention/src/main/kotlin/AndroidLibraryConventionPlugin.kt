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

abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            if (path == ":core:domain" || path == ":core:data") {
                apply(plugin = "tailormyresume.kover")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureAndroidLint(lint)
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
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
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
