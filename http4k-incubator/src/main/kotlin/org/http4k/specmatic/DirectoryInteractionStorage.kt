package org.http4k.specmatic

import `in`.specmatic.core.parseGherkinStringToFeature
import `in`.specmatic.core.toGherkinFeature
import `in`.specmatic.proxy.RealFileWriter
import org.http4k.format.JacksonYaml.asFormatString

fun DirectoryInteractionStorage(dataDirectory: String) = InteractionStorage { stubs ->
    val base = "proxy_generated"

    with(RealFileWriter(dataDirectory)) {
        createDirectory()
        writeText(
            "$base.yaml",
            asFormatString(
                parseGherkinStringToFeature(toGherkinFeature("New feature", stubs)).toOpenApi()
            )
        )

        val stubDataDirectory = subDirectory("${base}_data").apply { createDirectory() }
        stubs.mapIndexed { index, namedStub ->
            stubDataDirectory.writeText("stub$index.json", namedStub.stub.toJSON().toStringLiteral())
        }
    }
}
