package org.http4k.connect.typesafe

import dev.forkhandles.result4k.map
import org.http4k.ai.model.ModelName
import org.http4k.connect.typesafe.JevModels.JevLatest
import org.http4k.connect.typesafe.action.SystemOne

fun <A : Answer> TypeSafe.ask(state: Any?, question: Question<A>, model: ModelName = JevLatest) =
    this(SystemOne(state, model, question)).map { it.answerTo(question) }
