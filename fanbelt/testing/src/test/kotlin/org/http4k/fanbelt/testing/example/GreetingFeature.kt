package org.http4k.fanbelt.testing.example

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.read
import org.http4k.fanbelt.state.topic
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName

class PersonName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PersonName>(::PersonName)
}

class GreetingId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<GreetingId>(::GreetingId) {
        val THE = of("the")
    }
}

data class Greeting(val text: String, val by: PersonName, override val id: GreetingId = GreetingId.THE) : Identified<GreetingId>

object GreetingFeature : Feature<Table<GreetingId, Greeting>, PersonName>({ it(TableName.of("greeting"), Greeting::class) }) {
    val greeting by state { it.get(GreetingId.THE) }

    val setter by rule { ctx, person -> if (greeting.read(ctx)?.by == person) Allowed(Unit) else Denied }

    val say by operation { greetings, person, text: String ->
        if (text.isBlank()) return@operation Rejected(FieldName.of("text"), "Say something")
        greetings.delete(GreetingId.THE)
        greetings.insert(Greeting(text.trim(), person))
        Changes.of(greeting.topic())
    }

    val clear by operation(setter) { greetings, _, _ ->
        greetings.delete(GreetingId.THE)
        Changes.of(greeting.topic())
    }
}
