package org.http4k.mcp.util

import java.io.Reader
import java.io.Writer

fun DebuggingReader(reader: Reader, writer: Writer = System.err.writer()) = object : Reader() {
    private var read = StringBuilder()

    override fun read(cbuf: CharArray, off: Int, len: Int) = reader.read(cbuf, off, len)
        .also {
            read.append(cbuf)
            if (cbuf.contains('\n')) {
                with(writer) {
                    write(read.toString())
                    flush()
                }
                read = StringBuilder()
            }
        }

    override fun close() = reader.close()
}
