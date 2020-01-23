package org.http4k.testing

import org.http4k.core.Filter
import org.http4k.core.NoOp

/**
 * Defines a test contract which can be used to implement recording or replaying of Servirtium-formatted tests
 */
interface ServirtiumContract {
    val name: String
    val manipulations: Filter get() = Filter.NoOp
}
