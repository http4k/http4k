plugins {
    id("org.http4k.conventions")
}

dependencies {
    testImplementation(testFixtures(project(":http4k-platform-aws")))
    testImplementation(testFixtures(project(":http4k-serverless-lambda")))
}

tasks.register<JavaExec>("deployTestFunction", fun JavaExec.() {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContractKt")

    dependsOn("test-function:buildZip")
})

tasks.register<JavaExec>("deployHttpApiGateway", fun JavaExec.() {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployHttpApiGatewayKt")
})

tasks.register<JavaExec>("deployRestApiGateway", fun JavaExec.() {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployRestApiGatewayKt")
})
