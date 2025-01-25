plugins {
    id("org.http4k.conventions")
}

dependencies {
    testImplementation(testFixtures(project(":http4k-aws")))
    testImplementation(testFixtures(project(":http4k-serverless-lambda")))
}

task<JavaExec>("deployTestFunction") {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContractKt")

    dependsOn("test-function:buildZip")
}

task<JavaExec>("deployHttpApiGateway") {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployHttpApiGatewayKt")
}

task<JavaExec>("deployRestApiGateway") {
    classpath += sourceSets.getByName("test").runtimeClasspath
    mainClass.set("org.http4k.serverless.lambda.testing.setup.DeployRestApiGatewayKt")
}
