plugins {
    alias(libs.plugins.tailormyresume.android.application)
    alias(libs.plugins.tailormyresume.android.application.compose)
    alias(libs.plugins.tailormyresume.hilt)
    alias(libs.plugins.dependency.guard)
    id("io.github.takahirom.roborazzi")
}

fun releaseSetting(name: String): String? =
    (providers.environmentVariable(name).orNull ?: providers.gradleProperty(name).orNull)?.takeIf { it.isNotBlank() }

val maxVersionCode = 2_100_000_000

val releaseVersionCode: Int =
    releaseSetting("TMR_VERSION_CODE")?.let { raw ->
        val parsed = raw.toIntOrNull()
        require(parsed != null && parsed in 1..maxVersionCode) {
            "TMR_VERSION_CODE must be an integer from 1 to $maxVersionCode, got '$raw'"
        }
        parsed
    } ?: 1

val releaseVersionName: String =
    releaseSetting("TMR_VERSION_NAME")?.also { raw ->
        require(Regex("""\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?""").matches(raw)) {
            "TMR_VERSION_NAME must look like 1.2.3 or 1.2.3-beta.1, got '$raw'"
        }
    } ?: "0.1.0"

val uploadKeystoreFile = releaseSetting("TMR_UPLOAD_KEYSTORE_FILE")
val uploadStorePassword = releaseSetting("TMR_UPLOAD_STORE_PASSWORD")
val uploadKeyAlias = releaseSetting("TMR_UPLOAD_KEY_ALIAS")
val uploadKeyPassword = releaseSetting("TMR_UPLOAD_KEY_PASSWORD")
val hasUploadSigning =
    listOf(uploadKeystoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { it != null }

val prodBackendProperties =
    listOf(
        "tailormyresumeWebClientId",
        "tailormyresumeFirebaseApiKey",
        "tailormyresumeFirebaseAppId",
        "tailormyresumeFirebaseProjectId",
    )

val stagingBackendProperties =
    listOf(
        "tailormyresumeStagingApiBaseUrl",
        "tailormyresumeStagingWebClientId",
        "tailormyresumeStagingFirebaseApiKey",
        "tailormyresumeStagingFirebaseAppId",
        "tailormyresumeStagingFirebaseProjectId",
    )

fun com.android.build.api.dsl.BuildType.backendConfig(apiBaseUrl: String, otherValues: List<String>) {
    val names =
        listOf(
            "TAILORMYRESUME_WEB_CLIENT_ID",
            "TAILORMYRESUME_FIREBASE_API_KEY",
            "TAILORMYRESUME_FIREBASE_APP_ID",
            "TAILORMYRESUME_FIREBASE_PROJECT_ID",
        )
    buildConfigField("String", "TAILORMYRESUME_API_BASE_URL", "\"$apiBaseUrl\"")
    names.zip(otherValues).forEach { (name, value) -> buildConfigField("String", name, "\"$value\"") }
}

fun com.android.build.api.dsl.BuildType.stagingBuildConfig() {
    val values = stagingBackendProperties.map { providers.gradleProperty(it).getOrElse("") }
    backendConfig(values.first().ifBlank { "https://staging.invalid" }, values.drop(1))
}

android {
    namespace = "com.tailormyresume.app"

    defaultConfig {
        applicationId = "com.tailormyresume.app"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = file(uploadKeystoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".staging"
            stagingBuildConfig()
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
            backendConfig(
                "https://apps-backend.fly.dev",
                prodBackendProperties.map { providers.gradleProperty(it).getOrElse("") },
            )
        }
        create("staging") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.getByName(if (hasUploadSigning) "upload" else "debug")
            stagingBuildConfig()
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

val verifyUploadSigning by tasks.registering {
    val ready = hasUploadSigning
    doLast {
        check(ready) {
            "Release bundles must be signed with the upload key. Set TMR_UPLOAD_KEYSTORE_FILE, " +
                "TMR_UPLOAD_STORE_PASSWORD, TMR_UPLOAD_KEY_ALIAS and TMR_UPLOAD_KEY_PASSWORD. See docs/RELEASE.md."
        }
    }
}

fun verifyBackendConfig(taskName: String, buildLabel: String, properties: List<String>) =
    tasks.register(taskName) {
        val blank = properties.filter { providers.gradleProperty(it).orNull.isNullOrBlank() }
        doLast {
            check(blank.isEmpty()) {
                "The $buildLabel build needs these Gradle properties, missing or blank: ${blank.joinToString()}. " +
                    "See docs/RELEASE.md."
            }
        }
    }

val verifyProdBackendConfig = verifyBackendConfig("verifyProdBackendConfig", "release", prodBackendProperties)
val verifyStagingBackendConfig = verifyBackendConfig("verifyStagingBackendConfig", "staging", stagingBackendProperties)

tasks.configureEach {
    when (name) {
        "bundleRelease" -> dependsOn(verifyUploadSigning, verifyProdBackendConfig)
        "bundleStaging", "assembleStaging" -> dependsOn(verifyStagingBackendConfig)
    }
}

dependencies {
    implementation(projects.feature.applications.api)
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.onboarding.impl)
    implementation(projects.feature.applications.impl)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.profile.impl)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.settings.impl)
    implementation(projects.feature.analysis.api)
    implementation(projects.feature.analysis.impl)
    implementation(projects.feature.tailor.api)
    implementation(projects.feature.tailor.impl)

    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.navigation)

    implementation(projects.core.network)
    implementation(libs.room.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.google.googleid)
    implementation(libs.play.billing) {
        exclude(group = "com.google.android.gms", module = "play-services-location")
    }

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(projects.core.testing)
    testImplementation(projects.core.screenshot)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.testManifest)
    testImplementation(libs.androidx.test.ext.junit)
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}
