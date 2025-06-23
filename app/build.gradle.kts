import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.djatar.vidmax"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("keystore.file", "")
            val storePassword = localProperties.getProperty("keystore.password", "")
            val keyAlias = localProperties.getProperty("keystore.key.alias", "")
            val keyPassword = localProperties.getProperty("keystore.key.password", "")

            val storeFile = if (storeFilePath.isNotEmpty()) file(storeFilePath) else null
            if (storeFile != null && storeFile.exists()) {
                this.storeFile = storeFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.djatar.vidmax"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("x86")
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            val releaseAdId = localProperties.getProperty("admob.interstitial.release.id", "")
            val releaseAppId = localProperties.getProperty("admob.app.id.release", "")
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_AD_ID",
                "\"$releaseAdId\""
            )

            manifestPlaceholders["adMobAppId"] = releaseAppId
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "VidMax Debug")
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_AD_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )

            manifestPlaceholders["adMobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    splits.abi {
        isEnable = true
        reset()
        include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        isUniversalApk = true
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "VidMax-${defaultConfig.versionName}-${name}.apk"
        }
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.bundles.ytdlAndroid)

    implementation(libs.androidx.compose.material.iconsExtended)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.accompanist.permissions)

    implementation(libs.play.services.ads)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}