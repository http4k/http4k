package org.reekwest.http.filters

import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.with
import org.reekwest.http.lens.BiDiLensSpec
import org.reekwest.http.lens.Get
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Set
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

data class ZipkinTraces(val traceId: TraceId, val spanId: TraceId, val parentSpanId: TraceId) {
    companion object {

        private val X_B3_TRACE_ID = Header.map(::TraceId, TraceId::value).optional("x-b3-traceid")
        private val X_B3_SPAN_ID = Header.map(::TraceId, TraceId::value).optional("x-b3-spanid")
        private val X_B3_PARENTSPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-parentspanid")
        private val lens = BiDiLensSpec<HttpMessage, ZipkinTraces, ZipkinTraces>("headers",
            Get { _, target ->
                listOf(ZipkinTraces(
                    X_B3_TRACE_ID(target) ?: TraceId.new(),
                    X_B3_SPAN_ID(target) ?: TraceId.new(),
                    X_B3_PARENTSPANID(target) ?: TraceId.new()
                ))
            },
            Set { _, values, target ->
                values.fold(target) { msg, (traceId, spanId, parentSpanId) ->
                    msg.with(X_B3_TRACE_ID to traceId, X_B3_SPAN_ID to spanId, X_B3_PARENTSPANID to parentSpanId)
                }
            }
        ).required("traces")

        operator fun invoke(target: HttpMessage): ZipkinTraces = lens(target)
        operator fun <T : HttpMessage> invoke(value: ZipkinTraces, target: T): T = lens(value, target)

        val THREAD_LOCAL = object : ThreadLocal<ZipkinTraces>() {
            override fun initialValue(): ZipkinTraces
            {
                println("new traces")
                return ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new())
            }
        }

    }
}