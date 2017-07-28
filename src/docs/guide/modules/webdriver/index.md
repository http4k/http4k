### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-webdriver", version: "2.14.0"```

### About

A basic Selenium WebDriver API implementation for **http4k** HttpHandlers, which runs completely out of container (no network) for ultra fast tests.

| Feature | Supported |
|---------|-----------|
| Basic navigation|yes|
| CSS selector functionality|yes|
| Link clicking|yes|yes|
| Form detection and submission|yes|
| Form field entry|yes|
| Cookies|not yet :)|
| JavaScript|no|
| Alerts|no|
| Screenshots|no|
| Frames|no|
| Multiple windows|no|

Use the API like any other WebDriver implementation, by simply passing your app HttpHandler to construct it:

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/webdriver/example.kt"></script>
