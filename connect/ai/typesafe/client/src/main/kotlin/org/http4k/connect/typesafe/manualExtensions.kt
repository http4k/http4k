package org.http4k.connect.typesafe

import dev.forkhandles.result4k.map
import org.http4k.ai.model.ModelName
import org.http4k.connect.typesafe.JevModels.JevLatest
import org.http4k.connect.typesafe.action.SystemOne

/**
 * Ask a batch of questions about a single state.
 */
fun TypeSafe.systemOne(state: Any?, vararg questions: Question<*>, model: ModelName = JevLatest) =
    this(SystemOne(state, *questions, model = model))

/**
 * Ask a single question, returning its typed answer directly.
 */
fun <A : Answer> TypeSafe.ask(state: Any?, question: Question<A>, model: ModelName = JevLatest) =
    systemOne(state, question, model = model).map { it.answerTo(question) }
