package org.http4k.servirtium

/**
 * Defines a test contract which can be used to implement recording or replaying of Servirtium-formatted tests
 */
interface ServirtiumContract {
    /**
     * Provides the base name for this contract's test cases.
     */
    val name: String
}
