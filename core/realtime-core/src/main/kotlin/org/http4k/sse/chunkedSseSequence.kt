package org.http4k.sse

import org.http4k.sse.SseParseState.Collecting
import org.http4k.sse.SseParseState.ConsumingTrailingLineBreak
import org.http4k.sse.SseParseState.FirstLineBreakCr
import org.http4k.sse.SseParseState.FirstLineBreakCrLf
import org.http4k.sse.SseParseState.FirstLineBreakLf
import java.io.InputStream

sealed class SseParseState {
    data class Collecting(val buffer: StringBuilder = StringBuilder()) : SseParseState()
    data class FirstLineBreakCr(val buffer: StringBuilder) : SseParseState()
    data class FirstLineBreakLf(val buffer: StringBuilder) : SseParseState()
    data class FirstLineBreakCrLf(val buffer: StringBuilder) : SseParseState()
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
                    is FirstLineBreakCr -> state.buffer
                    is FirstLineBreakLf -> state.buffer
                    is FirstLineBreakCrLf -> state.buffer
                    is ConsumingTrailingLineBreak -> null // No buffer to emit
                }
                if (finalBuffer != null && finalBuffer.isNotEmpty()) {
                    val content = finalBuffer.toString().trim()
                    if (content.isNotEmpty()) {
                        try {
                            yield(SseMessage.parse(content))
                        } catch (e: Exception) {
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
                        '\r' -> FirstLineBreakCr(state.buffer)
                        '\n' -> FirstLineBreakLf(state.buffer)
                        else -> state
                    }
                }

                is FirstLineBreakCr -> {
                    when (char) {
                        '\r' -> {
                            // \r\r - double line break found, transition to consuming state to handle potential trailing \n
                            emitMessage(state.buffer)
                            ConsumingTrailingLineBreak
                        }

                        '\n' -> {
                            state.buffer.append(char)
                            FirstLineBreakCrLf(state.buffer)
                        }

                        else -> {
                            state.buffer.append(char)
                            Collecting(state.buffer)
                        }
                    }
                }

                is ConsumingTrailingLineBreak -> {
                    when (char) {
                        // Consume the trailing \n and start fresh
                        '\n' -> Collecting()
                        else -> {
                            // Start collecting with this character
                            val newBuffer = StringBuilder()
                            newBuffer.append(char)
                            when (char) {
                                '\r' -> FirstLineBreakCr(newBuffer)
                                else -> Collecting(newBuffer)
                            }
                        }
                    }
                }

                is FirstLineBreakLf -> {
                    when (char) {
                        '\n' -> {
                            // \n\n - double line break found
                            state.buffer.append(char)
                            emitMessage(state.buffer)
                            Collecting()
                        }

                        '\r' -> {
                            // \n\r - double line break found, transition to consuming state to handle potential trailing \n
                            emitMessage(state.buffer)
                            ConsumingTrailingLineBreak
                        }

                        else -> {
                            state.buffer.append(char)
                            Collecting(state.buffer)
                        }
                    }
                }

                is FirstLineBreakCrLf -> {
                    state.buffer.append(char)
                    when (char) {
                        '\r' -> {
                            // \r\n\r - check if next is \n for \r\n\r\n
                            val nextByte = it.read()
                            if (nextByte != -1 && nextByte.toChar() == '\n') {
                                state.buffer.append('\n')
                                // \r\n\r\n - double line break found
                                emitMessage(state.buffer)
                                Collecting()
                            } else {
                                // \r\n\r followed by something else - double line break found
                                emitMessage(state.buffer)
                                val newState = Collecting()
                                if (nextByte != -1) {
                                    val nextChar = nextByte.toChar()
                                    newState.buffer.append(nextChar)
                                    when (nextChar) {
                                        '\r' -> FirstLineBreakCr(newState.buffer)
                                        '\n' -> FirstLineBreakLf(newState.buffer)
                                        else -> newState
                                    }
                                } else {
                                    newState
                                }
                            }
                        }

                        '\n' -> {
                            // \r\n\n - double line break found
                            emitMessage(state.buffer)
                            Collecting()
                        }

                        else -> Collecting(state.buffer)
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
        } catch (e: Exception) {
            // Invalid message, skip
        }
    }
    buffer.clear()
}


