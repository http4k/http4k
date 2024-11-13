plugins {
    kotlin("jvm")
    idea
    `java-library`
    `java-test-fixtures`
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
    dependsOn(tasks.named("classes"))
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("javadoc").get().destinationDir)
    dependsOn(tasks.named("javadoc"))
}

val testJar by tasks.creating(Jar::class) {
    archiveClassifier.set("test")
    from(project.the<SourceSetContainer>()["test"].output)
}

tasks.named<Jar>("jar") {
    manifest {
        val projectName = rootProject.name.replace('-', '_')
        attributes(mapOf("${projectName}_version" to archiveVersion))
    }
}
