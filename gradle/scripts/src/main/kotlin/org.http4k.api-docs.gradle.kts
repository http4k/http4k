import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import java.time.Year

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        moduleVersion.set(version.toString())
        footerMessage = "(c) ${Year.now().value} http4k"
        homepageLink = "https://http4k.org"
        customStyleSheets = listOf(file("src/docs/css/dokka.css"))
    }
}
