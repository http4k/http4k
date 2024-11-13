import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        moduleVersion.set(version.toString())
        customAssets = listOf(file("src/docs/img/favicon-mono.png"))
        footerMessage = "(c) 2024 http4k"
        homepageLink = "https://http4k.org"
        customStyleSheets = listOf(file("src/docs/css/dokka.css"))
    }
}
