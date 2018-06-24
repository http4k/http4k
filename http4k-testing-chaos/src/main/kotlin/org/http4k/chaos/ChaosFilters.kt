package org.http4k.chaos

import org.http4k.core.Filter
import org.http4k.lens.Header

val Header.Common.CHAOS; get() = Header.required("x-http4k-chaos")

object ChaosFilters {
    operator fun invoke(chaosPeriod: ChaosPeriod) = Filter { next -> { chaosPeriod(next(chaosPeriod(it))) } }
}