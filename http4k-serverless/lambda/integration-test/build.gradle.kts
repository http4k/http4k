dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-cloudnative"))
    api(project(":http4k-serverless-lambda"))
    api(project(":http4k-aws"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-client-okhttp"))

    api("com.amazonaws:aws-lambda-java-events:_")
    api("dev.forkhandles:result4k:_")
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    testImplementation(testFixtures(project(":http4k-aws")))
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
