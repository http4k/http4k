@file:Suppress("UnstableApiUsage")

rootProject.name = "http4k"

pluginManagement {
    includeBuild("gradle/gradle-plugins")
}

plugins {
    id("de.fayard.refreshVersions").version("0.60.5")
}

refreshVersions {
    rejectVersionIf {
        candidate.stabilityLevel.isLessStableThan(current.stabilityLevel) ||
            setOf("milestone", "-rc", "-beta").map { it.lowercase() }.any { candidate.value.lowercase().contains(it) } ||
            Regex("""\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}.*""").matches(candidate.value) || // graphql nightlies
            candidate.value.contains("nf-execution") // graphql nightlies
    }
}

gradle.startParameter.isContinueOnFailure = true

val exclusions = setOf("gradle-plugins")

rootDir.walkTopDown()
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .filterNot { dir -> dir == rootDir || exclusions.any { dir.absolutePath.contains(it) } }
    .forEach {
        val moduleName = it.relativeTo(rootDir).path
            .removePrefix("core/") // remove core ecosystem modules as they have no prefix to add
            .removePrefix("enterprise/") // remove enterprise ecosystem modules as they have no prefix to add
            .replace('/', '-')
            .removeSuffix("-client") // replace http4k-connect client module names as they have no suffixx

        include(":http4k-$moduleName")
        project(":http4k-$moduleName").projectDir = File(it.absoluteFile.path)
    }
