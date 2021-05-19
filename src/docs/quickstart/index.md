title: http4k Quickstart
description: Easy ways to get started using http4k

Depending on your learning style, there are a number of options to get started with **http4k**...

# Follow a tutorial
**I'm starting from scratch**: There is a step-by-step [beginner tutorial](/tutorials/your_first_http4k_app). This will get you up and running with a basic buildable project.

# Generate a new project with the http4k Toolbox
**I'd like a helping hand**: We have developed a set of useful tools for Developers working with the **http4k** toolset to turbo-charge app development. Collectively, this is known as the <a href="https://toolbox.http4k.org">http4k Toolbox</a>. These tools include:

- A **Project Wizard** that generates entire bootstrap Server and Serverless project source folders - including
  fully working starter code, build tooling, extra modules and packaging options.
- From **OpenAPI v2 & V3** specification JSON/YAML, generate an entire working **http4k** Server, Client and Model objects (generated from JSON Schema).
- **Generate Kotlin Data Class** definitions from an inputted JSON, YAML, or XML document.

# Integrate http4k into an existing project
**Just show me the code!**: This simple example demonstrates how to serve and consume HTTP services using **http4k**. To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation platform("org.http4k:http4k-bom:4.9.1.0")
    implementation "org.http4k:http4k-core"
    implementation "org.http4k:http4k-server-netty"
    implementation "org.http4k:http4k-client-apache"
}
```

The following creates a simple endpoint, binds it to a Netty server then starts, queries, and stops it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>

#  See how it's done in the Examples Repo
**I want to see what http4k can do**: For fully self-contained examples demonstrates the standout features of **http4k**, there is a GitHub repository at [http4k/examples](https://github.com/http4k/examples).

# Single-line CD Bootstrap
**I want to get something deployed ASAP**: Run the single command in the readme of [this repo](https://github.com/http4k/http4k-bootstrap) to create a HelloWorld **http4k** app with a full CD pipeline using Github -> TravisCI -> Heroku.
