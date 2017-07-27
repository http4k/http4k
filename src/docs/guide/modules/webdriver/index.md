### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-webdriver", version: "2.11.3"```

### About

A basic Selenium WebDriver API implementation for **http4k** HttpHandlers, which runs completely out of container (no network) for ultra fast tests.

Note that since this is NOT a browser, we don't support any complicated features like JavaScript. Basic navigation and CSS selector functionality are supported.

Use the API like any other WebDriver implementation, by simply passing your app HttpHandler to construct it:

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/webdriver/example.kt"></script>
