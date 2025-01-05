package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Result.Failed
import org.http4k.hotreload.ProjectCompiler.Companion.Result.Ok
import java.io.File

/**
 * Represents the process to compile the project for hot reload.
 */
fun interface ProjectCompiler {

    operator fun invoke(): Result

    companion object {
        sealed interface Result {
            data object Ok : Result
            data class Failed(val error: String) : Result
        }

        /**
         * Compile the project using Gradle.
         */
        fun Gradle(task: String = "compileKotlin") = ProjectCompiler {
            val process = ProcessBuilder()
                .command("./gradlew", task)
                .directory(File(".").absoluteFile)
                .start()

            try {
                if (process.waitFor() == 0) Ok else Failed(process.errorStream.reader().readText())
            } catch (e: Throwable) {
                e.printStackTrace()
                Failed(process.errorStream)
            }
        }
    }
}
