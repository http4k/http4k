package org.http4k.connect.openfeature

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import kotlin.reflect.KClass

@Http4kConnectAction
abstract class OpenFeatureAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, OpenFeatureMoshi)
