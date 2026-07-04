package org.http4k.internal

plugins {
    kotlin("jvm")
    id("org.http4k.conventions")
    id("org.http4k.internal.license-check")
    id("org.http4k.internal.code-coverage")
    id("org.cyclonedx.bom")
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    setSource(files("src/main/kotlin"))
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    setSource(files("src/main/kotlin"))
}

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion()).editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "intellij_idea",
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_class-naming" to "disabled",
                "ktlint_standard_property-naming" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
                "ktlint_standard_enum-entry-name-case" to "disabled",
                "ktlint_standard_package-name" to "disabled",
                "ktlint_standard_mixed-condition-operators" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
                "ktlint_standard_comment-wrapping" to "disabled",
                "ktlint_standard_argument-list-wrapping" to "disabled",
                "ktlint_standard_binary-expression-wrapping" to "disabled",
                "ktlint_standard_class-signature" to "disabled",
                "ktlint_standard_enum-wrapping" to "disabled",
                "ktlint_standard_function-expression-body" to "disabled",
                "ktlint_standard_function-signature" to "disabled",
                "ktlint_standard_if-else-wrapping" to "disabled",
                "ktlint_standard_multiline-expression-wrapping" to "disabled",
                "ktlint_standard_parameter-list-wrapping" to "disabled",
                "ktlint_standard_property-wrapping" to "disabled",
                "ktlint_standard_statement-wrapping" to "disabled",
                "ktlint_standard_wrapping" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named<org.cyclonedx.gradle.CyclonedxDirectTask>("cyclonedxDirectBom") {
    jsonOutput.set(layout.buildDirectory.file("reports/${project.name}-sbom.json"))
}

tasks.named("cyclonedxBom") {
    dependsOn("cyclonedxDirectBom")
}

tasks.register("generateLicenseReportJson") {
    dependsOn("generateLicenseReport")
    doLast {
        val source = layout.buildDirectory.file("reports/dependency-license/index.json").get().asFile
        val dest = layout.buildDirectory.file("reports/${project.name}-license-report.json").get().asFile
        if (source.exists()) source.copyTo(dest, overwrite = true)
    }
}

tasks.register("writePublishManifest") {
    doLast {
        val group = project.group.toString()
        val artifactId = project.name
        val version = project.findProperty("releaseVersion")?.toString() ?: "LOCAL"
        val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
        val manifestFile = rootProject.layout.buildDirectory.file("publish-manifest.txt").get().asFile
        manifestFile.parentFile.mkdirs()
        manifestFile.appendText("$group|$artifactId|$version|$buildDir\n")
    }
}

fun ktlintVersion() = "1.8.0"
