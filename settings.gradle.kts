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
            setOf("milestone", "-RC").map { it.lowercase() }.any { candidate.value.contains(it) } ||
            Regex("""\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}.*""").matches(candidate.value) || // graphql nightlies
            candidate.value.contains("nf-execution") // graphql nightlies
    }
}

gradle.startParameter.isContinueOnFailure = true

File(".").walkTopDown()
    .filter { it.name == "build.gradle.kts" }
    .filterNot {
        it.parent in setOf(
            ".",
            "./gradle/gradle-plugins",
            "./core/serverless/azure/integration-test/test-function"
        )
    }
    .forEach {
        val moduleRelativePath = it.parent.removePrefix("./")
        val moduleName = it.parent
            .replace("/", "-")
            .replaceFirst(".-", "")
            .replace(Regex("^core-"), "")
            .replace(Regex("^"), "http4k-")
            .replace(Regex("^http4k-connect-(.*)-client$"), "http4k-connect-$1")

        includeModule(moduleName, moduleRelativePath)
    }

fun includeModule(projectName: String, projectPath: String) {
    val moduleName = ":$projectName"
    include(moduleName)
    project(moduleName).projectDir = File(projectPath)
}
