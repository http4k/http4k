package org.http4k.chaos

import org.http4k.core.Filter

const val LATENCY_HEADER = "X-Chaos-Behaviour"

object ChaosFilters {
    object ChaosPreFilter {
        operator fun invoke(chaosPolicy: ChaosPolicy, behaviour: ChaosBehaviour): Filter = Filter { next ->
            { request ->
                if (chaosPolicy.shouldInject(request)) {
                    behaviour.inject(request)
                    next(request).header(LATENCY_HEADER, behaviour.javaClass.name)
                } else {
                    next(request)
                }
            }
        }
    }

    object ChaosPostFilter {
        operator fun invoke(chaosPolicy: ChaosPolicy, behaviour: ChaosBehaviour): Filter = Filter { next ->
            { request ->
                val response = next(request)
                if (chaosPolicy.shouldInject(response)) {
                    behaviour.inject(response)
                    response.header(LATENCY_HEADER, behaviour.javaClass.name)
                } else {
                    response
                }
            }
        }
    }
}
