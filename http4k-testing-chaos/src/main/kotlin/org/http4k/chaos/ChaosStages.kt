package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.then
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines a periodic element during which a particular ChaosBehaviour is active.
 */
typealias Stage = (Request) -> Filter?

/**
 * Chain the next ChaosBehaviour to apply when this stage is finished.
 */
fun Stage.then(nextStage: Stage) = object : Stage {
    override fun invoke(request: Request): Filter? = this@then(request) ?: nextStage(request)
    override fun toString() = "[${this@then}] then [$nextStage]"
}

/**
 * Stop applying the ChaosBehaviour of this stage when the ChaosTrigger fires.
 */
fun Stage.until(trigger: Trigger): Stage = object : Stage {
    private val active = AtomicBoolean(true)
    override fun invoke(request: Request): Filter? {
        if (active.get()) active.set(!trigger(request))
        return if (active.get()) this@until(request) else null
    }

    override fun toString(): String = this@until.toString() + " until " + trigger
}

/**
 * Converts this chaos stage to a standard http4k Filter.
 */
fun Stage.asFilter(): Filter = Filter { next ->
    {
        (this@asFilter(it) ?: Filter.NoOp).then(next)(it)
    }
}

object ChaosStages {
    /**
     * Repeats a stage (or composite stage in repeating pattern). Since ChaosStages are STATEFUL,
     * the stage function will be fired on each iteration and expecting a NEW instance.
     */
    fun Repeat(newStageFn: () -> Stage): Stage = object : Stage {
        private val current by lazy { AtomicReference(newStageFn()) }

        override fun invoke(request: Request): Filter? =
            current.get()(request) ?: run {
                current.set(newStageFn())
                current.get()(request)
            }

        override fun toString() = "Repeat [${current.get()}]"
    }

    /**
     * Does not apply any ChaosBehaviour.
     */
    object Wait : Stage {
        override fun invoke(request: Request) = Filter.NoOp
        override fun toString() = "Wait"
    }

    /**
     * Provide a means of modifying a ChaosBehaviour at runtime.
     */
    class Variable(internal var current: Stage = Wait) : Stage {
        override fun invoke(request: Request) = current(request)
        override fun toString() = current.toString()
    }
}

fun JsonNode.asStage(clock: Clock = Clock.systemUTC()): Stage {
    val baseStage = when (asNullable<String>("type")) {
        "wait" -> Wait
        "repeat" -> Repeat {
            this["stages"]!!
                .elements().asSequence()
                .map { it.asStage(clock) }
                .reduce { acc, next -> acc.then(next) }
        }
        "trigger" -> this["behaviour"]!!.asBehaviour().appliedWhen(this["trigger"]!!.asTrigger(clock))
        else -> throw IllegalArgumentException("unknown stage")
    }
    return this["until"]?.let { baseStage.until(it.asTrigger(clock)) } ?: baseStage
}
