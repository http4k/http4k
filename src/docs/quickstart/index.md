title: http4k Quickstart
description: Easy ways to get started using http4k

# Quickstart

This simple example demonstates how to serve and consume HTTP services using **http4k**. 

To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation group: "org.http4k", name: "http4k-core", version: "3.256.1"
    implementation group: "org.http4k", name: "http4k-server-jetty", version: "3.256.1"
    implementation group: "org.http4k", name: "http4k-client-apache", version: "3.256.1"
}
```

The following creates a simple endpoint, binds it to a Jetty server then starts, queries, and stops it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>

## Single-line CD Bootstrap
Run the single command in the readme of [this repo](https://github.com/http4k/http4k-bootstrap) to create a HelloWorld **http4k** app with a full CD pipeline using Github -> TravisCI -> Heroku.
