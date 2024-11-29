package org.http4k.internal

plugins {
    kotlin("jvm")
    id("org.http4k.conventions")
    id("org.http4k.internal.license-check")
    id("org.http4k.internal.code-coverage")
}

tasks.create("dependencyList") {
    doLast {
        configurations.runtimeClasspath.get().resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { resolved ->
            println("${project.name} -> ${resolved.module.id}")
        }
    }
}
