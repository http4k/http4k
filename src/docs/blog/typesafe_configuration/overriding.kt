package blog.typesafe_configuration

val name = System.getProperty("USERNAME") ?: System.getenv("USERNAME") ?: "DEFAULT_USER"