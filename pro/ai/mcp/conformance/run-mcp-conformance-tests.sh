#!/usr/bin/env bash

set -e

rm -rf results

#../../../../gradlew :http4k-ai-mcp-conformance:startMcpConformanceServer

npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp
#npx @modelcontextprotocol/conformance server --url https://demo.http4k.org/mcp-conformance/mcp

## individual scenario
# npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario server-initialize

#../../../../gradlew :http4k-ai-mcp-conformance:stopMcpConformanceServer
