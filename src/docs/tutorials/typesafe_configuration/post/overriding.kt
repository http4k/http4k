package blog.typesafe_configuration.post

import org.http4k.cloudnative.env.Environment
import java.io.File

val systemEnv: Environment = Environment.ENV
val jvmFlags: Environment = Environment.JVM_PROPERTIES
val jar: Environment = Environment.fromResource("jar.properties")
val filesystem: Environment = Environment.from(File("fs.properties"))
val codeBased: Environment = Environment.from("key1" to "value1", "key2" to "value2")

val consolidated: Environment = jvmFlags overrides systemEnv overrides codeBased overrides filesystem overrides jar
