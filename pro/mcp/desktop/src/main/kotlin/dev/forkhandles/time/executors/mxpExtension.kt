package dev.forkhandles.time.executors

import java.io.Reader
import java.time.Duration

fun SimpleScheduler.readLines(input: Reader, action: (String) -> Unit) {
    schedule({ input.buffered().lineSequence().forEach(action) }, Duration.ZERO)
}
