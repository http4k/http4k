package org.http4k.connect.slack

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import kotlin.reflect.KClass

@Http4kConnectAction
abstract class SlackAction<R: Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, SlackMoshi)

