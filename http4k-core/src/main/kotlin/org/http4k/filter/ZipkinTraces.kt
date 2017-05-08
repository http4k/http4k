package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Get
import org.http4k.lens.Header
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.Set
import java.util.*
import kotlin.experimental.and

data class TraceId(val value: String) {
    companion object {
        private val TraceRandom = Random()
        fun new(random: Random = TraceRandom): TraceId {
            val randomBytes = ByteArray(8)
            random.nextBytes(randomBytes)
            val sb = StringBuilder(randomBytes.size * 2)
            for (b in randomBytes) {
                sb.append(String.format("%02x", b and 0xff.toByte()))
            }
            return TraceId(sb.toString())
        }
    }
}

data class ZipkinTraces(val traceId: TraceId, val spanId: TraceId, val parentSpanId: TraceId?) {
    companion object {
        private val X_B3_TRACEID = Header.map(::TraceId, TraceId::value).optional("x-b3-traceid")
        private val X_B3_SPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-spanid")
        private val X_B3_PARENTSPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-parentspanid")

        private val lens = BiDiLensSpec<HttpMessage, ZipkinTraces, ZipkinTraces>("headers",
            StringParam,
            Get { _, target ->
                listOf(ZipkinTraces(
                    X_B3_TRACEID(target) ?: TraceId.new(),
                    X_B3_SPANID(target) ?: TraceId.new(),
                    X_B3_PARENTSPANID(target)
                ))
            },
            Set { _, values, target ->
                values.fold(target) { msg, (traceId, spanId, parentSpanId) ->
                    msg.with(X_B3_TRACEID to traceId, X_B3_SPANID to spanId, X_B3_PARENTSPANID to parentSpanId)
                }
            }
        ).required("traces")

        operator fun invoke(target: HttpMessage): ZipkinTraces = lens(target)
        operator fun <T : HttpMessage> invoke(value: ZipkinTraces, target: T): T = lens(value, target)

        val THREAD_LOCAL = object : ThreadLocal<ZipkinTraces>() {
            override fun initialValue() = ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new())
        }
    }
}