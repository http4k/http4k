# reekwest

[![coverage](https://coveralls.io/repos/reekwest/reekwest/badge.svg?branch=master)](https://coveralls.io/github/reekwest/reekwest?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.1-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/reekwest/reekwest.svg?branch=master)](https://travis-ci.org/reekwest/reekwest)
[![bintray version](https://api.bintray.com/packages/reekwest/maven/reekwest/images/download.svg)](https://bintray.com/reekwest/maven/reekwest/_latestVersion)

A sane implementation of HTTP for Kotlin

Features:
 * Immutable Request/Response
 * Same abstractions for client and server usage
 * Enables "HTTP application as a function"
 * Can be plugged to different libraries and containers. Current implementation includes:
   * Client: ApacheHttpClient
   * Server: Servlets, Jetty
 * No 3rd party dependency required to start
