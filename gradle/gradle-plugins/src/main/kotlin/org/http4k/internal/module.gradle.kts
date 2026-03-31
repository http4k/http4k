package org.http4k.internal

plugins {
    kotlin("jvm")
    id("org.http4k.conventions")
    id("org.http4k.internal.license-check")
    id("org.http4k.internal.code-coverage")
    id("org.cyclonedx.bom")
}

tasks.named<org.cyclonedx.gradle.CycloneDxTask>("cyclonedxBom") {
    setIncludeConfigs(listOf("runtimeClasspath"))
    setOutputFormat("json")
    setOutputName("${project.name}-sbom")
}

tasks.register("dependencyList") {
    doLast {
        configurations.runtimeClasspath.get().resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { resolved ->
            println("${project.name} -> ${resolved.module.id}")
        }
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

tasks.register("uploadProvenance") {
    doLast { project.uploadProvenance() }
}
