package blog.typesafe_configuration.pre

class Host(val value: String)

// export HOSTNAMES=eu-west1.aws.com,eu-west2.aws.com,eu-west3.aws.com

val hosts = System.getenv("HOSTNAMES").split(",").map { Host(it.trim()) }
