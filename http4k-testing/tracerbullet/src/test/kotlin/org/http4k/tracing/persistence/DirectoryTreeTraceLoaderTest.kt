package org.http4k.tracing.persistence

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TraceLoader
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.entire_trace_1
import org.http4k.tracing.entire_trace_2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files

@ExtendWith(JsonApprovalTest::class)
class DirectoryTreeTraceLoaderTest {
    @Test
    fun `can load all traces by walking the directory structure`(approver: Approver) {

        fun File.toName(i: Int) = absolutePath.substringAfter("root").filter { it.isLetterOrDigit() } + i

        fun populate(dir: File) {
            with(TracePersistence.FileSystem(dir)) {
                store(ScenarioTraces(dir.toName(1), listOf(entire_trace_1)))
                store(ScenarioTraces(dir.toName(2), listOf(entire_trace_2)))
            }
        }

        val tempDir = Files.createTempDirectory("").toFile()

        val root = File(tempDir, "root").also(::populate)
        val child1 = File(root, "1").also(::populate)
        File(root, "2").also(::populate)
        File(child1, "1").also(::populate)

        approver.assertApproved(
            Response(OK)
                .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
                .body(TraceLoader.DirectoryTree(root).load().toJson())
        )
    }
}

fun Iterable<ScenarioTraces>.toJson() = TraceMoshi.asFormatString(this)
