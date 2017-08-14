### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-webdriver", version: "2.18.4"```

### About

A basic Selenium WebDriver API implementation for **http4k** HttpHandlers, which runs completely out of container (no network) for ultra fast tests.

| Feature | Supported | Notes |
|---------|-----------|-------|
| Navigation|yes|simple back/forward/refresh history|
| CSS selectors|yes||
| Link navigation|yes||
| Form field entry and submission|yes||
| Cookie storage|yes|manual expiry management|
| JavaScript|no||
| Alerts|no||
| Screenshots|no||
| Frames|no||
| Multiple windows|no||

Use the API like any other WebDriver implementation, by simply passing your app HttpHandler to construct it:

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/webdriver/example.kt"></script>
