title: http4k Quickstart
description: Easy ways to get started using http4k

# Quickstart 

This simple example demonstrates how to serve and consume HTTP services using **http4k**. To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation platform("org.http4k:http4k-bom:3.266.0")
    implementation "org.http4k:http4k-core"
    implementation "org.http4k:http4k-server-netty"
    implementation "org.http4k:http4k-client-apache"
}
```

The following creates a simple endpoint, binds it to a Netty server then starts, queries, and stops it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>

# Examples Repo
For fully self-contained examples demonstrates the standout features of **http4k**, there is a GitHub repository at [http4k/examples](https://github.com/http4k/examples).

# Single-line CD Bootstrap
Run the single command in the readme of [this repo](https://github.com/http4k/http4k-bootstrap) to create a HelloWorld **http4k** app with a full CD pipeline using Github -> TravisCI -> Heroku.
