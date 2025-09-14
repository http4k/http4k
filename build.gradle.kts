import java.time.Year

plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.api-docs")
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.typeflows)
    id("org.http4k.conventions")
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}

dependencies {
    typeflowsApi(libs.typeflows.github)
    typeflowsApi(libs.typeflows.github.marketplace)
}

typeflows {
    typeflowsClass = "Http4kTypeflows"
}

dependencies {
    subprojects.forEach { subproject ->
        dokka(subproject)
    }
}

dokka {
    moduleName.set("http4k")

    dokkaPublications.html {
        includes.from("README.md")

        pluginsConfiguration.html {
            moduleVersion.set(project.properties["releaseVersion"]?.toString() ?: "LOCAL")
            footerMessage.set("(c) ${Year.now().value} http4k")
            homepageLink.set("https://http4k.org")
            customAssets.from(
                file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/logo-icon.svg")
            )
            customStyleSheets.from(
                file("${rootProject.projectDir}/gradle/gradle-plugins/src/main/resources/dokka.css").takeIf { it.exists() }
            )
        }
    }

    basePublicationsDirectory.set(layout.buildDirectory.dir("docs/api"))
}

