package org.http4k.connect.openfga

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import kotlin.reflect.KClass

@Http4kConnectAction
abstract class OpenFgaAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, OpenFgaMoshi)
