plugins {
    alias(libs.plugins.tailormyresume.android.application)
    alias(libs.plugins.tailormyresume.android.application.compose)
    alias(libs.plugins.tailormyresume.hilt)
    alias(libs.plugins.dependency.guard)
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

    flavorDimensions += "backend"
    productFlavors {
        create("demo") {
            dimension = "backend"
        }
        create("prod") {
            dimension = "backend"
            buildConfigField("String", "TAILORMYRESUME_API_BASE_URL", "\"https://apps-backend.fly.dev\"")
            buildConfigField(
                "String",
                "TAILORMYRESUME_WEB_CLIENT_ID",
                "\"${providers.gradleProperty("tailormyresumeWebClientId").getOrElse("")}\"",
            )
            buildConfigField(
                "String",
                "TAILORMYRESUME_FIREBASE_API_KEY",
                "\"${providers.gradleProperty("tailormyresumeFirebaseApiKey").getOrElse("")}\"",
            )
            buildConfigField(
                "String",
                "TAILORMYRESUME_FIREBASE_APP_ID",
                "\"${providers.gradleProperty("tailormyresumeFirebaseAppId").getOrElse("")}\"",
            )
            buildConfigField(
                "String",
                "TAILORMYRESUME_FIREBASE_PROJECT_ID",
                "\"${providers.gradleProperty("tailormyresumeFirebaseProjectId").getOrElse("")}\"",
            )
        }
    }

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
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
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

val prodBackendProperties =
    listOf(
        "tailormyresumeWebClientId",
        "tailormyresumeFirebaseApiKey",
        "tailormyresumeFirebaseAppId",
        "tailormyresumeFirebaseProjectId",
    )

val verifyProdBackendConfig by tasks.registering {
    val blank = prodBackendProperties.filter { providers.gradleProperty(it).orNull.isNullOrBlank() }
    doLast {
        check(blank.isEmpty()) {
            "The prod release bundle needs these Gradle properties, missing or blank: ${blank.joinToString()}. " +
                "See docs/RELEASE.md."
        }
    }
}

tasks.configureEach {
    if (name.startsWith("bundle") && name.endsWith("Release")) {
        dependsOn(verifyUploadSigning)
        if (name.contains("Prod")) dependsOn(verifyProdBackendConfig)
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

    "prodImplementation"(projects.core.network)
    "prodImplementation"(libs.room.runtime)
    "prodImplementation"(platform(libs.firebase.bom))
    "prodImplementation"(libs.firebase.auth)
    "prodImplementation"(libs.androidx.credentials)
    "prodImplementation"(libs.androidx.credentials.playServicesAuth)
    "prodImplementation"(libs.google.googleid)
    "prodImplementation"(libs.play.billing) {
        exclude(group = "com.google.android.gms", module = "play-services-location")
    }

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(projects.core.testing)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    "testProdImplementation"(libs.okhttp.mockwebserver)
    "testProdImplementation"(libs.robolectric)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.testManifest)
    testImplementation(libs.androidx.test.ext.junit)
}

dependencyGuard {
    configuration("demoReleaseRuntimeClasspath")
    configuration("prodReleaseRuntimeClasspath")
}
