pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri(rootDir.resolve("build/repository")) }
        google()
        mavenCentral()
    }
}
rootProject.name = "LisomAssistant"
include(":lisom-models", ":lisom-api", ":lisom-speech", ":lisom-wakeword", ":lisom-nlu", ":lisom-tts", ":lisom-core", ":lisom-platform", ":lisom-testing", ":lisom-demo")
