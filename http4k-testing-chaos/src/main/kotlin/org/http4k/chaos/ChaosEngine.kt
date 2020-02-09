package org.http4k.chaos

import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The Chaos Engine controls the lifecycle of applying Chaotic behaviour to traffic, which is exposed as a
 * standard Http4k Filter. Chaos can be programmatically updated and enabled/disabled.
 */
class ChaosEngine(initialStage: Stage = Wait) : Filter {
    constructor(behaviour: Behaviour) : this(behaviour.appliedWhen(Always()))

    private val on = AtomicBoolean(false)
    private val trigger: Trigger = { on.get() }
    private val state = ChaosStages.Variable(initialStage)

    override fun invoke(p1: HttpHandler) = Repeat { Wait.until(trigger).then(state.until(!trigger)) }.asFilter()(p1)

    /**
     * Check if the configured Chaos behaviour is currently being applied to all traffic.
     */
    fun isActive() = on.get()

    /**
     * Turn on Chaos behaviour
     */
    fun activate() = apply { on.set(true) }

    /**
     * Turn off Chaos behaviour
     */
    fun deactivate() = apply { on.set(false) }

    /**
     * Update the new simple Chaotic behaviour to be applied when the ChaosEngine is enabled.
     */
    fun update(behaviour: Behaviour) = apply {
        state.current = behaviour.appliedWhen(trigger)
    }

    /**
     * Update the new complex (multi-stage, triggers etc) Chaotic behaviour to be applied when the ChaosEngine is enabled.
     */
    fun update(stage: Stage) = apply {
        state.current = stage
    }

    /**
     * Outputs description of the current state.
     */
    override fun toString() = state.toString()
}
