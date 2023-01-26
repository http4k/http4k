package org.http4k.storage

import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.http4k.format.AutoMarshalling
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

inline fun <reified T : Any> AutoRedisCodec(autoMarshalling: AutoMarshalling) = object : RedisCodec<String, T> {
    override fun decodeKey(bytes: ByteBuffer) = UTF_8.decode(bytes).toString()

    override fun decodeValue(bytes: ByteBuffer) = autoMarshalling.asA<T>(UTF_8.decode(bytes).toString())

    override fun encodeKey(key: String) = StringCodec().encodeKey(key)

    override fun encodeValue(value: T) = UTF_8.encode(autoMarshalling.asFormatString(value))
}
