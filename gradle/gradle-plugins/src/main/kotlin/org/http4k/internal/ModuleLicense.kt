package org.http4k.internal

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named

enum class ModuleLicense(
    val commonName: String,
    val url: String,
    val licenseDir: String,
    val comments: String? = null
) {
    Apache2(
        "Apache-2.0",
        "https://www.apache.org/licenses/LICENSE-2.0",
        "."
    ),
    Http4kCommercial(
        "http4k Commercial License",
        "https://http4k.org/commercial-license",
        "./pro",
        "Use requires a valid http4k commercial subscription."
    ),
    Http4kEE(
        "http4k Commercial License",
        "https://http4k.org/commercial-license",
        ".",
        "Use requires a valid http4k Enterprise Edition subscription. Contains portions licensed under Apache-2.0 - see LICENSE-APACHE and NOTICE."
    )
}

private val LICENSE_FILES = listOf("LICENSE", "LICENSE-APACHE", "NOTICE")

fun Project.addLicenseToJars(license: ModuleLicense) {
    tasks.named<Jar>("jar") {
        from(rootProject.file(license.licenseDir).absolutePath) {
            include(LICENSE_FILES)
        }
    }

    tasks.named<Jar>("sourcesJar") {
        from(project.extensions.getByType(SourceSetContainer::class.java).named("main").get().allSource)
        from(rootProject.file(license.licenseDir).absolutePath) {
            include(LICENSE_FILES)
        }
        archiveClassifier.set("sources")
    }
}
