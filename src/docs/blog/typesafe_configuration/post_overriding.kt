package blog.typesafe_configuration

import org.http4k.cloudnative.env.Environment

val systemEnv: Environment = Environment.ENV
val jvmFlags: Environment = Environment.JVM_PROPERTIES
val jarLoaded: Environment = Environment.fromResource("jar.properties")
val codeBased: Environment = Environment.from("key1" to "value1", "key2" to "value2")

val consolidated: Environment = jvmFlags overrides systemEnv overrides codeBased overrides jarLoaded
