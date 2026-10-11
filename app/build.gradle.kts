plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "top.lanrhyme.horizon"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "top.lanrhyme.horizon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // GeckoView & Mozilla Android Components
    val acVersion = "157.0"
    implementation("org.mozilla.geckoview:geckoview-omni:157.0.20261005135250")
    implementation("org.mozilla.components:browser-engine-gecko:$acVersion")
    implementation("org.mozilla.components:feature-session:$acVersion")
    implementation("org.mozilla.components:feature-tabs:$acVersion")
    implementation("org.mozilla.components:service-firefox-accounts:$acVersion") {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }
    implementation("org.mozilla.components:service-sync:$acVersion") {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }
    implementation("org.mozilla.components:feature-syncedtabs:$acVersion") {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }
    implementation("org.mozilla.components:browser-storage-sync:$acVersion") {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
