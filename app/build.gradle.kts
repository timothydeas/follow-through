plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ideasinc.followthrough"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ideasinc.followthrough"
        minSdk = 31
        targetSdk = 35
        // Bumped 19 → 20 for the per-goal barriers/intentions/progress restructure
        // (barriers capture replaces check-ins; Stats & Customize Questions removed).
        // Bumped 20 → 21 for the read-only "science behind FollowThru" Settings screen.
        // Bumped 21 → 22 for the check-in–centered rework (typed check-ins, DB v31,
        // Stats restored, Home-card polish).
        // Bumped 22 → 23 for distinctive reminder cues (emoji/label/image/sound per
        // goal; DB v32).
        // Bumped 23 → 24 for the per-plan model: many plans per goal, each with its
        // own intention + cue + reminder (DB v33).
        // Bumped 24 → 25 reverting that: intention + cue + reminder live ON the
        // check-in again (DB v34).
        // Bumped 25 → 26: reminder+cue step in the check-in flow; check-in streak
        // is now the Stats headline.
        // Bumped 26 → 27: single-select cue (one trigger), fixed cue controls
        // (phrase cursor, photo/sound pickers), reminder deep-links to its check-in.
        // Bumped 27 → 28: streak-card reassurance copy fix (no logic change).
        // Bumped 28 → 29: removed the "Make it distinctive" cue authoring; reminders
        // now show only the check-in's implementation intention.
        // Bumped 29 → 30: adaptive tablet layout — two-pane goals-list + goal-detail
        // Home on expanded widths; single-column screens capped/centred per-screen.
        // NOTE: codes 22–30 above were internal iteration only — never uploaded. The live
        // release on Play is 1.0 (versionCode 1), so public versioning resets to 2 here to
        // keep codes just ahead of what's actually published. Next upload must be 3+.
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Export Room schemas so future migrations can be validated against a known
// baseline. The v26→v27 migration test does not depend on the exported JSON
// (it builds a populated v26 DB by hand), but exporting is good hygiene.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.0")
    // Google's official Play In-App Review API (no data collection of ours).
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")
    debugImplementation(libs.androidx.ui.tooling)

    // Local (JVM) tests — Robolectric lets the Room v26→v27 migration test run
    // without an emulator/device.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Instrumented tests (run on a device/emulator during the testing week).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
