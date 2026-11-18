plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kmp) apply false
    application apply false
}
allprojects {
    group = "dev.lisom"
    version = "0.1.0-SNAPSHOT"
}
