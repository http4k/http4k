title: http4k Installation
description: How to get the http4k library

<hr/>

### http4k JARs

All **http4k** libraries are available on Maven Central and JCenter and are released under a single platform version. The most convenient way to use **http4k** on most projects is to install the **Platform BOM** (Bill-of-Materials) module and then just add extra module dependencies as you need them. 

To do this for the simplest project using only the **core** module, just add the following to your Gradle file dependencies block:

```groovy
implementation platform("org.http4k:http4k-bom:3.272.0")
implementation "org.http4k:http4k-core"
```

<hr/>

### http4k Toolbox 
We have developed a set of useful tools for Developers working with the **http4k** toolset to turbo-charge app development. Collectively, this is known as the <a href="https://toolbox.http4k.org">http4k Toolbox</a>. These tools include:

- A **Project Wizard** that generates entire bootstrap Server and Serverless project source folders - including 
fully working starter code, build tooling, extra modules and packaging options.
- From **OpenAPI v2 & V3** specification JSON/YAML, generate an entire working **http4k** Server, Client and Model objects (generated from JSON Schema).
- **Generate Kotlin Data Class** definitions from an inputted JSON, YAML, or XML document.
