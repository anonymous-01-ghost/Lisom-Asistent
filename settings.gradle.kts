pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "LisomAssistant"
include(":lisom-models", ":lisom-api", ":lisom-speech", ":lisom-wakeword", ":lisom-nlu", ":lisom-tts", ":lisom-core", ":lisom-platform", ":lisom-testing", ":lisom-demo")
