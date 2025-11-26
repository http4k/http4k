package org.http4k.testing

import com.fasterxml.jackson.annotation.JsonInclude
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant

class ContainerEvents : Events {
    override fun invoke(event: Event) {
        val payload = ContainerEventsJackson.asFormatString(ContainerEvent(Instant.now(), event))
        println("container_event=$payload")
    }
}

sealed class TestServerEvent : Event {
    data class ApplicationStarted(val ignored: String = "") : TestServerEvent()
    data class ServerStarted(val backend: String, val stopModeName: String) : TestServerEvent()
    data class ServerStopRequested(val ignore: String = "") : TestServerEvent()
    data class ServerStopped(val ignore: String = "") : TestServerEvent()
}

data class ContainerEvent(val timestamp: Instant, val event: Event) : Event

object ContainerEventsJackson : ConfigurableJackson(
    KotlinModule.Builder()
        .build()
        .asConfigurable(
            JsonMapper.builder().activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(TestServerEvent::class.java).build()
            )
                .changeDefaultPropertyInclusion { incl ->
                    incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                })
        .withStandardMappings()
        .text(BiDiMapping({ ServerBackend.valueOf(it) }, ServerBackend::name))
        .done()
)
