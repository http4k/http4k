package org.http4k.hotreload

import org.http4k.hotreload.CompileProject.Companion.Result.Failed
import org.http4k.hotreload.CompileProject.Companion.Result.Ok
import java.io.File
import java.io.InputStream

/**
 * Represents the process to compile the project for hot reload.
 */
fun interface CompileProject {

    operator fun invoke(): Result

    companion object {
        sealed interface Result {
            data object Ok : Result
            data class Failed(val errorStream: InputStream) : Result
        }

        /**
         * Compile the project using Gradle.
         */
        fun Gradle(task: String = "compileKotlin") = CompileProject {
            val process = ProcessBuilder()
                .command("./gradlew", task)
                .directory(File(".").absoluteFile)
                .start()

            try {
                if (process.waitFor() == 0) Ok else Failed(process.errorStream)
            } catch (e: Throwable) {
                e.printStackTrace()
                Failed(process.errorStream)
            }
        }
    }
}
