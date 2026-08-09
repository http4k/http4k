# http4k Distribution & Release Channels

**Last updated: 9 August 2026**

This document explains how http4k is distributed, what is changing on 1 October 2026, and what
you may want to do about it. It is kept current as the situation develops.

## Summary

From **1 October 2026**, Sonatype are enforcing new publishing limits on Maven Central. http4k is
substantially over those limits, so from that date:

| Channel | Cadence | Contents |
|---|---|---|
| **Maven Central** (`org.http4k`) | approximately **quarterly** | http4k Community Edition, free, Apache-2.0 |
| **[maven.http4k.org](https://maven.http4k.org)** | every **1-2 weeks**, unchanged | Community Edition, Pro modules and Enterprise Edition |

Both channels are supported. The Community Edition remains free and remains on Maven Central.

## What is changing

Sonatype are introducing publishing limits on Maven Central, applied per publishing organisation
(e.g. `org.http4k`) as a rolling three-month average. The free thresholds are approximately:

- **1,000 files** per month
- **80 MB** per month
- **7 releases** per month

A single http4k release is approximately **6,500 files** and **~200 MB** across each of the 200+ modules. One
release therefore consumes several months of the file and size allowance. The limits apply at
organisation level, so splitting the project across namespaces does not help.

Publishers over the thresholds need an adjusted limit, an exemption, or Sonatype's paid "Maven
Central Publisher Pro" product in order to continue publishing without interruption.

Details can be found on Sonatype's own [site](https://central.sonatype.org).

Until 30 September 2026 the limits are informational only. Enforcement begins on 1 October 2026.

## Exemption request - status

Sonatype operate an exemption process for open source projects with unusual publishing patterns.

| Date | Event |
|---|---|
| 17 June 2026 | Exemption request submitted to `central-support@sonatype.com` for the `org.http4k` namespace |
| 9 August 2026 | No response received |

This table is updated as the situation changes. We do not currently expect an exemption to be
granted, and we are planning on the basis that the limits will apply to us in full.

## What this means for you

**If you consume http4k from Maven Central:** from October, expect new versions to appear
approximately quarterly rather than every 1-2 weeks. Security fixes will reach Maven Central on
that same cadence. Everything currently published stays published - nothing is being removed.

**If you need releases on the current cadence** - because you have a defined patching SLA,
because you are subject to regulatory obligations such as the EU Cyber Resilience Act, or simply
because you would rather not wait - `maven.http4k.org` will continue to publish every 1-2 weeks.
Access is included with [http4k Enterprise Edition](https://http4k.org/enterprise/), which also
provides LTS, priority support, source access, signed licence reports, and supply-chain artifacts
(SLSA provenance, CycloneDX SBOMs, cosign signatures) verifiable at build time with
[http4k Verify](https://verify.http4k.org).

**If you mirror or proxy dependencies** through Artifactory, Nexus or similar, both channels work
without infrastructure changes.

## Question channels
- Slack: [http4k on Kotlin Slack](https://kotlinlang.slack.com/messages/http4k/)
- Commercial: `enterprise@http4k.org`
