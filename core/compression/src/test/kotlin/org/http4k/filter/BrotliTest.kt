package org.http4k.filter

import com.aayushatharva.brotli4j.decoder.Decoder
import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding

class BrotliTest : ContentEncodingContract(ContentEncoding.Brotli(), { Decoder.decompress(it).decompressedData })
