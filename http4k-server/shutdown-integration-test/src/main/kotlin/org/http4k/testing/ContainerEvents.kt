package org.http4k.testing

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping

class ContainerEvents : Events {
    override fun invoke(event: Event) {
        val payload = ContainerEventsJackson.asFormatString(event)
        println("container_event=$payload")
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TestServerEvent.ServerStarted::class, name = "ServerStarted"),
    JsonSubTypes.Type(value = TestServerEvent.ServerStopRequested::class, name = "ServerStopRequested"),
    JsonSubTypes.Type(value = TestServerEvent.ServerStopped::class, name = "ServerStopped")
)
sealed class TestServerEvent : Event {
    data class ServerStarted(val backend: ServerBackend, val stopModeName: String) : TestServerEvent()
    data class ServerStopRequested(val ignore: String = "") : TestServerEvent()
    data class ServerStopped(val ignore: String = "") : TestServerEvent()
}

object ContainerEventsJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(BiDiMapping({ ServerBackend.valueOf(it) }, ServerBackend::name))
        .done()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
)



