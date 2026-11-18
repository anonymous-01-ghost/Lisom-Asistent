plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}
kotlin {
    android {
        namespace = "dev.lisom.platform"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    sourceSets {
        commonMain.dependencies {
            api(project(":lisom-api"))
            implementation(project(":lisom-speech"))
            implementation(project(":lisom-wakeword"))
            implementation(project(":lisom-tts"))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
