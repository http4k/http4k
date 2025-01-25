package org.http4k.mcp.stdio

import java.io.Writer

fun DebuggingWriter(writer: Writer, output: Writer = System.err.writer()) = object : Writer() {
    private var written = StringBuilder()

    override fun close() = writer.close()

    override fun flush() = writer.flush()

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        writer.write(cbuf, off, len)
            .also {
                written.append(cbuf.toList().subList(off, len).joinToString(""))
                if (cbuf.contains('\n')) {
                    output.write(written.toString())
                    written = StringBuilder()
                    flush()
                }
            }
    }
}
