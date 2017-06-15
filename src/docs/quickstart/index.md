# Quickstart

This simple example demonstates how to serve and consume HTTP services using **http4k**. 

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "2.5.0"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "2.5.0"
    compile group: "org.http4k", name: "http4k-client-apache", version: "2.5.0"
}
```

The following creates a simple endpoint, binds it to a Jetty server then starts, queries, and stops it.

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>
