title: http4k Serverless Modules
description: Feature overview of the http4k-serverless modules, covering Serverless backends

### Installation (Gradle)

```groovy
// AWS Lambda: 
implementation group: "org.http4k", name: "http4k-serverless-lambda", version: "3.285.0"

// Google Cloud Functions: 
implementation group: "org.http4k", name: "http4k-serverless-gcf", version: "3.285.0"

// Apache OpenWhisk (IBM Cloud Functions): 
implementation group: "org.http4k", name: "http4k-serverless-openwhisk", version: "3.285.0"

// Azure Functions: 
implementation group: "org.http4k", name: "http4k-serverless-azure", version: "3.285.0"

// Alibaba Function Compute: 
implementation group: "org.http4k", name: "http4k-serverless-alibaba", version: "3.285.0"

// Tencent Serverless Cloud Functions: 
implementation group: "org.http4k", name: "http4k-serverless-tencent", version: "3.285.0"
```

### About
These modules provide integration with Serverless deployment environments, such as AWS Lambda or Google Cloud Functions by implementing a single interface. 

#### AWS Lambda integration
Since [http4k] is server independent, it turns out to be fairly trivial to deploy full applications to [AWS Lambda](https://aws.amazon.com/lambda), and then call them by setting up the [API Gateway](https://aws.amazon.com/api-gateway) to proxy requests to the function. Effectively, the combination of these two services become just another Server back-end supported by the library. This has the added bonus that you can test your applications in a local environment and then simply deploy them to AWS Lambda via S3 upload.

In order to achieve this, only a single interface `AppLoader` needs to be implemented and a simple extension of `AwsLambdaFunction` supplied depending on which invocation type is required - Direct, ApiGateway V1/2 or ApplicationLoadBalancer.

This is far from a complete guide, but configuring AWS Lambda and the API Gateway involves several stages:

1. Users, Roles and Policies for the API Gateway and Lambda.
1. API Gateway to proxy all requests to your Lambda.
1. Building your http4k application into a standard UberJar.
1. Optionally using Proguard to minify the JAR.
1. Package up the (minified) JAR into a standard Zip distribution.
1. Create and configure the Lambda function, and at the same time:

    1. Upload the standard Zip file to S3.
    1. Set the function execution to call the main http4k entry point: `guide.modules.serverless.lambda.FunctionsExampleEntryClass`

We hope to soon provide some tools to automate at least some of the above process, or at least document it somewhat. However, AWS is a complicated beast and many people have a preferred way to set it up: CloudFormation templates, Serverless framework, Terraform, etc. In the meantime, here is an example of how the `AppLoader` is created and how to launch the app locally:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/lambda/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/lambda/example.kt"></script>

#### Google Cloud Functions integration
Google Cloud Functions are triggered in the cloud by calling an entry point class which implements their `HttpFunction` interface.

In order to achieve this in [http4k], only a single interface `AppLoader` needs to be implemented, and then a simple extension class needs to be written which accepts this interface.

You can compose filters and handlers as usual and pass them to the constructor of the `GoogleCloudFunction` and make your entry point class extend from it.
Here is an example:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/gcf/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/gcf/example.kt"></script>

If you are using gradle, gcloud can't deploy the function directly from the project, you must build the fat jar first.
Applying this plugin [shadow jar](https://imperceptiblethoughts.com/shadow/) will provide you with appropriate gradle task to build the fat jar.

After building, and having your jar as the only file in the `libs/` folder you can deploy the function from the parent folder with : 

```gcloud functions deploy example-function --runtime=java11 --entry-point=guide.modules.serverless.gcf.FunctionsExampleEntryClass --trigger-http --source=libs/```

If you wan't to invoke functions locally you can do it with this gradle setup and passing a `-PrunFunction.target` parameter to the build task : 
```groovy
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

In order to achieve this in [http4k], only a single interface `AppLoader` needs to be implemented, and then a simple class needs to be written which uses the `OpenWhiskFunction` wrapper. Because of the OpenWhisk runtime usage of the library, a `compileOnly` dependency also needs to be added on [GSON](https://mvnrepository.com/artifact/com.google.code.gson/gson) to ensure that your function can build correctly.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/openwhisk/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/serverless/openwhisk/example.kt"></script>

Packaging of the app should be done using [ShadowJar](https://imperceptiblethoughts.com/shadow/) and then an action created with the `wsk` CLI:

```
wsk -i action create myFunctionName myApp.jar --main org.http4k.example.MyFunctionClass --web true
```

Locally, you can then just call the function with `curl`:
```
curl -k `wsk -i action get test --url | tail -1`
```

[http4k]: https://http4k.org
