package org.http4k.internal

import gradle.kotlin.dsl.accessors._63b4a5a02be9986151f9bdba492362cc.sourceSets
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named

enum class ModuleLicense(val commonName: String, val url: String, val licenseDir: String) {
    Apache2(
        "Apache-2.0",
        "https://www.apache.org/licenses/LICENSE-2.0",
        "."
    ),
    Http4kCommercial(
        "http4k Commercial License",
        "https://http4k.org/commercial-license",
        "./pro"
    )
}

fun Project.addLicenseToJars(license: ModuleLicense) {
    tasks.named<Jar>("jar") {
        from(rootProject.file(license.licenseDir).absolutePath) {
            include("LICENSE")
        }
    }

    tasks.named<Jar>("sourcesJar") {
        from(sourceSets.named("main").get().allSource)
        from(rootProject.file(license.licenseDir).absolutePath) {
            include("LICENSE")
        }
        archiveClassifier.set("sources")
    }
}
