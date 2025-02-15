package org.http4k

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.api-docs")
}

group = "org.http4k.pro"

tasks.jar {
    from(rootProject.file("pro").absolutePath) {
        include("LICENSE")
    }
}

tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    from(rootProject.file("pro").absolutePath) {
        include("LICENSE")
    }
    archiveClassifier.set("sources")
}
