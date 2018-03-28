package org.http4k.util


import org.junit.internal.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule private constructor(private val attempts: Int = 5) : TestRule {

    override fun apply(base: Statement, description: Description): Statement = statement(base, description)

    private fun statement(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            var caughtThrowable: Throwable? = null

            for (i in 0 until attempts) {
                try {
                    base.evaluate()
                    return
                } catch (e: AssumptionViolatedException) {
                    throw e
                } catch (t: Throwable) {
                    caughtThrowable = t
                }

                Thread.sleep(5000.toLong())
            }
            System.err.println(description.displayName + ": giving up after " + attempts + " failures.")
            throw caughtThrowable!!
        }
    }

    companion object {
        val CI = RetryRule(10)
        val LOCAL = RetryRule(1)
    }
}