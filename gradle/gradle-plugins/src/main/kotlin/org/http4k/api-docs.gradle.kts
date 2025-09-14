package org.http4k

import java.time.Year

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            remoteUrl("https://github.com/http4k/http4k/tree/master")
            localDirectory.set(rootDir)
        }
    }

    dokkaPublications.html {
        pluginsConfiguration.html {
            moduleVersion.set(project.properties["releaseVersion"]?.toString() ?: "LOCAL")
            footerMessage.set("(c) ${Year.now().value} http4k")
            homepageLink.set("https://http4k.org")
            customAssets.from(
                file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/logo-icon.svg")
            )
            customStyleSheets.from(
                file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/dokka.css")
            )
        }
    }
}
