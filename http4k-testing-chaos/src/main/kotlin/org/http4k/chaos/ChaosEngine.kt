package org.http4k.chaos

import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The Chaos Engine controls the lifecycle of applying Chaotic behaviour to traffic, which is exposed as a
 * standard Http4k Filter. Chaos can be programmatically updated and enabled/disabled. By default, the engine
 * is deactivated, so activate() needs to be called to witness any change in behaviour,
 */
class ChaosEngine(initialStage: Stage = Wait) : Filter {
    constructor(behaviour: Behaviour) : this(behaviour.appliedWhen(Always()))

    private val on = AtomicBoolean(false)
    private var trigger: Trigger = { on.get() }
    private val state = ChaosStages.Variable(initialStage)

    override fun invoke(p1: HttpHandler) = Repeat { Wait.until(trigger).then(state.until(!trigger)) }.asFilter()(p1)

    /**
     * Check if the configured Chaos behaviour is currently being applied to all traffic.
     */
    fun isEnabled() = on.get()

    /**
     * Turn off Chaos Engine. No Chaotic behaviour will be applied.
     */
    fun disable() = apply { on.set(false) }

    /**
     * Turn on Chaos Engine using the pre-initialised chaotic behaviour. Note that this may not actually produce any
     * effect based on the configured behaviour (e.g. if there is a specific Trigger that is condition-based.)
     */
    fun enable() = apply { on.set(true) }

    /**
     * Update with new simple Chaotic behaviour to be applied whenever the ChaosEngine is enabled.
     */
    fun enable(behaviour: Behaviour) = apply {
        state.current = behaviour.appliedWhen(Always())
        enable()
    }

    /**
     * Update the new complex (multi-stage, triggers etc) Chaotic behaviour to be applied whenever the ChaosEngine is enabled.
     */
    fun enable(stage: Stage) = apply {
        state.current = stage
        enable()
    }

    /**
     * Outputs description of the current state.
     */
    override fun toString() = if (isEnabled()) state.toString() else Wait.toString()
}
