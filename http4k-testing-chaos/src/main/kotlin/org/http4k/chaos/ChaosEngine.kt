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
class ChaosEngine(initialStage: Stage = Wait, alreadyActivated: Boolean = true) : Filter {
    constructor(behaviour: Behaviour, alreadyActivated: Boolean = true): this(behaviour.appliedWhen(Always()), alreadyActivated)

    private val on = AtomicBoolean(alreadyActivated)
    private val trigger: Trigger = { on.get() }
    private val state = ChaosStages.Variable(initialStage)

    override fun invoke(p1: HttpHandler) = Repeat { Wait.until(trigger).then(state.until(!trigger)) }.asFilter()(p1)

    /**
     * Check if the configured Chaos behaviour is currently being applied to all traffic.
     */
    fun isActive() = on.get()

    /**
     * Toggle the behaviour on/off.
     */
    fun toggle(isActive: Boolean) = on.set(isActive)

    /**
     * Update the new simple Chaotic behaviour to be applied when the ChaosEngine is enabled.
     */
    fun update(behaviour: Behaviour) {
        state.current = behaviour.appliedWhen(trigger)
    }

    /**
     * Update the new complex (multi-stage, triggers etc) Chaotic behaviour to be applied when the ChaosEngine is enabled.
     */
    fun update(stage: Stage) {
        state.current = stage
    }

    /**
     * Outputs description of the current state.
     */
    override fun toString() = state.toString()
}
