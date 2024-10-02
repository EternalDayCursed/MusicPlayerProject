import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    base
}

base {
    archivesName.set("android-music-player")
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
    defaultConfig {
        applicationId = libs.versions.app.version.appId.get()
        minSdk = 29
        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()
        versionName = project.libs.versions.app.version.versionName.get()
        versionCode = project.libs.versions.app.version.versionCode.get().toInt()
        vectorDrawables.useSupportLibrary = true
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        buildFeatures {
            buildConfig = true
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
        isCoreLibraryDesugaringEnabled = true
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = project.libs.versions.app.build.kotlinJVMTarget.get()
    }

    namespace = libs.versions.app.version.appId.get()

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.eventbus)
    implementation(libs.androidx.media)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.lottie)
    implementation(libs.m3u.parser)
    implementation(libs.autofittextview)
    implementation(libs.jaudiotagger)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.protolite.well.known.types)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api(libs.gson)
    api(libs.reprint)
    api(libs.recyclerView.fastScroller)
    api(libs.rtl.viewpager)
    api(libs.joda.time)

    implementation(libs.glide.compose)
    api(libs.glide)
    ksp(libs.glide.compiler)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)}
