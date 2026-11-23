package todo.store

import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import todo.domain.TodoId
import todo.domain.TodoListId
import todo.domain.TodoTitle

object TodoJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(TodoListId)
        .value(TodoId)
        .value(TodoTitle)
        .done()
)
