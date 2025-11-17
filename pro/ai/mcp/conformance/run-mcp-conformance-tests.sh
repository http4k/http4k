#!/usr/bin/env bash

set -e
#../../../../gradlew :http4k-ai-mcp-conformance:startMcpConformanceServer

rm -rf results
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp

#../../../../gradlew :http4k-ai-mcp-conformance:stopMcpConformanceServer

## list of scenarios
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario server-initialize
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario completion-complete
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-list
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-simple-text
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-image
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-audio
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-embedded-resource
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-mixed-content
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-error
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario resources-list
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario resources-read-text
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario resources-read-binary
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario resources-templates-read
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-with-progress
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-sampling
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-elicitation
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario logging-set-level
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario resources-subscribe
npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario tools-call-with-logging

npx @modelcontextprotocol/conformance server --url http://localhost:4001/mcp --scenario elicitation-sep1034-defaults
