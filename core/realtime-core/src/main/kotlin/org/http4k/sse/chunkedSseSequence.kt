package org.http4k.sse

import org.http4k.sse.SseParseState.Collecting
import org.http4k.sse.SseParseState.ConsumingTrailingLineBreak
import org.http4k.sse.SseParseState.AfterSingleLineBreak
import org.http4k.sse.SseParseState.AfterCRLF
import java.io.InputStream

sealed class SseParseState {
    data class Collecting(val buffer: StringBuilder = StringBuilder()) : SseParseState()
    data class AfterSingleLineBreak(val buffer: StringBuilder, val lastChar: Char) : SseParseState()
    data class AfterCRLF(val buffer: StringBuilder) : SseParseState()
    data object ConsumingTrailingLineBreak : SseParseState()
}

fun InputStream.chunkedSseSequence(): Sequence<SseMessage> = sequence {
    use {
        var state: SseParseState = Collecting()

        while (true) {
            val byte = it.read()
            if (byte == -1) {
                // EOF - emit any remaining content
                val finalBuffer = when (state) {
                    is Collecting -> state.buffer
                    is AfterSingleLineBreak -> state.buffer
                    is AfterCRLF -> state.buffer
                    is ConsumingTrailingLineBreak -> null // No buffer to emit
                }
                if (finalBuffer != null && finalBuffer.isNotEmpty()) {
                    val content = finalBuffer.toString().trim()
                    if (content.isNotEmpty()) {
                        try {
                            yield(SseMessage.parse(content))
                        } catch (_: Exception) {
                            // Invalid message, skip
                        }
                    }
                }
                break
            }

            val char = byte.toChar()

            state = when (state) {
                is Collecting -> {
                    state.buffer.append(char)
                    when (char) {
                        '\r', '\n' -> AfterSingleLineBreak(state.buffer, char)
                        else -> state
                    }
                }

                is AfterSingleLineBreak -> {
                    when {
                        // Same line break character repeated - double line break
                        char == state.lastChar -> {
                            // Include the second \n
                            if (char == '\n') state.buffer.append(char)
                            emitMessage(state.buffer)
                            if (char == '\r') {
                                ConsumingTrailingLineBreak // \r\r might be followed by \n
                            } else {
                                Collecting()
                            }
                        }
                        // Complete CRLF sequence
                        state.lastChar == '\r' && char == '\n' -> {
                            state.buffer.append(char)
                            AfterCRLF(state.buffer)
                        }
                        // Different line break character - emit and start new
                        (state.lastChar == '\n' && char == '\r') -> {
                            emitMessage(state.buffer)
                            ConsumingTrailingLineBreak
                        }
                        // Regular character - continue collecting
                        else -> {
                            state.buffer.append(char)
                            Collecting(state.buffer)
                        }
                    }
                }
                
                is AfterCRLF -> {
                    when (char) {
                        '\r' -> {
                            // \r\n\r - double line break, handle potential trailing \n
                            emitMessage(state.buffer)
                            ConsumingTrailingLineBreak
                        }
                        '\n' -> {
                            state.buffer.append(char)
                            emitMessage(state.buffer)
                            Collecting()
                        }
                        else -> {
                            state.buffer.append(char)
                            Collecting(state.buffer)
                        }
                    }
                }

                is ConsumingTrailingLineBreak -> {
                    when (char) {
                        '\n' -> Collecting() // Consume trailing \n and start fresh
                        else -> {
                            // Start collecting with this character
                            val newBuffer = StringBuilder()
                            newBuffer.append(char)
                            when (char) {
                                '\r' -> AfterSingleLineBreak(newBuffer, char)
                                else -> Collecting(newBuffer)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun SequenceScope<SseMessage>.emitMessage(buffer: StringBuilder) {
    val content = buffer.toString().trimEnd('\r', '\n')
    if (content.isNotEmpty()) {
        try {
            yield(SseMessage.parse(content))
        } catch (_: Exception) {
            // Invalid message, skip
        }
    }
    buffer.clear()
}
