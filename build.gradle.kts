import org.gradle.api.publish.PublishingExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kmp) apply false
}

val lisomVersion = providers.gradleProperty("lisomVersion")
    .orElse("0.1.0-SNAPSHOT")

allprojects {
    group = "dev.lisom"
    version = lisomVersion.get()
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")

        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "buildRepository"
                    url = rootProject.layout.buildDirectory.dir("repository").get().asFile.toURI()
                }
            }
        }
    }
}
