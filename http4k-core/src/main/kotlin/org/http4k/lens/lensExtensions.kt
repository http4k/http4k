package org.http4k.lens

import org.http4k.filter.SamplingDecision
import org.http4k.filter.TraceId

fun StringBiDiMappings.traceId() = BiDiMapping(::TraceId, TraceId::value)
fun StringBiDiMappings.samplingDecision() = BiDiMapping(::SamplingDecision, SamplingDecision::value)

