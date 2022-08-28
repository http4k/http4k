title: http4k Quickstart
description: Easy ways to get started using http4k

Depending on your learning style, there are a number of options to get started with **http4k**...

# I'm starting from scratch
**Follow a tutorial**: There is a step-by-step [beginner tutorial](/guide/tutorials/your_first_http4k_app). This will get you up and running with a basic buildable project.

# I'd like a helping hand
**Generate a new project with the http4k Toolbox**: We have developed a set of useful tools for Developers working with the **http4k** toolset to turbo-charge app development. Collectively, this is known as the <a href="https://toolbox.http4k.org">http4k Toolbox</a>. These tools include:

- A **Project Wizard** that generates entire bootstrap Server and Serverless project source folders - including
  fully working starter code, build tooling, extra modules and packaging options.
- From **OpenAPI v2 & V3** specification JSON/YAML, generate an entire working **http4k** Server, Client and Model objects (generated from JSON Schema).
- **Generate Kotlin Data Class** definitions from an inputted JSON, YAML, or XML document.

# I'm already set up and just need to integrate!
**Add http4k into an existing project**: This simple example demonstrates how to serve and consume HTTP services using **http4k**. To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation platform("org.http4k:http4k-bom:4.30.1.0")
    implementation "org.http4k:http4k-core"
    implementation "org.http4k:http4k-server-undertow"
    implementation "org.http4k:http4k-client-apache"
}
```

The following creates a simple endpoint, binds it to a Undertow server then starts, queries, and stops it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/quickstart/example.kt"></script>

# I want to see what http4k can do!
**See how it's done in the Examples Repo**: For fully self-contained examples demonstrates the standout features of **http4k**, there is a GitHub repository at [http4k/examples](https://github.com/http4k/examples).

[comment]: <> (# I want to get something deployed ASAP)

[comment]: <> (**Single-line CD Bootstrap**: Run the single command in the readme of [this repo]&#40;https://github.com/http4k/http4k-bootstrap&#41; to create a HelloWorld **http4k** app with a full CD pipeline using Github -> TravisCI -> Heroku.)
