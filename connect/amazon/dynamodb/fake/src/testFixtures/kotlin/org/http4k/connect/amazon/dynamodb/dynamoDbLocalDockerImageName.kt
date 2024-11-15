package org.http4k.connect.amazon.dynamodb

import org.testcontainers.utility.DockerImageName

val dynamoDbLocalDockerImageName = DockerImageName.parse("amazon/dynamodb-local:2.2.1")
