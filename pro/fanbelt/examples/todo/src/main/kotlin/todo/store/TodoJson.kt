package todo.store

import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.withFanbeltMappings
import todo.domain.TodoId
import todo.domain.TodoTitle

object TodoJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(TodoId)
        .value(TodoTitle)
        .done()
)