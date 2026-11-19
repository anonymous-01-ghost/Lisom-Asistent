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
                    name = "LisomGitHubPackages"
                    url = uri("https://maven.pkg.github.com/anonymous-01-ghost/Lisom-Asistent")
                    credentials {
                        username = providers.gradleProperty("gpr.user")
                            .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                        password = providers.gradleProperty("gpr.key")
                            .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
                    }
                }
            }
        }
    }
}
