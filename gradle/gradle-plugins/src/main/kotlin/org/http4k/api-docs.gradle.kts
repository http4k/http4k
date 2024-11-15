package org.http4k

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import java.time.Year

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<AbstractDokkaTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        moduleVersion.set(version.toString())
        footerMessage = "(c) ${Year.now().value} http4k"
        homepageLink = "https://http4k.org"
        customAssets = listOf(
            file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/logo-icon.svg"),
        )
        customStyleSheets = listOf(file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/dokka.css"))
    }
}
