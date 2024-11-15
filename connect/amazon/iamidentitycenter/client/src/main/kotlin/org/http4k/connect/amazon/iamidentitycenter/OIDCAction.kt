package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.NonNullAutoMarshalledAction
import kotlin.reflect.KClass

abstract class OIDCAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, IAMIdentityCenterMoshi)
