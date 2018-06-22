package org.http4k.chaos

import org.http4k.core.Filter

const val CHAOS_HEADER = "X-Http4k-Chaos"

object ChaosFilter {
    operator fun invoke(chaosPolicy: ChaosPolicy, behaviour: ChaosBehaviour): Filter = Filter { next ->
        { request ->
            val response = if (chaosPolicy.shouldInject(request)) {
                val injectedRequest = behaviour.inject(request)
                next(injectedRequest).header(CHAOS_HEADER, behaviour.description)
            } else {
                next(request)
            }
            if (chaosPolicy.shouldInject(response)) {
                val injectedResponse = behaviour.inject(response)
                injectedResponse.header(CHAOS_HEADER, behaviour.description)
            } else {
                response
            }
        }
    }
}
