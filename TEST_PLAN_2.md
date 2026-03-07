# Wiretap Test Plan 2: Webdriver Testing

## Overview

Headless browser testing for Wiretap using `http4k-testing-webdriver` (`:http4k-testing-webdriver`).

## Key Constraint

`Http4kWebDriver` uses **JSoup** for HTML parsing — it does **NOT** execute JavaScript. Wiretap uses Datastar for interactivity, so webdriver tests are limited
to **initial page rendering, navigation, and form structure** — not interactive Datastar behavior.

## Approach

`Http4kWebDriver` works with an in-memory `HttpHandler`, requiring no real server. Tests instantiate a full Wiretap `HttpHandler` and navigate pages via the
webdriver API.

```kotlin
val driver = Http4kWebDriver(wiretapApp)
driver.navigate().to("/_wiretap/inbound")
assertThat(driver.title, equalTo("Inbound Client"))
```

## Test Ideas

### 1. Full Smoke Test — All Pages Return 200

Navigate to every Wiretap page and verify:

- HTTP 200 status (via `driver.status`)
- Page title is present and correct

Pages to cover:

- `/_wiretap/inbound` — Inbound Client
- `/_wiretap/outbound` — Outbound Client
- `/_wiretap/traffic` — Traffic
- `/_wiretap/chaos` — Chaos
- `/_wiretap/otel` — OpenTelemetry
- `/_wiretap/home` — Home / Overview
- `/_wiretap/mcp` — MCP Panel

### 2. Client Page — Form Structure

Navigate to `/_wiretap/inbound` and verify:

- Method selector is present
- URL input field exists
- Header rows section renders
- Body textarea is present
- Send button exists

### 3. Client Import — Pre-populated Form

- Record a transaction into the store
- Navigate to `/_wiretap/inbound?import=<id>`
- Verify form fields are pre-populated with the transaction's method, URL, headers, and body

### 4. Traffic Page — View Bar

Navigate to `/_wiretap/traffic` and verify:

- View bar renders with default views
- Transaction list container is present

### 5. Chaos Page — Form Fields

Navigate to `/_wiretap/chaos` and verify:

- Activation form renders
- Direction selector is present
- Status/latency configuration fields exist

### 6. OTel Page — Trace List

Navigate to `/_wiretap/otel` and verify:

- Trace list container renders
- Page structure includes expected sections

### 7. Cross-Page Navigation

- Navigate to home page
- Find and click nav links
- Verify each link navigates to the correct page

### 8. Static Assets

Verify CSS and JS resources load with 200 status:

- `/_wiretap/static/wiretap.css`
- `/_wiretap/static/wiretap.js` (if applicable)
- Datastar library resources

## Dependencies

```kotlin
testImplementation("org.http4k:http4k-testing-webdriver")
```

## Notes

- These tests complement the existing approval tests (which test Datastar SSE fragment responses) by verifying full-page HTML rendering
- JSoup limitation means we cannot test: Datastar reactivity, SSE streaming, dynamic DOM updates, JavaScript-driven interactions
- Focus on structural validation: elements exist, forms have correct fields, navigation works
