### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-webdriver", version: "2.13.0"```

### About

A basic Selenium WebDriver API implementation for **http4k** HttpHandlers, which runs completely out of container (no network) for ultra fast tests.

| Feature | Supported | Planned |
|---------|-----------|----------|
| Basic navigation|yes|yes|
| CSS selector functionality|yes|yes|
| Link clicking|yes|yes|
| Form detection and submission|yes|yes|
| Form field entry|no|yes|
| JavaScript|no|no|
| Alerts|no|no|
| Screenshots|no|no|
| Frames|no|no|
| Multiple windows|no|no|


Use the API like any other WebDriver implementation, by simply passing your app HttpHandler to construct it:

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/webdriver/example.kt"></script>
