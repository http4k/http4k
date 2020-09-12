title: http4k Quickstart
description: Easy ways to get started using http4k

# Quickstart 

For the impatient, there is a repository of examples at [http4k/examples](https://github.com/http4k/examples).

... alternatively, for a step-by-step guide, keep reading...

This simple example demonstrates how to serve and consume HTTP services using **http4k**. 

To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation platform("org.http4k:http4k-bom:3.260.0")
    implementation "org.http4k:http4k-core"
    implementation "org.http4k:http4k-server-jetty"
    implementation "org.http4k:http4k-client-apache"
}
```

The following creates a simple endpoint, binds it to a Jetty server then starts, queries, and stops it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>

## Single-line CD Bootstrap
Run the single command in the readme of [this repo](https://github.com/http4k/http4k-bootstrap) to create a HelloWorld **http4k** app with a full CD pipeline using Github -> TravisCI -> Heroku.
