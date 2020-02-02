#!/bin/bash

source ./release-functions.sh

ensure_release_commit

maven_publish "http4k-cloudnative"
maven_publish "http4k-resilience4j"
maven_publish "http4k-security-oauth"
maven_publish "http4k-metrics-micrometer"
maven_publish "http4k-template-dust"
maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-template-freemarker"
maven_publish "http4k-template-jade4j"
