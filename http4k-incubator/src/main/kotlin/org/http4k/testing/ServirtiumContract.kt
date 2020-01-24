package org.http4k.testing

import org.http4k.core.Filter
import org.http4k.core.NoOp

/**
 * Defines a test contract which can be used to implement recording or replaying of Servirtium-formatted tests
 */
interface ServirtiumContract {
    /**
     * Provides the base name for this contract's test cases.
     */
    val name: String

    /**
     * Custom manipulations to apply to the request and responses going into the recorded version of the contract
     * HTTP traffic.
     */
    val manipulations get() = Filter.NoOp
}
