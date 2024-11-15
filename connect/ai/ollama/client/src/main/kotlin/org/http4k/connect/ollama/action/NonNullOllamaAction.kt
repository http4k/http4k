package org.http4k.connect.ollama.action

import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.ollama.OllamaAction
import org.http4k.connect.ollama.OllamaMoshi
import kotlin.reflect.KClass

abstract class NonNullOllamaAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, OllamaMoshi),
    OllamaAction<R>
