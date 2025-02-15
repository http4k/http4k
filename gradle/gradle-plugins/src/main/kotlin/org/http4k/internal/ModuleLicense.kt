package org.http4k.internal

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named

enum class ModuleLicense(val commonName: String, val url: String, val licenseDir: String) {
    Http4kCommercial(
        "http4k Commercial License",
        "http://http4k.org/commercial-license",
        "."
    )
}

fun Project.addLicenseToJars(license: ModuleLicense) {
    tasks.named<Jar>("jar") {
        from(rootProject.file(license.licenseDir).absolutePath) {
            include("LICENSE")
        }
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.file(license.licenseDir).absolutePath) {
            include("LICENSE")
        }
        archiveClassifier.set("sources")
    }
}
