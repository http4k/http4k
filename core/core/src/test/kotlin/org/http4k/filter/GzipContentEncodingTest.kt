package org.http4k.filter

import org.http4k.core.ContentEncoding
import org.http4k.core.Gzip
import java.util.zip.GZIPInputStream

class GzipContentEncodingTest : ContentEncodingContract(ContentEncoding.Gzip(), { GZIPInputStream(it.inputStream()).readBytes() })
