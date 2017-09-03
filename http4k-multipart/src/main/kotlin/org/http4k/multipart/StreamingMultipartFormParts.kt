package org.http4k.multipart

import org.apache.commons.fileupload.util.ParameterParser
import org.http4k.multipart.exceptions.AlreadyClosedException
import org.http4k.multipart.exceptions.ParseError
import org.http4k.multipart.exceptions.TokenNotFoundException
import org.http4k.multipart.part.StreamingPart
import org.http4k.multipart.stream.TokenBoundedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

/**
 * [RFC 1867](http://www.ietf.org/rfc/rfc1867.txt)
 */
class StreamingMultipartFormParts private constructor(boundary: ByteArray, private val encoding: Charset, private val inputStream: TokenBoundedInputStream) : Iterable<StreamingPart> {
    private val iterator: Iterator<StreamingPart>

    private var boundary: ByteArray? = null
    private var boundaryWithPrefix: ByteArray? = null
    private var state: MultipartFormStreamState? = null
    // yes yes, I should use a stack or something for this
    private var mixedName: String? = null
    private var oldBoundary: ByteArray? = null
    private var oldBoundaryWithPrefix: ByteArray? = null

    init {
        this.boundary = prependBoundaryWithStreamTerminator(boundary)

        this.boundaryWithPrefix = addPrefixToBoundary(this.boundary)

        state = MultipartFormStreamState.findBoundary
        iterator = StreamingMulipartFormPartIterator()
    }

    override fun iterator(): Iterator<StreamingPart> {
        return iterator
    }

    private fun addPrefixToBoundary(boundary: ByteArray?): ByteArray {
        val b = ByteArray(boundary!!.size + FIELD_SEPARATOR.size) // in apache they just use BOUNDARY_PREFIX
        System.arraycopy(boundary, 0, b, 2, boundary.size)
        System.arraycopy(FIELD_SEPARATOR, 0, b, 0, FIELD_SEPARATOR.size)
        return b
    }


    private fun findBoundary() {
        if (state == MultipartFormStreamState.findPrefix) {
            if (!inputStream.matchInStream(FIELD_SEPARATOR)) {
                throw TokenNotFoundException("Boundary must be proceeded by field separator, but didn't find it")
            }
            state = MultipartFormStreamState.findBoundary
        }

        if (state == MultipartFormStreamState.findBoundary) {
            if (!inputStream.matchInStream(boundary!!)) {
                throw TokenNotFoundException("Boundary not found <<" + String(boundary!!, encoding) + ">>")
            }
        }

        state = MultipartFormStreamState.boundaryFound
        if (inputStream.matchInStream(STREAM_TERMINATOR)) {
            if (inputStream.matchInStream(FIELD_SEPARATOR)) {
                if (mixedName != null) {
                    boundary = oldBoundary
                    boundaryWithPrefix = oldBoundaryWithPrefix
                    mixedName = null

                    state = MultipartFormStreamState.findBoundary
                    findBoundary()
                } else {
                    state = MultipartFormStreamState.eos
                }
            } else {
                throw TokenNotFoundException("Stream terminator must be followed by field separator, but didn't find it")
            }
        } else {
            if (!inputStream.matchInStream(FIELD_SEPARATOR)) {
                throw TokenNotFoundException("Boundary must be followed by field separator, but didn't find it")
            } else {
                state = MultipartFormStreamState.header
            }
        }
    }


    private fun parseNextPart(): StreamingPart? {
        findBoundary()
        return if (state == MultipartFormStreamState.header) parsePart() else null
    }

    private fun parsePart(): StreamingPart? {
        val headers = parseHeaderLines()

        val contentType = headers["Content-Type"]
        if (contentType != null && contentType.startsWith("multipart/mixed")) {
            val contentDisposition = ParameterParser().parse(headers["Content-Disposition"], ';')
            val contentTypeParams = ParameterParser().parse(contentType, ';')

            mixedName = trim(contentDisposition["name"])

            oldBoundary = boundary
            oldBoundaryWithPrefix = boundaryWithPrefix
            boundary = (String(STREAM_TERMINATOR, encoding) + trim(contentTypeParams["boundary"])!!).toByteArray(encoding)
            boundaryWithPrefix = addPrefixToBoundary(this.boundary)

            state = MultipartFormStreamState.findBoundary

            return parseNextPart()
        } else {
            val contentDisposition = ParameterParser().parse(headers["Content-Disposition"], ';')
            val fieldName = (if (contentDisposition.containsKey("attachment")) mixedName else trim(contentDisposition["name"])) ?: throw ParseError("no name for part")
            val filename = filenameFromMap(contentDisposition)

            return StreamingPart(
                fieldName,
                !contentDisposition.containsKey("filename"),
                contentType,
                filename,
                BoundedInputStream(), headers)
        }
    }

    private fun filenameFromMap(contentDisposition: Map<String, String>): String? =
        if (contentDisposition.containsKey("filename")) trim(contentDisposition["filename"] ?: "") else null


    private fun trim(string: String?): String? = string?.trim { it <= ' ' }


    private fun parseHeaderLines(): Map<String, String> {
        if (MultipartFormStreamState.header != state) {
            throw IllegalStateException("Expected state ${MultipartFormStreamState.header} but got $state")
        }

        val result = HashMap<String, String>()
        var previousHeaderName: String? = null
        val maxByteIndexForHeader = inputStream.currentByteIndex() + HEADER_SIZE_MAX
        while (inputStream.currentByteIndex() < maxByteIndexForHeader) {
            val header = readStringFromStreamUntilMatched(inputStream, FIELD_SEPARATOR, (maxByteIndexForHeader - inputStream.currentByteIndex()).toInt(), encoding)
            if (header == "") {
                state = MultipartFormStreamState.contents
                return result
            }
            if (header.matches("\\s+.*".toRegex())) {
                result.put(previousHeaderName!!, result[previousHeaderName] + "; " + header.trim { it <= ' ' })
            } else {
                val index = header.indexOf(":")
                if (index < 0) {
                    throw ParseError("Header didn't include a colon <<$header>>")
                } else {
                    previousHeaderName = header.substring(0, index).trim { it <= ' ' }
                    result.put(previousHeaderName, header.substring(index + 1).trim { it <= ' ' })
                }
            }
        }
        throw TokenNotFoundException("Didn't find end of Header section within $HEADER_SIZE_MAX bytes")
    }

    inner class StreamingMulipartFormPartIterator : Iterator<StreamingPart> {
        private var nextIsKnown: Boolean = false
        private var currentPart: StreamingPart? = null

        override fun hasNext(): Boolean {
            if (nextIsKnown) {
                return !isEndOfStream
            }
            nextIsKnown = true

            if (state == MultipartFormStreamState.contents) {
                currentPart!!.inputStream.close()
            }

            currentPart = safelyParseNextPart()

            return !isEndOfStream
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         * @throws ParseError             if there was a problem parsing the stream
         */
        override fun next(): StreamingPart {
            if (nextIsKnown) {
                if (isEndOfStream) {
                    throw NoSuchElementException("No more parts in this MultipartForm")
                }
                nextIsKnown = false
            } else {

                if (state == MultipartFormStreamState.contents) {
                    currentPart!!.inputStream.close()
                }

                currentPart = safelyParseNextPart()
                if (isEndOfStream) {
                    throw NoSuchElementException("No more parts in this MultipartForm")
                }
            }
            return currentPart!!
        }

        private fun safelyParseNextPart(): StreamingPart? =
            try {
                parseNextPart()
            } catch (e: IOException) {
                nextIsKnown = true
                currentPart = null
                throw ParseError(e)
            }

        private val isEndOfStream: Boolean
            get() = currentPart == null

    }

    private inner class BoundedInputStream : InputStream() {

        internal var endOfStream = false
        internal var closed = false

        override fun read(): Int = if (closed) throw AlreadyClosedException() else if (endOfStream) -1 else readNextByte()

        private fun readNextByte(): Int {
            val result = inputStream.readByteFromStreamUnlessTokenMatched(boundaryWithPrefix!!)
            return when (result) {
                -1 -> {
                    state = MultipartFormStreamState.findPrefix
                    endOfStream = true
                    -1
                }
                -2 -> {
                    state = MultipartFormStreamState.boundaryFound
                    endOfStream = true
                    -1 // inputStream.read(byte b[], int off, int len) checks for exactly -1
                }
                else -> result
            }
        }

        override fun close() {
            closed = true
            if (!endOfStream) {
                try {
                    while (readNextByte() >= 0) {
                        // drop unwanted bytes :(
                    }
                } catch (e: IOException) {
                    endOfStream = true
                    throw ParseError(e)
                }

            }
        }
    }

    private enum class MultipartFormStreamState {
        findPrefix, findBoundary, boundaryFound, eos, header, contents, error
    }

    companion object {
        private val DEFAULT_BUFSIZE = 4096

        /**
         * The Carriage Return ASCII character value.
         */
        private val CR: Byte = 0x0D

        /**
         * The Line Feed ASCII character value.
         */
        private val LF: Byte = 0x0A

        /**
         * The dash (-) ASCII character value.
         */
        private val DASH: Byte = 0x2D

        /**
         * The maximum length of all headers
         */
        val HEADER_SIZE_MAX = 10 * 1024

        /**
         * A byte sequence that that follows a delimiter that will be
         * followed by an encapsulation (`CRLF`).
         */
        val FIELD_SEPARATOR = byteArrayOf(CR, LF)

        /**
         * A byte sequence that that follows a delimiter of the last
         * encapsulation in the stream (`--`).
         */
        val STREAM_TERMINATOR = byteArrayOf(DASH, DASH)

        /**
         * Uses the `boundary` to parse the `encoding` coded `inputStream`,
         * returning an `Iterable` of `StreamingPart`s.
         * <br></br>
         * You need to look after closing the inputStream yourself.
         * <br></br>
         * The Iterable can throw a number of `ParseError` Exceptions that you should look
         * out for. Sorry :( They will only happen if something goes wrong with the parsing, which
         * it shouldn't ;)
         *
         * @param boundary    byte array defining the boundary between parts, usually found in the
         * Content-Type header of an HTTP request
         * @param inputStream of the body of an HTTP request - will be parsed as `multipart/form-data`
         * @param encoding    of the body of the HTTP request
         * @return an `Iterable<StreamingPart>` that you can for() through to get each part
         * @throws IOException
         */
        fun parse(boundary: ByteArray, inputStream: InputStream, encoding: Charset): Iterable<StreamingPart> {
            return StreamingMultipartFormParts(boundary, encoding, TokenBoundedInputStream(inputStream, DEFAULT_BUFSIZE))
        }

        fun parse(boundary: ByteArray, inputStream: InputStream, encoding: Charset, maxStreamLength: Int): Iterable<StreamingPart> {
            return StreamingMultipartFormParts(boundary, encoding, TokenBoundedInputStream(inputStream, DEFAULT_BUFSIZE, maxStreamLength))
        }

        fun prependBoundaryWithStreamTerminator(boundary: ByteArray): ByteArray {
            val actualBoundary = ByteArray(boundary.size + 2)
            System.arraycopy(STREAM_TERMINATOR, 0, actualBoundary, 0, 2)
            System.arraycopy(boundary, 0, actualBoundary, 2, boundary.size)
            return actualBoundary
        }


        fun readStringFromStreamUntilMatched(tokenBoundedInputStream: TokenBoundedInputStream, endOfToken: ByteArray, maxStringSizeInBytes: Int, encoding: Charset): String {
            // very inefficient search!
            val buffer = ByteArray(maxStringSizeInBytes)
            val bytes = tokenBoundedInputStream.getBytesUntil(endOfToken, buffer, encoding)
            return String(buffer, 0, bytes, encoding)
        }
    }
}
