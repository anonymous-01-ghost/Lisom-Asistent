plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}
dependencies {
    implementation(project(":lisom-nlu"))
}
application {
    mainClass.set("dev.lisom.demo.MainKt")
}
