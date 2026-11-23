package org.http4k.fanbelt.events

import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig

/**
 * The default JSON for Fanbelt events.
 */
internal object EventsJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().done())
