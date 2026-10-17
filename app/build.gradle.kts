import java.net.URI

plugins {
    alias(libs.plugins.kotlin.serialization)
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

val stagingUploadKeystoreFile = releaseSetting("TMR_STAGING_UPLOAD_KEYSTORE_FILE")
val stagingUploadStorePassword = releaseSetting("TMR_STAGING_UPLOAD_STORE_PASSWORD")
val stagingUploadKeyAlias = releaseSetting("TMR_STAGING_UPLOAD_KEY_ALIAS")
val stagingUploadKeyPassword = releaseSetting("TMR_STAGING_UPLOAD_KEY_PASSWORD")
val hasStagingUploadSigning =
    listOf(stagingUploadKeystoreFile, stagingUploadStorePassword, stagingUploadKeyAlias, stagingUploadKeyPassword).all { it != null }

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

fun integrityProjectNumber(property: String): String = providers.gradleProperty(property).orNull
    ?.takeIf { raw -> Regex("[0-9]+").matches(raw) && (raw.toLongOrNull() ?: 0) > 0 }.orEmpty()

fun referralOrigin(property: String): String {
    val origin = providers.gradleProperty(property).getOrElse("")
    if (origin.isEmpty()) return origin
    require(origin.length <= 253 && Regex("https://[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+").matches(origin)) { "Referral origin must be an exact lowercase HTTPS DNS origin" }
    val host = origin.removePrefix("https://")
    require(host.split('.').all { it.length <= 63 } && !host.all { it.isDigit() || it == '.' } && listOf("local", "localhost", "internal", "flycast", "invalid").none { host == it || host.endsWith(".$it") }) { "Referral origin requires an approved public host" }
    return origin
}

fun com.android.build.api.dsl.ApplicationBuildType.referralConfig(property: String) {
    val origin = referralOrigin(property)
    buildConfigField("String", "TAILORMYRESUME_REFERRAL_ORIGIN", "\"$origin\"")
    manifestPlaceholders["referralLinkHost"] = origin.removePrefix("https://")
}

fun com.android.build.api.dsl.ApplicationBuildType.stagingBuildConfig() {
    val values = stagingBackendProperties.map { providers.gradleProperty(it).getOrElse("") }
    applicationIdSuffix = ".staging"
    referralConfig("tailormyresumeStagingReferralOrigin")
    resValue("string", "app_name", "TMR Staging")
    backendConfig(values.first().ifBlank { "https://staging.invalid" }, values.drop(1))
    buildConfigField("String", "TAILORMYRESUME_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER", "\"${integrityProjectNumber("tailormyresumeStagingPlayIntegrityCloudProjectNumber")}\"")
}

fun com.android.build.api.dsl.ApplicationBuildType.shrinkWithR8(defaultRules: File) {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(defaultRules, "proguard-rules.pro")
}

android {
    namespace = "com.tailormyresume.app"

    defaultConfig {
        applicationId = "com.tailormyresume.app"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    signingConfigs {
        if (hasStagingUploadSigning) {
            create("stagingUpload") {
                storeFile = file(stagingUploadKeystoreFile!!)
                storePassword = stagingUploadStorePassword
                keyAlias = stagingUploadKeyAlias
                keyPassword = stagingUploadKeyPassword
            }
        }
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
            stagingBuildConfig()
        }
        release {
            referralConfig("tailormyresumeReferralOrigin")
            shrinkWithR8(getDefaultProguardFile("proguard-android-optimize.txt"))
            buildConfigField("String", "TAILORMYRESUME_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER", "\"${integrityProjectNumber("tailormyresumePlayIntegrityCloudProjectNumber")}\"")
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
            backendConfig(
                "https://apps-backend.fly.dev",
                prodBackendProperties.map { providers.gradleProperty(it).getOrElse("") },
            )
        }
        create("stagingPlay") {
            shrinkWithR8(getDefaultProguardFile("proguard-android-optimize.txt"))
            matchingFallbacks += "release"
            if (hasStagingUploadSigning) signingConfig = signingConfigs.getByName("stagingUpload")
            stagingBuildConfig()
        }
        create("staging") {
            shrinkWithR8(getDefaultProguardFile("proguard-android-optimize.txt"))
            matchingFallbacks += "release"
            signingConfig = signingConfigs.getByName("debug")
            stagingBuildConfig()
        }
    }

    sourceSets {
        listOf("debug", "staging", "stagingPlay", "release").forEach { variant ->
            val property = if (variant == "release") "tailormyresumeReferralOrigin" else "tailormyresumeStagingReferralOrigin"
            if (referralOrigin(property).isNotEmpty()) {
                val original = layout.projectDirectory.file("src/$variant/AndroidManifest.xml")
                val template = layout.projectDirectory.file("src/referralLinks/AndroidManifest.xml")
                val generated = layout.buildDirectory.file("generated/referralManifest/$variant/AndroidManifest.xml")
                val generate = tasks.register("generate${variant.replaceFirstChar(Char::uppercaseChar)}ReferralManifest") {
                    inputs.file(template)
                    if (original.asFile.exists()) inputs.file(original)
                    outputs.file(generated)
                    doLast {
                        val overlay = template.asFile.readText().substringAfter("<application>").substringBefore("</application>")
                        val base = if (original.asFile.exists()) original.asFile.readText() else template.asFile.readText().replace(overlay, "")
                        val activity = "<activity android:name=\".MainActivity\">"
                        val merged = if (base.contains(activity)) {
                            base.replace(activity, activity + overlay.substringAfter(activity).substringBefore("</activity>"))
                        } else if (base.contains("</application>")) {
                            base.replace("</application>", "$overlay</application>")
                        } else {
                            base.replace(Regex("<application\\b([^>]*)/>")) { match ->
                                "<application${match.groupValues[1]}>$overlay</application>"
                            }
                        }
                        require(merged.contains("android.intent.action.VIEW")) { "Referral manifest must include the link filter" }
                        generated.get().asFile.apply { parentFile.mkdirs(); writeText(merged) }
                    }
                }
                getByName(variant).manifest.srcFile(generated)
                tasks.configureEach { if (name == "pre${variant.replaceFirstChar(Char::uppercaseChar)}Build") dependsOn(generate) }
            }
        }
        getByName("debug").res.srcDir("src/localStaging/res")
        getByName("staging").res.srcDir("src/localStaging/res")
        getByName("stagingPlay").res.srcDir("src/localStaging/res")
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

val verifyStagingUploadSigning = tasks.register("verifyStagingUploadSigning") {
    val ready = hasStagingUploadSigning
    val keyFile = stagingUploadKeystoreFile?.let { file(it).canonicalFile }
    val productionKeyFile = uploadKeystoreFile?.let { file(it).canonicalFile }
    val debugKeyFile = android.signingConfigs.findByName("debug")?.storeFile?.canonicalFile
    val alias = stagingUploadKeyAlias
    doLast {
        check(ready) {
            "Staging Play artifacts require dedicated TMR_STAGING_UPLOAD_KEYSTORE_FILE, " +
                "TMR_STAGING_UPLOAD_STORE_PASSWORD, TMR_STAGING_UPLOAD_KEY_ALIAS and TMR_STAGING_UPLOAD_KEY_PASSWORD. See docs/RELEASE.md."
        }
        check(keyFile?.isFile == true && keyFile != productionKeyFile && keyFile != debugKeyFile && alias != "androiddebugkey") {
            "Staging Play requires an existing dedicated staging upload keystore, separate from production and debug signing."
        }
    }
}

val verifyStagingPlayIdentity = tasks.register("verifyStagingPlayIdentity") {
    val api = providers.gradleProperty("tailormyresumeStagingApiBaseUrl").getOrElse("")
    val project = providers.gradleProperty("tailormyresumeStagingFirebaseProjectId").getOrElse("")
    val appId = providers.gradleProperty("tailormyresumeStagingFirebaseAppId").getOrElse("")
    val clientId = providers.gradleProperty("tailormyresumeStagingWebClientId").getOrElse("")
    val integrityProject = integrityProjectNumber("tailormyresumeStagingPlayIntegrityCloudProjectNumber")
    doLast {
        val uri = runCatching { URI(api) }.getOrNull()
        check(uri != null && uri.scheme == "https" &&
            uri.host?.lowercase() in listOf("abhishek-dubey-ms-7b16.local", "192.168.88.15") &&
            uri.port in listOf(8443, 20002) && uri.path in listOf("", "/") &&
            uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Staging Play requires the approved LAN HTTPS staging API; production and other endpoints are forbidden."
        }
        check(project == "tailormy-resume-staging" && appId.startsWith("1:496155242121:android:") &&
            clientId.startsWith("496155242121-") && clientId.endsWith(".apps.googleusercontent.com") &&
            integrityProject == "496155242121") {
            "Staging Play requires Firebase and Play Integrity project 496155242121 (tailormy-resume-staging)."
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
        "preStagingBuild" -> dependsOn(verifyStagingBackendConfig)
        "preStagingPlayBuild" -> dependsOn(verifyStagingBackendConfig, verifyStagingPlayIdentity, verifyStagingUploadSigning)
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
    implementation(libs.install.referrer)
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
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    kspTest(libs.kotlin.metadata)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.testManifest)
    testImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}
