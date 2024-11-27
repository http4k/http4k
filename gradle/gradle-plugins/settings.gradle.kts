@file:Suppress("UnstableApiUsage")
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
