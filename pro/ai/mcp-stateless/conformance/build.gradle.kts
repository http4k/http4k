description = "http4k AI MCP Conformance suite (Stateless)"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-stateless-sdk"))
    api(project(":http4k-ai-mcp-stateless-client"))
    api(project(":http4k-server-jetty"))

    testImplementation(testFixtures(project(":http4k-core")))
}

val complianceServerMainClass = "org.http4k.ai.mcp.conformance.server.McpConformanceServerKt"

tasks.register<JavaExec>("runMcpConformanceServer") {
    group = "application"
    description = "Run the MCP Conformance Server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(complianceServerMainClass)
}

tasks.register<JavaExec>("startMcpConformanceServer") {
    group = "application"
    description = "Start the MCP Conformance Server in background"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("McpConformanceServerKt")
    
    doFirst {
        val pidFile = file("${project.layout.buildDirectory}/mcp-server.pid")
        pidFile.parentFile.mkdirs()
        ProcessBuilder("java", "-cp", classpath.asPath, complianceServerMainClass).apply {
            directory(projectDir)
            val process = start()
            pidFile.writeText(process.pid().toString())
            println("Started MCP Conformance Server with PID: ${process.pid()}")
            println("PID saved to: ${pidFile.absolutePath}")
        }
    }
}

tasks.register<Exec>("stopMcpConformanceServer") {
    group = "application"
    description = "Stop the MCP Conformance Server"
    
    doFirst {
        val pidFile = file("${project.layout.buildDirectory}/mcp-server.pid")
        if (pidFile.exists()) {
            val pid = pidFile.readText().trim()
            try {
                commandLine("kill", pid)
                pidFile.delete()
                println("Stopped MCP Conformance Server with PID: $pid")
            } catch (e: Exception) {
                println("Failed to stop server: ${e.message}")
            }
        } else {
            println("No PID file found. Server may not be running.")
        }
    }
}
