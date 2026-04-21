plugins {
    id("com.android.application")
    id("org.lsposed.lsparanoid")
    id("com.google.gms.google-services")
    kotlin("android")
}

apply(from = "../signing.gradle")

android {
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    buildToolsVersion = rootProject.extra["buildToolsVersion"] as String
    namespace = rootProject.extra["LoaderPkg"] as String

    lint {
        baseline = file("lint-baseline.xml")
    }

    defaultConfig {
        applicationId = rootProject.extra["LoaderPkg"] as String
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

        resourceConfigurations += listOf("en", "hi")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("debug")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    ndkVersion = rootProject.extra["ndkVersion"] as String

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("com.github.nukc:StateView:2.1.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.github.tiann:FreeReflection:3.2.2")
    implementation("com.github.CodingGay.BlackReflection:core:1.1.4")
    annotationProcessor("com.github.CodingGay.BlackReflection:compiler:1.1.4")
    implementation("org.jdeferred:jdeferred-android-aar:1.2.6")
    implementation("io.github.molihuan:pathselector:1.1.16")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation("com.airbnb.android:lottie:6.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
