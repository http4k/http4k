title: http4k Approval Testing Module
description: Feature overview of the http4k-testing-approval module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-testing-approval", version: "4.25.14.0"
```

### About
[Approval testing](http://approvaltests.com/) is a form of testing which allows the expected output of 
a test to be specified in a non-code but still source-controlled format, such as a text file. This is a powerful alternative to traditional assertion-based tests for a number of reasons:

1. It is often inconvenient and/or error prone to attempt to write assertions to cover the entirety of 
test output - examples of this include JSON, HTML or XML documents.
1. Output may not always be in a format that can be created easily in a test.
1. In case of a mismatch, output can be more efficiently diagnosed by the human eye.
1. The output of a test may change significantly in a short period (this is especially true for HTML 
content), but we also want to tightly control the contract.

The general idea for implementing this style of testing in http4k is based on the excellent 
[okeydoke](https://github.com/dmcg/okey-doke) library, and is centered around the idea of comparing 
the output of an HTTP operation - this is generally the `Response` content, but it can also be the 
`Request` if we are interested in testing construction of request content. 

For each test-case, a named `<test name>.approved` file is committed (under the `src/test/resources` 
folder), against which the test output can be compared by an `Approver` object injected into the test 
method. In case of a mismatch, an equivalent `<test name>.actual` file is written. This file can then 
be verified and if ok, renamed to become the approved file. To make this operation easier in the IDE, we
recommend the usage of the 
[IntelliJ OkeyDoke plugin](https://plugins.jetbrains.com/plugin/9424-okey-doke-support) which adds a 
mouse and keyboard shortcut to rename the file. 

The `http4k-testing-approval` module implements this functionality as a JUnit5 extension that 
will inject the `Approver` automatically into test methods.

### Standard Approval tests
By using the `ApprovalTest` extension, an instance of an `Approver` is injected into each test.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/approvaltests/example_standard.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/approvaltests/example_standard.kt"></script>

### Content-type specific Approval tests
Because so many APIs are based around messages with a particular content type, the 
module also provides Junit 5 extensions that will:

1. Check for the presence of the a particular `content-type` on the `HttpMessage` under test and fail if it is not valid.
1. Validate that the `HttpMessage` actually contains valid content for the content type.
1. Format and compare the approval output as pretty-printed version. Note that by default the http4k format modules use compact printing to conserve message space.

The module also provides the following built-in extensions:

- `HtmlApprovalTest`
- `JsonApprovalTest`
- `XmlApprovalTest`

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/approvaltests/example_json.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/approvaltests/example_json.kt"></script>

### Implementing custom JUnit Extensions
As with the rest of http4k, a base implementation, `BaseApprovalTest` of the Junit5 Extension is 
provided, allowing API users to implement custom approval schemes or non-FS based approaches for 
storing the approval files.

[http4k]: https://http4k.org
