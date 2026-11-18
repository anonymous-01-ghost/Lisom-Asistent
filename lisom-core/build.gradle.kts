plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}
kotlin {
    android {
        namespace = "dev.lisom.core"
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
            api(project(":lisom-speech"))
            api(project(":lisom-wakeword"))
            api(project(":lisom-nlu"))
            api(project(":lisom-tts"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":lisom-testing"))
        }
    }
}
