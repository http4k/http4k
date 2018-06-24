package org.http4k.chaos

import org.http4k.core.Filter
import org.http4k.core.with
import org.http4k.lens.Header

val Header.Common.CHAOS; get() = Header.required("x-http4k-chaos")

object ChaosFilters {
    operator fun invoke(chaosPolicy: ChaosPolicy, behaviour: ChaosBehaviour) = Filter { next ->
        {
            if (chaosPolicy.shouldInject(it)) {
                next(behaviour.inject(it)).with(Header.Common.CHAOS of behaviour.description)
            } else next(it).let {
                if (chaosPolicy.shouldInject(it)) {
                    behaviour.inject(it).with(Header.Common.CHAOS of behaviour.description)
                } else it
            }
        }
    }
}
