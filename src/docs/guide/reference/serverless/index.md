title: http4k Serverless Modules
description: Feature overview of the http4k-serverless modules, covering Serverless backends

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.46.0.0"))

    // AWS Lambda: 
    implementation("org.http4k:http4k-serverless-lambda")

    // Google Cloud Functions: 
    implementation("org.http4k:http4k-serverless-gcf")

    // Apache OpenWhisk (IBM Cloud Functions): 
    implementation("org.http4k:http4k-serverless-openwhisk")

    // Azure Functions: 
    implementation("org.http4k:http4k-serverless-azure")

    // Alibaba Function Compute: 
    implementation("org.http4k:http4k-serverless-alibaba")

    // Tencent Serverless Cloud Functions: 
    implementation("org.http4k:http4k-serverless-tencent")
}
```

### About
These modules provide integration with Serverless deployment environments, such as AWS Lambda or Google Cloud Functions by implementing a single interface. 

#### AWS Lambda integration (HTTP apps)
Since http4k is server independent, it turns out to be fairly trivial to deploy full applications to [AWS Lambda](https://aws.amazon.com/lambda), and then call them by setting up the [API Gateway](https://aws.amazon.com/api-gateway) to proxy requests to the function. Effectively, the combination of these two services become just another Server back-end supported by the library. This has the added bonus that you can test your applications in a local environment and then simply deploy them to AWS Lambda via S3 upload.

In order to achieve this, only a single interface `AppLoader` needs to be implemented and a simple extension of `AwsLambdaFunction` supplied depending on which invocation type is required - Direct, ApiGateway V1/2 or ApplicationLoadBalancer.

This is far from a complete guide, but configuring AWS Lambda and the API Gateway involves several stages:

1. Users, Roles and Policies for the API Gateway and Lambda.
2. API Gateway to proxy all requests to your Lambda.
3. Building your http4k application into a standard UberJar.
4. Optionally using Proguard to minify the JAR.
5. Package up the (minified) JAR into a standard Zip distribution.
6. Create and configure the Lambda function, and at the same time:
    - Upload the standard Zip file to S3.
    - Set the function execution to call the main http4k entry point: `guide.modules.serverless.lambda.FunctionsExampleEntryClass`

We hope to soon provide some tools to automate at least some of the above process, or at least document it somewhat. However, AWS is a complicated beast and many people have a preferred way to set it up: CloudFormation templates, Serverless framework, Terraform, etc. In the meantime, here is an example of how the `AppLoader` is created and how to launch the app locally:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/lambda/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/lambda/example.kt"></script>

#### AWS Lambda integration (Event-based apps)
http4k also supports writing Event-based functions to receive AWS events from services like SQS and Dynamo. One advantage of using http4k version is that it uses the AWS SDK RequestStreamHandler instead of the standard RequestHandler - which avoids the heavyweight Jackson deserialisation process (we use Moshi under the covers) utilised by the standard AWS runtime. To use this events functionality, you should also import the AWS Events JAR:

```kotlin
implementation "com.amazonaws:aws-lambda-java-events:3.8.0"
```

Similarly to HttpHandler, for event processing in a functional style, the main body of the Lambda function is encapsulated in a single interface `FnHandler`. This typesafe class is created by an `FnLoader` function and simply passed into an extension of `AwsLambdaEventFunction` - which is the class configured as the entry point of your AWS lambda.

The process of configuration is the same as for HTTP apps above.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/http4k-serverless/lambda/src/examples/kotlin/example_event_handling.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/http4k-serverless/lambda/src/examples/kotlin/example_event_handling.kt"></script>

#### Google Cloud Functions integration
Google Cloud Functions are triggered in the cloud by calling an entry point class which implements their `HttpFunction` interface.

In order to achieve this in http4k, only a single interface `AppLoader` needs to be implemented, and then a simple extension class needs to be written which accepts this interface.

You can compose filters and handlers as usual and pass them to the constructor of the `GoogleCloudFunction` and make your entry point class extend from it.
Here is an example:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/gcf/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/gcf/example.kt"></script>

If you are using gradle, gcloud can't deploy the function directly from the project, you must build the fat jar first.
Applying this plugin [shadow jar](https://imperceptiblethoughts.com/shadow/) will provide you with appropriate gradle task to build the fat jar.

After building, and having your jar as the only file in the `libs/` folder you can deploy the function from the parent folder with : 

```gcloud functions deploy example-function --runtime=java11 --entry-point=guide.modules.serverless.gcf.FunctionsExampleEntryClass --trigger-http --source=libs/```

If you wan't to invoke functions locally you can do it with this gradle setup and passing a `-PrunFunction.target` parameter to the build task : 
```kotlin
configurations {
    invoker
}

dependencies {
    invoker 'com.google.cloud.functions.invoker:java-function-invoker:1.0.0-alpha-2-rc5'
}

tasks.register("runFunction", JavaExec) {
    main = 'com.google.cloud.functions.invoker.runner.Invoker'
    classpath(configurations.invoker)
    inputs.files(configurations.runtimeClasspath, sourceSets.main.output)
    args(
            '--target', project.findProperty('runFunction.target'),
            '--port', project.findProperty('runFunction.port') ?: 8080
    )
    doFirst {
        args('--classpath', files(configurations.runtimeClasspath, sourceSets.main.output).asPath)
    }
}
```

If you are using Maven, you do not have to build the fat JAR and can deploy the function from the project folder.
Simple example on how to setup `pom.xml` to run functions locally and deploy Maven project to the cloud is shown [here](https://cloud.google.com/functions/docs/first-java)

#### Apache OpenWhisk integration
OpenWhisk has a Java runtime which is triggered by calling an entry point class which contains a static `main()` function receiving a GSON `JsonObject`.

In order to achieve this in http4k, only a single interface `AppLoader` needs to be implemented, and then a simple class needs to be written which uses the `OpenWhiskFunction` wrapper. Because of the OpenWhisk runtime usage of the library, a `compileOnly` dependency also needs to be added on [GSON](https://mvnrepository.com/artifact/com.google.code.gson/gson) to ensure that your function can build correctly.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/openwhisk/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/serverless/openwhisk/example.kt"></script>

Packaging of the app should be done using [ShadowJar](https://imperceptiblethoughts.com/shadow/) and then an action created with the `wsk` CLI:

```
wsk -i action create myFunctionName myApp.jar --main org.http4k.example.MyFunctionClass --web true
```

Locally, you can then just call the function with `curl`:
```
curl -k `wsk -i action get test --url | tail -1`
```

[http4k]: https://http4k.org
