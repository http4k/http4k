package org.http4k.mcp.util

import dev.forkhandles.time.executors.SimpleScheduler
import java.io.Reader
import java.time.Duration

fun SimpleScheduler.readLines(input: Reader, action: (String) -> Unit) =
    schedule({ input.buffered().lineSequence().forEach(action) }, Duration.ZERO)
