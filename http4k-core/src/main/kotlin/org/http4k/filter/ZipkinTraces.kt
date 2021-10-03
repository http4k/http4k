package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Header
import org.http4k.lens.composite
import org.http4k.util.Hex
import kotlin.random.Random

data class TraceId(val value: String) {
    companion object {
        fun new(random: Random = Random): TraceId {
            val randomBytes = ByteArray(8)
            random.nextBytes(randomBytes)
            return TraceId(Hex.hex(randomBytes))
        }
    }
}

data class SamplingDecision(val value: String) {
    companion object {
        val SAMPLE = SamplingDecision("1")
        val DO_NOT_SAMPLE = SamplingDecision("0")

        private val VALID_VALUES = listOf("1", "0")

        fun from(sampledHeaderValue: String?) = sampledHeaderValue?.takeIf { it in VALID_VALUES }?.let(::SamplingDecision)
            ?: SAMPLE
    }
}

data class ZipkinTraces(val traceId: TraceId, val spanId: TraceId, val parentSpanId: TraceId?, val samplingDecision: SamplingDecision = SAMPLE) {
    companion object {
        private val X_B3_TRACEID = Header.map(::TraceId, TraceId::value).optional("x-b3-traceid")
        private val X_B3_SPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-spanid")
        private val X_B3_PARENTSPANID = Header.map(::TraceId, TraceId::value).optional("x-b3-parentspanid")
        private val X_B3_SAMPLED = Header.map(SamplingDecision.Companion::from, SamplingDecision::value).optional("x-b3-sampled")

        private fun set(): ZipkinTraces.(HttpMessage) -> HttpMessage = { it: HttpMessage ->
            it.with(X_B3_TRACEID of traceId, X_B3_SPANID of spanId, X_B3_PARENTSPANID of parentSpanId, X_B3_SAMPLED of samplingDecision)
        }

        private fun get(): BiDiLensSpec<HttpMessage, String>.(HttpMessage) -> ZipkinTraces = {
            ZipkinTraces(
                X_B3_TRACEID(it) ?: TraceId.new(),
                X_B3_SPANID(it) ?: TraceId.new(),
                X_B3_PARENTSPANID(it),
                X_B3_SAMPLED(it) ?: SAMPLE
            )
        }

        private val lens = Header.composite(get(), set())

        operator fun invoke(target: HttpMessage): ZipkinTraces = lens(target)
        operator fun <T : HttpMessage> invoke(value: ZipkinTraces, target: T): T = lens(value, target)

        fun setForCurrentThread(zipkinTraces: ZipkinTraces) {
            THREAD_LOCAL.set(zipkinTraces)
        }

        fun forCurrentThread(): ZipkinTraces = THREAD_LOCAL.get()

        internal val THREAD_LOCAL = object : ThreadLocal<ZipkinTraces>() {
            override fun initialValue() = ZipkinTraces(TraceId.new(), TraceId.new(), null, SAMPLE)
        }
    }
}
