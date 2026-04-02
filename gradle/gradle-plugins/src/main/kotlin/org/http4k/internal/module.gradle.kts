package org.http4k.internal

plugins {
    kotlin("jvm")
    id("org.http4k.conventions")
    id("org.http4k.internal.license-check")
    id("org.http4k.internal.code-coverage")
    id("org.cyclonedx.bom")
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
        val version = project.properties["releaseVersion"]?.toString() ?: "LOCAL"
        val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
        val manifestFile = rootProject.layout.buildDirectory.file("publish-manifest.txt").get().asFile
        manifestFile.parentFile.mkdirs()
        manifestFile.appendText("$group|$artifactId|$version|$buildDir\n")
    }
}

