title: http4k Installation
description: How to get the http4k library

<hr/>

## http4k JARs

All **http4k** libraries are available on Maven Central and JCenter and are released under a single platform version. The most convenient way to use **http4k** on most projects is to install the **Platform BOM** (Bill-of-Materials) module and then just add extra module dependencies as you need them. 

To do this for the simplest project using only the **core** module, just add the following to your Gradle file dependencies block:

```groovy
implementation platform("org.http4k:http4k-bom:3.260.0")
implementation "org.http4k:http4k-core"
```

<hr/>

## http4k Toolbox CLI
We have developed a collection of useful tools for Developers working with the **http4k** toolset to turbo-charge
development. These include:

- A **Project Wizard** that generates entire bootstrap Server and Serverless project source folders - including 
fully working starter code, build tooling, extra modules and packaging options.
- From **OpenAPI v2 & V3** specification JSON/YAML, generate an entire working **http4k** Server, Client and Model objects (generated from JSON Schema).
- **Generate Kotlin Data Class** definitions from an inputted JSON, YAML, or XML document.

### Install with <a href="https://sdkman.io/">SDKman!</a>
SDKMan! provides package management for Unix-based systems.

To install <a href="https://sdkman.io/">SDKman!</a>, just paste the following into your terminal:

```bash
curl -s https://get.sdkman.io | bash
```

After installation, bootstrap SDKMan! by opening a new terminal:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

To install the latest http4k Toolbox binary:

```bash
sdk install http4k
```
... then test it to show the help with:

```bash
http4k --help
```

### Install with <a href="https://brew.sh/">Brew</a>
Brew is the "missing package manager for MacOS".

To install <a href="https://brew.sh/">Brew</a>, just paste the following into your terminal:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
```

To install the latest http4k Toolbox binary:

```bash
brew tap http4k/tap && brew install http4k
```

... then test it with:

```bash
http4k --help
```
