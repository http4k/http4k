package org.http4k.servirtium

import org.http4k.core.Request
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.servirtium.RecordingControl.Companion.NoOp
import org.http4k.traffic.ByteStorage
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.replayingMatchingContent
import java.io.File

/**
 * Replay server which will match and replay recorded traffic read from the named Servirtium Markdown file.
 * Incoming requests can be manipulated to ensure that it matches the expected request.
 */
class ServirtiumReplayServer(
    name: String,
    root: File = File("."),
    port: Int = 0,
    requestManipulations: (Request) -> Request = { it }
) : Http4kServer by
Replay.Servirtium(ByteStorage.Disk(File(root, "$name.md")))
    .replayingMatchingContent(requestManipulations)
    .asServer(SunHttp(port)),
    RecordingControl by NoOp
