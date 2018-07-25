package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.http4k.filter.SamplingDecision.Companion.from
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Header
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam
import java.util.Random

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

data class SamplingDecision(val value: String) {
    companion object {
        val SAMPLE = SamplingDecision("1")
        val DO_NOT_SAMPLE = SamplingDecision("0")

        private val VALID_VALUES = listOf("1", "0")

        fun from(sampledHeaderValue: String?): SamplingDecision =
            if (sampledHeaderValue != null && VALID_VALUES.contains(sampledHeaderValue)) {
                SamplingDecision(sampledHeaderValue)
            } else {
                SamplingDecision.SAMPLE
            }
    }
}

data class ZipkinTraces(val traceId: TraceId, val spanId: TraceId, val parentSpanId: TraceId?, val samplingDecision: SamplingDecision = SAMPLE) {
    companion object {
        private val X_B3_TRACEID = Header.map(::TraceId, TraceId::value).optional("x-b3-traceid")
        private val X_B3_SPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-spanid")
        private val X_B3_PARENTSPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-parentspanid")
        private val X_B3_SAMPLED = Header.map(SamplingDecision.Companion::from, SamplingDecision::value).optional("x-b3-sampled")

        private val lens = BiDiLensSpec<HttpMessage, ZipkinTraces>("headers",
            StringParam,
            LensGet { _, target ->
                listOf(ZipkinTraces(
                    X_B3_TRACEID(target) ?: TraceId.new(),
                    X_B3_SPANID(target) ?: TraceId.new(),
                    X_B3_PARENTSPANID(target),
                    X_B3_SAMPLED(target) ?: SAMPLE
                ))
            },
            LensSet { _, values, target ->
                values.fold(target) { msg, (traceId, spanId, parentSpanId, samplingDecision) ->
                    msg.with(X_B3_TRACEID of traceId, X_B3_SPANID of spanId, X_B3_PARENTSPANID of parentSpanId, X_B3_SAMPLED of samplingDecision)
                }
            }
        ).required("traces")

        operator fun invoke(target: HttpMessage): ZipkinTraces = lens(target)
        operator fun <T : HttpMessage> invoke(value: ZipkinTraces, target: T): T = lens(value, target)

        internal val THREAD_LOCAL = object : ThreadLocal<ZipkinTraces>() {
            override fun initialValue() = ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new(), SAMPLE)
        }
    }
}