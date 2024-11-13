plugins {
    id("org.http4k.nexus-config")
    id("org.http4k.conventions")
}

allprojects {
    apply(plugin = "org.http4k.conventions")
    apply(plugin = "org.http4k.code-coverage")

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(Testing.junit.jupiter.api)
        testImplementation(Testing.junit.jupiter.engine)
        testImplementation("com.natpryce:hamkrest:_")

        testFixturesImplementation(Testing.junit.jupiter.api)
        testFixturesImplementation(Testing.junit.jupiter.engine)
        testFixturesImplementation("com.natpryce:hamkrest:_")
    }
}
